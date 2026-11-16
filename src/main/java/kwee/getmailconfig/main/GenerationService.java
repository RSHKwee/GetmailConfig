package kwee.getmailconfig.main;

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

/**
 * Gedeelde generatie-logica voor CLI en GUI.
 *
 * <p>
 * Doet het pure werk: Excel lezen, Velocity-template mergen, output schrijven.
 * Voortgang en annulering lopen via een {@link GenerationListener}.
 * </p>
 */
public final class GenerationService {

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

    listener.status("🔄 Start generatie...");
    listener.progress(0, 1);
    checkCancelled(listener);

    // ---- Template laden ----
    File templateBaseDir = templateFile.getParentFile();
    listener.log("Template basis directory: "
        + (templateBaseDir != null ? templateBaseDir.getAbsolutePath() : "?"));

    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
    ve.setProperty("resource.loader.file.path",
        templateBaseDir != null ? templateBaseDir.getAbsolutePath() : ".");
    ve.setProperty("resource.loader.file.cache", "false");
    ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");

    ve.init();

    String templateName = templateFile.getName();
    Template template = ve.getTemplate(templateName, "UTF-8");
    listener.log("Template geladen: " + templateName);
    checkCancelled(listener);

    // ---- Excel lezen ----
    listener.status("📊 Excel inlezen...");
    listener.progress(-1, 1); // onbepaald
    List<Map<String, String>> excelData = readExcel(excelFile, listener);
    listener.progress(1, 1);
    listener.status("📊 " + excelData.size() + " rijen gelezen");
    listener.log(excelData.size() + " rijen gelezen");
    checkCancelled(listener);

    // ---- Context vullen ----
    VelocityContext context = new VelocityContext();
    context.put("rows", excelData);
    context.put("rowCount", excelData.size());
    context.put("timestamp", new Date());
    if (!excelData.isEmpty()) {
      context.put("headers", excelData.get(0).keySet());
    }

    // ---- Mergen ----
    listener.status("⚙️ Template samenvoegen...");
    listener.log("Template samenvoegen...");
    StringWriter writer = new StringWriter();
    template.merge(context, writer);
    String finalOutput = writer.toString();
    checkCancelled(listener);

    // ---- Opslaan ----
    listener.status("💾 Opslaan...");
    listener.log("Opslaan naar: " + outputFile.getAbsolutePath());
    File parent = outputFile.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw new IOException("Kon output-map niet aanmaken: " + parent);
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      fos.write(finalOutput.getBytes(StandardCharsets.UTF_8));
    }

    listener.status("✅ Klaar.");
    listener.log("Opgeslagen: " + outputFile.getAbsolutePath());
    listener.progress(1, 1);
    return finalOutput;
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