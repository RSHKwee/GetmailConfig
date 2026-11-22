package kwee.getmailconfig.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import kwee.logger.MyLogger;

/**
 * Gedeelde generatie-logica voor CLI en GUI.
 *
 * <p>
 * Doet het pure werk: Excel lezen, Velocity-template mergen, output schrijven.
 * Voortgang en annulering lopen via een {@link GenerationListener}.
 * </p>
 */
public final class GenerationService {
  private static final Logger LOGGER = MyLogger.getLogger();

  /** Wordt gegooid als de listener aangeeft dat er geannuleerd is. */
  public static class CancelledException extends Exception {
    private static final long serialVersionUID = -4534508091637013336L;

    public CancelledException() {
      super("Generatie geannuleerd");
    }
  }

  private GenerationService() {
  }

  /**
   * Genereert output op basis van template + Excel en schrijft naar outputFile.
   *
   * @return de gegenereerde string (handig voor logging/grootte)
   * @throws CancelledException als de listener annulering aanvraagt
   * @throws IOException        bij I/O-problemen
   */
  public static String generate(File templateFile, File excelFile,
      File outputFile, GenerationListener listener)
      throws IOException, CancelledException {

    if (listener == null) {
      listener = GenerationListener.NONE;
    }

    LOGGER.log(Level.INFO, "🔄 Start generatie...");
    listener.status("🔄 Start generatie...");
    listener.progress(0, 1);
    checkCancelled(listener);

    // ---- Excel lezen ----
    listener.status("📊 Excel inlezen...");
    listener.progress(-1, 1); // onbepaald
    List<Map<String, String>> excelData = readExcel(excelFile, listener);
    listener.progress(1, 1);
    listener.status("📊 " + excelData.size() + " rijen gelezen");
    LOGGER.log(Level.INFO, excelData.size() + " rijen gelezen");

    // ---- Template laden ----
    File templateBaseDir = templateFile.getParentFile();
    listener.log("Template basis directory: "
        + (templateBaseDir != null ? templateBaseDir.getAbsolutePath() : "?"));

    // ---- Context vullen ----
    VelocityContext context = new VelocityContext();
    context.put("rows", excelData);
    context.put("rowCount", excelData.size());
    context.put("timestamp", new Date());
    if (!excelData.isEmpty()) {
      context.put("headers", excelData.get(0).keySet());
    }

    // Velocity & Templater
    checkCancelled(listener);
    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
    ve.setProperty("resource.loader.file.path",
        templateBaseDir != null ? templateBaseDir.getAbsolutePath() : ".");
    ve.setProperty("resource.loader.file.cache", "false");
    ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
    ve.init();

    String templateName = templateFile.getName();
    Template template = ve.getTemplate(templateName, "UTF-8");
    LOGGER.log(Level.INFO, "Template geladen: " + templateName);
    checkCancelled(listener);

    // ---- Mergen ----
    listener.status("⚙️ Template samenvoegen...");
    LOGGER.log(Level.INFO, "Template samenvoegen...");
    StringWriter writer = new StringWriter();
    template.merge(context, writer);
    String finalOutput = writer.toString();
    checkCancelled(listener);

    // ---- Opslaan ----
    listener.status("💾 Opslaan...");
    LOGGER.log(Level.INFO, "Opslaan naar: " + outputFile.getAbsolutePath());
    File parent = outputFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Kon output-map niet aanmaken: " + parent);
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      fos.write(finalOutput.getBytes(StandardCharsets.UTF_8));
    }

    listener.status("✅ Klaar.");
    LOGGER.log(Level.INFO, "Opgeslagen: " + outputFile.getAbsolutePath());
    listener.progress(1, 1);
    return finalOutput;
  }

  // ------------------------------------------------------------------
  // Per-row generatie
  // ------------------------------------------------------------------
  /** Configuratie voor per-rij bestandsgeneratie. */
  public static final class PerRowOptions {
    /** Kolom met submap (relatief t.o.v. outputRoot). Mag null/leeg zijn. */
    public String subdirColumn = "Subdir";
    /** Kolom met prefix. */
    public String prefixColumn = "Prefix";
    /** Kolom met extensie (zonder punt). */
    public String extColumn = "Ext";
    /** Fallback-extensie als Ext-kolom leeg is (zonder punt). */
    public String defaultExt = "txt";
    /** Basis-map waarnaar alle bestanden worden geschreven. */
    public File outputRoot;
    /** Naam van de variabele in de Velocity-context voor de rij-map. */
    public String rowVarName = "row";
    /** Voeg ook 1-based rownummer toe aan de context (variabele "rownr"). */
    public boolean exposeRowNumber = true;
    /** Vervang ongeldige bestandsnaam-tekens door '_'. */
    public boolean sanitizeFilenames = true;
    /** Empty extension ("", empty string) allowed. */
    public boolean allowEmptyExtension = true;

    public PerRowOptions(File outputRoot) {
      this.outputRoot = outputRoot;
    }
  }

  /**
   * Genereert per Excel-rij een apart bestand.
   *
   * <p>
   * Bestandsnaam: {@code <Subdir>/<Prefix>_<rownr>.<Ext>} waarbij de kolomnamen
   * configureerbaar zijn via {@link PerRowOptions}. Lege subdir betekent direct
   * in outputRoot.
   * </p>
   *
   * @param templateFile Velocity-template
   * @param excelFile    Excel-bron
   * @param opts         configuratie
   * @param listener     voortgangs-callback (mag null zijn)
   * @return lijst van geschreven bestanden (in volgorde van de rijen)
   */
  public static List<File> generatePerRow(File templateFile, File excelFile,
      PerRowOptions opts, GenerationListener listener)
      throws IOException, CancelledException {

    if (listener == null) {
      listener = GenerationListener.NONE;
    }
    if (opts == null || opts.outputRoot == null) {
      throw new IllegalArgumentException(
          "PerRowOptions.outputRoot is verplicht");
    }

    listener.status("🔄 Start per-rij generatie...");
    listener.progress(0, 1);
    checkCancelled(listener);

    // ---- Excel lezen (hergebruik bestaande code) ----
    listener.status("📊 Excel inlezen...");
    listener.progress(-1, 1);
    List<Map<String, String>> rows = readExcel(excelFile, listener);
    listener.status("📊 " + rows.size() + " rijen gelezen");
    LOGGER.log(Level.INFO, rows.size() + " rijen gelezen");

    // ---- Template engine opzetten ----
    File templateBaseDir = templateFile.getParentFile();
    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
    ve.setProperty("resource.loader.file.path",
        templateBaseDir != null ? templateBaseDir.getAbsolutePath() : ".");
    ve.setProperty("resource.loader.file.cache", "false");
    ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
    ve.init();

    Template template = ve.getTemplate(templateFile.getName(), "UTF-8");
    LOGGER.log(Level.INFO, "Template geladen: " + templateFile.getName());

    // ---- Output-root aanmaken ----
    if (!opts.outputRoot.exists() && !opts.outputRoot.mkdirs()) {
      throw new IOException("Kon output-map niet aanmaken: " + opts.outputRoot);
    }

    List<File> written = new ArrayList<>();
    int total = Math.max(1, rows.size());
    int done = 0;

    for (Map<String, String> row : rows) {
      checkCancelled(listener);

      // 1-based rownummer, zodat rij 1 in Excel ook "_1" wordt
      int rownr = done + 1;

      // ---- Bestandsnaam opbouwen ----
      String subdir = value(row, opts.subdirColumn);
      String prefix = value(row, opts.prefixColumn);
      String ext = value(row, opts.extColumn);
      if (opts.allowEmptyExtension) {
        if (ext == null) {
          ext = opts.defaultExt;
        }
      } else if (ext == null || ext.isEmpty()) {
        ext = opts.defaultExt;
      }
      ext = ext.replaceFirst("^\\.", ""); // ".txt" -> "txt"

      String baseName;
      if (opts.exposeRowNumber) {
        baseName = (prefix.isBlank() ? "row" : prefix) + "_" + rownr;
      } else {
        baseName = (prefix.isBlank() ? "row" : prefix);
      }
      if (opts.sanitizeFilenames) {
        baseName = sanitizeFilename(baseName);
        if (!opts.allowEmptyExtension) {
          ext = sanitizeFilename(ext);
        }
        subdir = subdir == null ? "" : subdir; // paden niet strippen, wel per
                                               // segment
      }

      // Subdir veilig opschonen: elk segment saneren
      File targetDir = opts.outputRoot;
      if (subdir != null && !subdir.isBlank()) {
        for (String segment : subdir.replace('\\', '/').split("/")) {
          if (segment.isBlank()) {
            continue;
          }
          targetDir = new File(targetDir,
              opts.sanitizeFilenames ? sanitizeFilename(segment) : segment);
        }
      }
      if (!targetDir.exists() && !targetDir.mkdirs()) {
        throw new IOException("Kon submap niet aanmaken: " + targetDir);
      }

      File outputFile = new File(targetDir, baseName + "." + ext);
      if (outputFile.exists()) {
        baseName = baseName + "_" + rownr;
        outputFile = new File(targetDir, baseName + "." + ext);
      }

      // ---- Context vullen ----
      VelocityContext context = new VelocityContext();
      context.put(opts.rowVarName, row);
      // Gemaksvelden, ook handig in templates:
      context.put("rows", rows);
      context.put("rowCount", rows.size());
      context.put("timestamp", new Date());
      if (opts.exposeRowNumber) {
        context.put("rownr", rownr);
      }
      if (!rows.isEmpty()) {
        context.put("headers", rows.get(0).keySet());
      }

      // ---- Mergen ----
      listener
          .status("⚙️ Rij " + rownr + " / " + rows.size() + " samenvoegen...");
      StringWriter writer = new StringWriter();
      template.merge(context, writer);
      String finalOutput = writer.toString();
      checkCancelled(listener);

      // ---- Schrijven ----
      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
        fos.write(finalOutput.getBytes(StandardCharsets.UTF_8));
      }
      written.add(outputFile);
      LOGGER.log(Level.INFO, "Geschreven: " + outputFile.getAbsolutePath());

      done++;
      listener.progress(done, total);
    }

    listener.status("✅ Klaar: " + written.size() + " bestand(en) geschreven.");
    listener.progress(1, 1);
    return written;
  }

//------------------------------------------------------------------
//Helpers
//------------------------------------------------------------------

  /** Veilige lookup: null-safe, trimt waarden. */
  private static String value(Map<String, String> row, String column) {
    if (column == null || column.isBlank()) {
      return "";
    }
    String v = row.get(column);
    return v == null ? "" : v.trim();
  }

  /**
   * Maakt een string veilig als bestandsnaam-segment. Verwijdert/vervangt
   * tekens die op Windows/macOS problematisch zijn.
   */
  public static String sanitizeFilename(String name) {
    if (name == null) {
      return "";
    }
    // Vervang ongeldige tekens door '_'
    String cleaned = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
    // Windows: geen punt of spatie aan het eind
    cleaned = cleaned.replaceAll("[. ]+$", "");
    // Lege naam voorkomen
    return cleaned.isEmpty() ? "unnamed" : cleaned;
  }

  // ------------------------------------------------------------------
  // Excel lezen
  // ------------------------------------------------------------------

  public static List<Map<String, String>> readExcel(File excelFile,
      GenerationListener listener) throws IOException, CancelledException {
    List<Map<String, String>> result = new ArrayList<>();
    try (Workbook workbook = WorkbookFactory.create(excelFile)) {
      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> rowIterator = sheet.iterator();
      if (!rowIterator.hasNext()) {
        return result;
      }
      Row headerRow = rowIterator.next();
      List<String> headers = new ArrayList<>();
      for (Cell cell : headerRow) {
        headers.add(getCellStringValue(cell));
      }

      int total = Math.max(1, sheet.getLastRowNum());
      int done = 0;
      while (rowIterator.hasNext()) {
        if (listener.isCancelled()) {
          throw new CancelledException();
        }
        Row dataRow = rowIterator.next();
        Map<String, String> rowMap = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
          Cell cell = dataRow.getCell(i,
              Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
          rowMap.put(headers.get(i), getCellStringValue(cell));
        }
        result.add(rowMap);

        done++;
        listener.progress(done, total);
        if (done % 50 == 0) {
          listener.status("📊 " + done + " / " + total + " rijen gelezen...");
        }
      }
    }
    return result;
  }

  /** Overload zonder listener (voor tests of eenvoudige CLI). */
  public static List<Map<String, String>> readExcel(File excelFile)
      throws IOException {
    try {
      return readExcel(excelFile, GenerationListener.NONE);
    } catch (CancelledException e) {
      // kan niet voorkomen met NONE
      throw new IOException(e);
    }
  }

  public static String getCellStringValue(Cell cell) {
    if (cell == null) {
      return "";
    }
    return switch (cell.getCellType()) {
    case STRING -> cell.getStringCellValue();
    case NUMERIC -> {
      if (DateUtil.isCellDateFormatted(cell)) {
        yield cell.getDateCellValue().toString();
      } else {
        yield String.valueOf(cell.getNumericCellValue());
      }
    }
    case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
    case FORMULA -> cell.getCellFormula();
    default -> "";
    };
  }

  private static void checkCancelled(GenerationListener listener)
      throws CancelledException {
    if (listener.isCancelled()) {
      throw new CancelledException();
    }
  }
}