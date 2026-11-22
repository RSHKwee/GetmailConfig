package NewGUI;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ExcelVelocityApp extends Application {

  private static final Path STANDAARD_CONFIG = Path
      .of(System.getProperty("user.home"), ".excel-velocity", "config.json");

  private final TextField excelField = new TextField();
  private final TextField templateDirField = new TextField();
  private final TextField outputDirField = new TextField();

  private final ObservableList<ConfigEntry> configData = FXCollections
      .observableArrayList();
  private final TableView<ConfigEntry> configTable = new TableView<>(
      configData);

  private final Label statusLabel = new Label("Gereed");

  private File laatsteConfigBestand;

  @Override
  public void start(Stage stage) {
    // === Invoervelden ===
    GridPane inputGrid = new GridPane();
    inputGrid.setHgap(10);
    inputGrid.setVgap(10);
    inputGrid.setPadding(new Insets(10));

    Button excelBtn = new Button("...");
    excelBtn.setOnAction(e -> chooseExcelFile(stage));
    HBox excelBox = new HBox(5, excelField, excelBtn);
    HBox.setHgrow(excelField, Priority.ALWAYS);

    Button templateBtn = new Button("...");
    templateBtn.setOnAction(e -> chooseDirectory(stage, templateDirField));
    HBox templateBox = new HBox(5, templateDirField, templateBtn);
    HBox.setHgrow(templateDirField, Priority.ALWAYS);

    Button outputBtn = new Button("...");
    outputBtn.setOnAction(e -> chooseDirectory(stage, outputDirField));
    HBox outputBox = new HBox(5, outputDirField, outputBtn);
    HBox.setHgrow(outputDirField, Priority.ALWAYS);

    inputGrid.add(new Label("Excel bestand:"), 0, 0);
    inputGrid.add(excelBox, 1, 0);
    inputGrid.add(new Label("Templates map:"), 0, 1);
    inputGrid.add(templateBox, 1, 1);
    inputGrid.add(new Label("Output map:"), 0, 2);
    inputGrid.add(outputBox, 1, 2);

    ColumnConstraints cc0 = new ColumnConstraints();
    ColumnConstraints cc1 = new ColumnConstraints();
    cc1.setHgrow(Priority.ALWAYS);
    inputGrid.getColumnConstraints().addAll(cc0, cc1);

    // === Configuratie tabel ===
    setupTable();

    Button addRowBtn = new Button("+ Rij toevoegen");
    addRowBtn.setOnAction(e -> configData.add(new ConfigEntry("", "", true)));

    Button removeRowBtn = new Button("- Rij verwijderen");
    removeRowBtn.setOnAction(e -> {
      ConfigEntry selected = configTable.getSelectionModel().getSelectedItem();
      if (selected != null) {
        configData.remove(selected);
      }
    });

    Button browseTemplateBtn = new Button("Template kiezen...");
    browseTemplateBtn.setOnAction(e -> {
      ConfigEntry selected = configTable.getSelectionModel().getSelectedItem();
      if (selected == null) {
        return;
      }
      FileChooser fc = new FileChooser();
      fc.setTitle("Kies Velocity template");
      if (!templateDirField.getText().isBlank()) {
        File dir = new File(templateDirField.getText());
        if (dir.isDirectory()) {
          fc.setInitialDirectory(dir);
        }
      }
      File f = fc.showOpenDialog(stage);
      if (f != null) {
        selected.setTemplate(f.getName());
      }
    });

    Button saveBtn = new Button("Configuratie opslaan...");
    saveBtn.setOnAction(e -> opslaanConfiguratie(stage));

    Button loadBtn = new Button("Configuratie laden...");
    loadBtn.setOnAction(e -> ladenConfiguratie(stage));

    HBox tableButtons = new HBox(10, addRowBtn, removeRowBtn, browseTemplateBtn,
        saveBtn, loadBtn);
    tableButtons.setPadding(new Insets(5, 0, 5, 0));

    VBox tableBox = new VBox(5, new Label("Configuratie (template → output):"),
        tableButtons, configTable);
    VBox.setVgrow(configTable, Priority.ALWAYS);

    // === 10 Verwerkingsbuttons ===
    GridPane buttonGrid = new GridPane();
    buttonGrid.setHgap(10);
    buttonGrid.setVgap(10);
    buttonGrid.setPadding(new Insets(10));

    for (int i = 1; i <= 10; i++) {
      final int nr = i;
      Button b = new Button("Verwerking " + i);
      b.setMaxWidth(Double.MAX_VALUE);
      b.setPrefHeight(40);
      b.setOnAction(e -> startVerwerking(nr));
      buttonGrid.add(b, (i - 1) % 5, (i - 1) / 5);
    }
    for (int c = 0; c < 5; c++) {
      ColumnConstraints cc = new ColumnConstraints();
      cc.setHgrow(Priority.ALWAYS);
      cc.setFillWidth(true);
      buttonGrid.getColumnConstraints().add(cc);
    }

    // === Statusbalk ===
    HBox statusBar = new HBox(statusLabel);
    statusBar.setPadding(new Insets(5, 10, 5, 10));
    statusBar.setStyle("-fx-background-color: #e0e0e0;");
    statusBar.setAlignment(Pos.CENTER_LEFT);

    // === Layout samenstellen ===
    VBox root = new VBox(10, inputGrid, tableBox, buttonGrid, statusBar);
    root.setPadding(new Insets(10));
    VBox.setVgrow(tableBox, Priority.ALWAYS);

    Scene scene = new Scene(root, 1000, 800);
    stage.setTitle("Excel → Velocity verwerking");
    stage.setScene(scene);

    // === Automatisch laatste configuratie laden ===
    if (Files.exists(STANDAARD_CONFIG)) {
      try {
        pasConfigToe(ConfigService.load(STANDAARD_CONFIG.toFile()));
        laatsteConfigBestand = STANDAARD_CONFIG.toFile();
        statusLabel
            .setText("Automatisch geladen: " + STANDAARD_CONFIG.getFileName());
      } catch (IOException ex) {
        statusLabel.setText(
            "Kon standaardconfiguratie niet laden: " + ex.getMessage());
      }
    }

    // === Automatisch opslaan bij sluiten ===
    stage.setOnCloseRequest(ev -> {
      try {
        Files.createDirectories(STANDAARD_CONFIG.getParent());
        ConfigService.save(verzamelConfig(), STANDAARD_CONFIG.toFile());
      } catch (IOException ignored) {
        // stil falen; gebruiker sluit de app
      }
    });

    stage.show();
  }

  private void setupTable() {
    configTable.setEditable(true);
    configTable.setColumnResizePolicy(
        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    TableColumn<ConfigEntry, String> templateCol = new TableColumn<>(
        "Velocity template");
    templateCol.setCellValueFactory(c -> c.getValue().templateProperty());
    templateCol.setCellFactory(TextFieldTableCell.forTableColumn());
    templateCol
        .setOnEditCommit(ev -> ev.getRowValue().setTemplate(ev.getNewValue()));
    templateCol.setPrefWidth(250);

    TableColumn<ConfigEntry, String> outputCol = new TableColumn<>(
        "Output bestand");
    outputCol.setCellValueFactory(c -> c.getValue().outputProperty());
    outputCol.setCellFactory(TextFieldTableCell.forTableColumn());
    outputCol
        .setOnEditCommit(ev -> ev.getRowValue().setOutput(ev.getNewValue()));
    outputCol.setPrefWidth(250);

    TableColumn<ConfigEntry, Boolean> singleCol = new TableColumn<>(
        "Eén bestand");
    singleCol.setCellValueFactory(c -> c.getValue().eenBestandProperty());
    singleCol.setCellFactory(CheckBoxTableCell.forTableColumn(singleCol));
    singleCol.setEditable(true);
    singleCol.setPrefWidth(120);

    configTable.getColumns().addAll(templateCol, outputCol, singleCol);

    // Voorbeelddata (alleen bij eerste start)
    configData.add(new ConfigEntry("factuur.vm", "facturen.txt", true));
    configData.add(new ConfigEntry("klant.vm", "klant_${rij}.txt", false));
  }

  private void chooseExcelFile(Stage stage) {
    FileChooser fc = new FileChooser();
    fc.setTitle("Kies Excel bestand");
    fc.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"),
        new FileChooser.ExtensionFilter("Alle bestanden", "*.*"));
    File f = fc.showOpenDialog(stage);
    if (f != null) {
      excelField.setText(f.getAbsolutePath());
    }
  }

  private void chooseDirectory(Stage stage, TextField target) {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setTitle("Kies map");
    File f = dc.showDialog(stage);
    if (f != null) {
      target.setText(f.getAbsolutePath());
    }
  }

  private void startVerwerking(int nummer) {
    // Validatie
    if (excelField.getText().isBlank()) {
      toonFout("Selecteer eerst een Excel bestand.");
      return;
    }
    if (templateDirField.getText().isBlank()) {
      toonFout("Selecteer eerst de templates map.");
      return;
    }
    if (outputDirField.getText().isBlank()) {
      toonFout("Selecteer eerst de output map.");
      return;
    }
    if (configData.isEmpty()) {
      toonFout("Voeg minimaal één configuratieregel toe.");
      return;
    }

    statusLabel.setText("Verwerking " + nummer + " gestart...");
    System.out.println("=== Verwerking " + nummer + " ===");
    System.out.println("Excel:     " + excelField.getText());
    System.out.println("Templates: " + templateDirField.getText());
    System.out.println("Output:    " + outputDirField.getText());
    for (ConfigEntry ce : configData) {
      System.out.printf("  %s → %s (%s)%n", ce.getTemplate(), ce.getOutput(),
          ce.isEenBestand() ? "één bestand" : "per rij");
    }

    // TODO: roep hier je bestaande verwerkingsservice aan
    statusLabel.setText("Verwerking " + nummer + " gereed.");
  }

  // === Configuratie save/load ===

  private AppConfig verzamelConfig() {
    AppConfig cfg = new AppConfig();
    cfg.setExcelBestand(excelField.getText());
    cfg.setTemplatesMap(templateDirField.getText());
    cfg.setOutputMap(outputDirField.getText());
    cfg.setRegels(new ArrayList<>(configData));
    return cfg;
  }

  private void pasConfigToe(AppConfig cfg) {
    excelField.setText(cfg.getExcelBestand());
    templateDirField.setText(cfg.getTemplatesMap());
    outputDirField.setText(cfg.getOutputMap());
    configData.setAll(cfg.getRegels());
  }

  private void opslaanConfiguratie(Stage stage) {
    FileChooser fc = new FileChooser();
    fc.setTitle("Configuratie opslaan");
    fc.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("JSON", "*.json"));
    if (laatsteConfigBestand != null) {
      fc.setInitialFileName(laatsteConfigBestand.getName());
      File parent = laatsteConfigBestand.getParentFile();
      if (parent != null && parent.isDirectory()) {
        fc.setInitialDirectory(parent);
      }
    } else {
      fc.setInitialFileName("config.json");
    }
    File f = fc.showSaveDialog(stage);
    if (f == null) {
      return;
    }

    // Zorg dat extensie .json is
    if (!f.getName().toLowerCase().endsWith(".json")) {
      f = new File(f.getAbsolutePath() + ".json");
    }

    try {
      ConfigService.save(verzamelConfig(), f);
      laatsteConfigBestand = f;
      statusLabel.setText("Configuratie opgeslagen: " + f.getName());
    } catch (IOException ex) {
      new Alert(Alert.AlertType.ERROR, "Opslaan mislukt: " + ex.getMessage(),
          ButtonType.OK).showAndWait();
    }
  }

  private void ladenConfiguratie(Stage stage) {
    FileChooser fc = new FileChooser();
    fc.setTitle("Configuratie laden");
    fc.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("JSON", "*.json"));
    if (laatsteConfigBestand != null) {
      File parent = laatsteConfigBestand.getParentFile();
      if (parent != null && parent.isDirectory()) {
        fc.setInitialDirectory(parent);
      }
    }
    File f = fc.showOpenDialog(stage);
    if (f == null) {
      return;
    }

    try {
      AppConfig cfg = ConfigService.load(f);
      pasConfigToe(cfg);
      laatsteConfigBestand = f;
      statusLabel.setText("Configuratie geladen: " + f.getName());
    } catch (IOException ex) {
      new Alert(Alert.AlertType.ERROR, "Laden mislukt: " + ex.getMessage(),
          ButtonType.OK).showAndWait();
    }
  }

  private void toonFout(String msg) {
    Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
    a.setHeaderText(null);
    a.showAndWait();
  }

  public static void main(String[] args) {
    launch(args);
  }
}