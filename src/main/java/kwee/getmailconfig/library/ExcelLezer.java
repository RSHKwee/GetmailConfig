package kwee.getmailconfig.library;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelLezer {
  List<Map<String, String>> excelData;

  ExcelLezer(String excelPad) throws IOException {
    excelData = leesExcelBestand(excelPad);
  }

  public List<Map<String, String>> getExcelData() {
    return excelData;
  }

  /**
   * Lees Excel bestand en converteer naar lijst van maps
   */
  private List<Map<String, String>> leesExcelBestand(String excelPad) throws IOException {
    List<Map<String, String>> resultaat = new ArrayList<>();

    try (FileInputStream fis = new FileInputStream(excelPad); Workbook workbook = new XSSFWorkbook(fis)) {
      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);

      if (headerRow == null) {
        throw new IllegalArgumentException("Excel bestand heeft geen headers");
      }

      // Lees headers
      List<String> headers = new ArrayList<>();
      for (Cell cell : headerRow) {
        headers.add(getCellWaardeAlsString(cell));
      }

      // Verwerk elke data rij
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row rij = sheet.getRow(i);
        if (rij == null) {
          continue;
        }

        Map<String, String> rijData = new HashMap<>();
        for (int j = 0; j < headers.size(); j++) {
          Cell cel = rij.getCell(j);
          String waarde = cel != null ? getCellWaardeAlsString(cel) : "";
          rijData.put(headers.get(j), waarde);
        }

        resultaat.add(rijData);
      }
    }

    return resultaat;
  }

  /**
   * Haal celwaarde op als string
   */
  private String getCellWaardeAlsString(Cell cel) {
    if (cel == null) {
      return "";
    }

    switch (cel.getCellType()) {
    case STRING:
      return cel.getStringCellValue();
    case NUMERIC:
      if (DateUtil.isCellDateFormatted(cel)) {
        return cel.getDateCellValue().toString();
      } else {
        double getal = cel.getNumericCellValue();
        if (getal == (long) getal) {
          return String.valueOf((long) getal);
        } else {
          return String.valueOf(getal);
        }
      }
    case BOOLEAN:
      return String.valueOf(cel.getBooleanCellValue());
    case FORMULA:
      try {
        return String.valueOf(cel.getNumericCellValue());
      } catch (Exception e) {
        return cel.getCellFormula();
      }
    default:
      return "";
    }
  }

}
