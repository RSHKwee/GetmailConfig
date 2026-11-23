package NewGUI;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ExcelVelocityApp extends Application {

  private static final Path STANDAARD_CONFIG = Path
      .of(System.getProperty("user.home"), ".excel-velocity", "config.json");

  // Gedeelde state
  private final TextField excelField = new TextField();
  private final TextField templateDirField = new TextField();
  private final TextField outputDirField = new TextField();

  private final ObservableList<ConfigEntry> configData = FXCollections
      .observableArrayList();

  // Hoofdtab
  private final ListView<ConfigEntry> templateList = new ListView<>(configData);
  private final TextField gekozenOutputField = new TextField();
  private final Label gekozenTemplateLabel = new Label("(geen selectie)");

  private final Label statusLabel = new Label("Gereed");

  private File laatsteConfigBestand;

  @Override
  public void start(Stage stage) {
    TabPane tabs = new TabPane();
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    Tab verwerkingTab = new Tab("Verwerking", bouwVerwerkingTab(stage));
    Tab configuratieTab = new Tab("Configuratie",
        new ConfigTab(configData).bouwInhoud());
    tabs.getTabs().addAll(verwerkingTab, configuratieTab);

    VBox root = new VBox(tabs, bouwStatusBar());
    VBox.setVgrow(tabs, Priority.ALWAYS);

    Scene scene = new Scene(root, 1050, 800);
    stage.setTitle("Excel → Velocity verwerking");
    stage.setScene(scene);

    // Automatisch laden
    if (Files.exists(STANDAARD_CONFIG)) {
      try {
        AppConfig cfg = ConfigService.load(STANDAARD_CONFIG.toFile());
        pasConfigToe(cfg);
        laatsteConfigBestand = STANDAARD_CONFIG.toFile();
        statusLabel
            .setText("Automatisch geladen: " + STANDAARD_CONFIG.getFileName());
      } catch (IOException ex) {
        statusLabel.setText(
            "Kon standaardconfiguratie niet laden: " + ex.getMessage());
      }
    }

    // Automatisch opslaan bij sluiten
    stage.setOnCloseRequest(ev -> {
      try {
        Files.createDirectories(STANDAARD_CONFIG.getParent());
        ConfigService.save(verzamelConfig(), STANDAARD_CONFIG.toFile());
      } catch (IOException ignored) {
      }
    });

    stage.show();
  }

  // =========================================================
  // Tab 1: Verwerking
  // =========================================================
  private VBox bouwVerwerkingTab(Stage stage) {

    // --- Paden ---
    GridPane inputGrid = new GridPane();
    inputGrid.setHgap(10);
    inputGrid.setVgap(10);
    inputGrid.setPadding(new Insets(10));

    Button excelBtn = new Button("...");
    excelBtn.setOnAction(e -> chooseExcelFile(stage));
    HBox excelBox = new HBox(5, excelField, excelBtn);
    HBox.setHgrow(excelField, Priority.ALWAYS);

    Button templateBtn = new Button("...");
    templateBtn.setOnAction(e -> {
      chooseDirectory(stage, templateDirField);
      laadTemplatesUitMap();
    });
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

    // --- Template-lijst + output-veld ---
    templateList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(ConfigEntry ce, boolean empty) {
        super.updateItem(ce, empty);
        if (empty || ce == null) {
          setText(null);
        } else {
          String richting = ce.isEenBestand() ? "één bestand" : "per rij";
          setText(ce.getTemplate() + "  →  " + ce.getOutput() + "  (" + richting
              + ")");
        }
      }
    });
    templateList.getSelectionModel().selectedItemProperty()
        .addListener((obs, oud, nieuw) -> toonGekozenTemplate(nieuw));

    VBox templateBox2 = new VBox(5,
        new Label("Kies een template uit de configuratie:"), templateList,
        new Label("Gekozen template:"), gekozenTemplateLabel,
        new Label("Output bestand (aanpasbaar):"), gekozenOutputField);
    VBox.setVgrow(templateList, Priority.ALWAYS);

    // --- 10 verwerkingsknoppen ---
    GridPane buttonGrid = new GridPane();
    buttonGrid.setHgap(10);
    buttonGrid.setVgap(10);
    buttonGrid.setPadding(new Insets(10, 0, 0, 0));

    for (int i = 1; i <= 10; i++) {
      final int nr = i;
      Button b = new Button("Verwerking " + nr);
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

    VBox root = new VBox(10, inputGrid, templateBox2, buttonGrid);
    root.setPadding(new Insets(10));
    VBox.setVgrow(templateBox2, Priority.ALWAYS);
    return root;
  }

  // =========================================================
  // Statusbalk
  // =========================================================
  private HBox bouwStatusBar() {
    HBox statusBar = new HBox(statusLabel);
    statusBar.setPadding(new Insets(5, 10, 5, 10));
    statusBar.setStyle("-fx-background-color: #e0e0e0;");
    statusBar.setAlignment(Pos.CENTER_LEFT);
    return statusBar;
  }

  // =========================================================
  // Template-lijst & output-veld
  // =========================================================
  private void toonGekozenTemplate(ConfigEntry ce) {
    if (ce == null) {
      gekozenTemplateLabel.setText("(geen selectie)");
      gekozenOutputField.clear();
      return;
    }
    gekozenTemplateLabel.setText(ce.getTemplate());
    String output = ce.getOutput();
    if (output == null || output.isBlank()) {
      output = ConfigEntry.defaultOutputVoor(ce.getTemplate());
    }
    gekozenOutputField.setText(output);
  }

  /**
   * Leest de templates-map; voegt nieuwe .vm-bestanden toe zonder bestaande te
   * verwijderen.
   */
  private void laadTemplatesUitMap() {
    String dir = templateDirField.getText();
    if (dir == null || dir.isBlank()) {
      return;
    }
    File map = new File(dir);
    if (!map.isDirectory()) {
      return;
    }

    File[] bestanden = map
        .listFiles((d, naam) -> naam.toLowerCase().endsWith(".vm"));
    if (bestanden == null) {
      return;
    }

    Set<String> bestaand = new HashSet<>();
    for (ConfigEntry ce : configData) {
      bestaand.add(ce.getTemplate());
    }

    for (File f : bestanden) {
      if (!bestaand.contains(f.getName())) {
        configData.add(new ConfigEntry(f.getName(),
            ConfigEntry.defaultOutputVoor(f.getName()), true));
      }
    }
  }

  // =========================================================
  // Bestandskeuze
  // =========================================================
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

  // =========================================================
  // Verwerking
  // =========================================================
  private void startVerwerking(int nummer) {
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

    ConfigEntry gekozen = templateList.getSelectionModel().getSelectedItem();
    String overrideOutput = gekozenOutputField.getText();

    statusLabel.setText("Verwerking " + nummer + " gestart...");
    System.out.println("=== Verwerking " + nummer + " ===");
    System.out.println("Excel:     " + excelField.getText());
    System.out.println("Templates: " + templateDirField.getText());
    System.out.println("Output:    " + outputDirField.getText());
    if (gekozen != null) {
      System.out.println(
          "Focus template: " + gekozen.getTemplate() + " → " + overrideOutput);
    }
    for (ConfigEntry ce : configData) {
      System.out.printf("  %s → %s (%s)%n", ce.getTemplate(), ce.getOutput(),
          ce.isEenBestand() ? "één bestand" : "per rij");
    }

    // TODO: roep hier je bestaande verwerkingsservice aan
    statusLabel.setText("Verwerking " + nummer + " gereed.");
  }

  private void toonFout(String msg) {
    Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
    a.setHeaderText(null);
    a.showAndWait();
  }

  // =========================================================
  // Configuratie save/load (volledige config)
  // =========================================================
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

  public static void main(String[] args) {
    launch(args);
  }
}