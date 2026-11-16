package kwee.getmailconfig.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

public final class CliRunner {
  private static final Logger LOGGER = MyLogger.getLogger();

  private CliRunner() {
  }

  /** Retourneert een exit-code (0 = ok). */
  public static int run(CliOptions opts) {
    try {
      LOGGER.log(Level.INFO, "🔄 Start generatie...");
      LOGGER.log(Level.INFO, "   Template: " + opts.template.getAbsolutePath());
      LOGGER.log(Level.INFO, "   Excel   : " + opts.excel.getAbsolutePath());
      LOGGER.log(Level.INFO, "   Output  : " + opts.output.getAbsolutePath());

      List<Map<String, String>> excelData = readExcel(opts.excel);
      LOGGER.log(Level.INFO, "📊 " + excelData.size() + " rijen gelezen");

      VelocityEngine ve = new VelocityEngine();
      File baseDir = opts.template.getParentFile();
      ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
      ve.setProperty("resource.loader.file.path",
          baseDir != null ? baseDir.getAbsolutePath() : ".");
      ve.setProperty("resource.loader.file.cache", "false");
      ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
      ve.init();

      Template template = ve.getTemplate(opts.template.getName(), "UTF-8");

      VelocityContext context = new VelocityContext();
      context.put("rows", excelData);
      context.put("rowCount", excelData.size());
      if (!excelData.isEmpty()) {
        context.put("headers", excelData.get(0).keySet());
      }

      StringWriter writer = new StringWriter();
      template.merge(context, writer);
      String finalOutput = writer.toString();

      File parent = opts.output.getParentFile();
      if (parent != null && !parent.exists() && !parent.mkdirs()) {
        LOGGER.log(Level.WARNING, "❌ Kon output-map niet aanmaken: " + parent);
        return 3;
      }
      try (FileOutputStream fos = new FileOutputStream(opts.output)) {
        fos.write(finalOutput.getBytes(StandardCharsets.UTF_8));
      }
      LOGGER.log(Level.INFO, "✅ Opgeslagen: " + opts.output.getAbsolutePath());
      LOGGER.log(Level.INFO, "📏 Grootte: " + finalOutput.length() + " tekens");
      return 0;
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "❌ Fout: " + ex.getMessage());
      ex.printStackTrace(System.err);
      return 1;
    }
  }

  public static List<Map<String, String>> readExcel(File excelFile)
      throws IOException {
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
      while (rowIterator.hasNext()) {
        Row dataRow = rowIterator.next();
        Map<String, String> rowMap = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
          Cell cell = dataRow.getCell(i,
              Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
          rowMap.put(headers.get(i), getCellStringValue(cell));
        }
        result.add(rowMap);
      }
    }
    return result;
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
}