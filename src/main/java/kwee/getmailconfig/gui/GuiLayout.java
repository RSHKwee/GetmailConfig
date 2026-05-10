package kwee.getmailconfig.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GuiLayout extends Application {
  private File templateFile;
  private File excelFile;
  private TextArea logArea;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("Apache Velocity + Excel Generator");

    Label templateLabel = new Label("Template bestand:");
    Button templateButton = new Button("Kies template (.vm/.txt)");
    Label templatePath = new Label("Geen bestand gekozen");

    Label excelLabel = new Label("Excel databron:");
    Button excelButton = new Button("Kies Excel bestand (.xlsx/.xls)");
    Label excelPath = new Label("Geen bestand gekozen");

    Label outputLabel = new Label("Output bestand:");
    Button outputButton = new Button("Genereer en opslaan als...");

    logArea = new TextArea();
    logArea.setEditable(false);
    logArea.setPrefHeight(200);

    templateButton.setOnAction(e -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.getExtensionFilters()
          .add(new FileChooser.ExtensionFilter("Velocity templates", "*.vm", "*.txt", "*.*"));
      File selected = fileChooser.showOpenDialog(primaryStage);
      if (selected != null) {
        templateFile = selected;
        templatePath.setText(selected.getAbsolutePath());
      }
    });

    excelButton.setOnAction(e -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setInitialDirectory(templateFile.getParentFile());
      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel bestanden", "*.xlsx", "*.xls"));
      File selected = fileChooser.showOpenDialog(primaryStage);
      if (selected != null) {
        excelFile = selected;
        excelPath.setText(selected.getAbsolutePath());
      }
    });

    outputButton.setOnAction(e -> {
      if (templateFile == null || excelFile == null) {
        log("❌ Eerst template én Excel bestand kiezen!");
        return;
      }
      FileChooser saver = new FileChooser();
      saver.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tekstbestand", "*.txt"));
      saver.setInitialFileName("output.txt");
      saver.setInitialDirectory(templateFile.getParentFile());
      File outputFile = saver.showSaveDialog(primaryStage);
      if (outputFile != null) {
        generateOutput(outputFile);
      }
    });

    VBox root = new VBox(10, templateLabel, templateButton, templatePath, excelLabel, excelButton, excelPath,
        new Separator(), outputLabel, outputButton, new Label("Log:"), logArea);
    root.setPadding(new Insets(15));
    Scene scene = new Scene(root, 700, 550);
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private void generateOutput(File outputFile) {
    try {
      log("🔄 Start generatie...");

      // Bepaal de basis directory voor includes
      File templateBaseDir = templateFile.getParentFile();
      log("📁 Template basis directory: " + templateBaseDir.getAbsolutePath());

      // Velocity engine met FileResourceLoader
      VelocityEngine ve = new VelocityEngine();
      ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
      ve.setProperty("file.resource.loader.path", templateBaseDir.getAbsolutePath());
      ve.setProperty("file.resource.loader.cache", "false"); // false tijdens testen
      ve.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
      ve.init();

      // Alleen bestandsnaam gebruiken (zonder pad)
      String templateName = templateFile.getName();
      Template template = ve.getTemplate(templateName, "UTF-8");
      log("📄 Template geladen: " + templateName);

      // Excel data inlezen
      List<Map<String, String>> excelData = readExcelToMapList(excelFile);
      log("📊 " + excelData.size() + " rijen gelezen");

      // Context vullen
      VelocityContext context = new VelocityContext();
      context.put("rows", excelData);
      context.put("rowCount", excelData.size());
      if (!excelData.isEmpty()) {
        context.put("headers", excelData.get(0).keySet());
      }

      // Genereer output
      StringWriter writer = new StringWriter();
      template.merge(context, writer);
      String finalOutput = writer.toString();

      // Opslaan
      try (FileOutputStream fos = new FileOutputStream(outputFile)) {
        fos.write(finalOutput.getBytes(StandardCharsets.UTF_8));
      }

      log("✅ Opgeslagen naar: " + outputFile.getAbsolutePath());
      log("📏 Grootte: " + finalOutput.length() + " tekens");

    } catch (Exception ex) {
      log("❌ Fout: " + ex.getMessage());
      ex.printStackTrace();
      showAlert("Fout bij genereren", ex.getMessage());
    }
  }

  private void showAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  private List<Map<String, String>> readExcelToMapList(File excelFile) throws IOException {
    List<Map<String, String>> result = new ArrayList<>();
    try (Workbook workbook = WorkbookFactory.create(excelFile)) {
      Sheet sheet = workbook.getSheetAt(0);
      Iterator<Row> rowIterator = sheet.iterator();

      if (!rowIterator.hasNext()) {
        return result;
      }

      // Eerste rij = headers
      Row headerRow = rowIterator.next();
      List<String> headers = new ArrayList<>();
      for (Cell cell : headerRow) {
        headers.add(getCellStringValue(cell));
      }

      // Data rijen
      while (rowIterator.hasNext()) {
        Row dataRow = rowIterator.next();
        Map<String, String> rowMap = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
          Cell cell = dataRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
          rowMap.put(headers.get(i), getCellStringValue(cell));
        }
        result.add(rowMap);
      }
    }
    return result;
  }

  private String getCellStringValue(Cell cell) {
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

  private void log(String msg) {
    logArea.appendText(msg + "\n");
    System.out.println(msg);
  }
}
