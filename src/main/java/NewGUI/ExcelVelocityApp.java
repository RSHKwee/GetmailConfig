package NewGUI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * @formatter:off
 * 
 * ExcelVelocityApp   (bedrading + save/load + verwerking)
 * ├── AppState       (gedeelde properties: paden + ObservableList<ConfigEntry>)
 * ├── ConfigTab      (tab "Configuratie": paden + tabel + save/load-knoppen)
 * └── VerwerkingTab  (tab "Verwerking": template-lijst + output-veld + 10 knoppen)
 * 
 * @formatter:on
 */
public class ExcelVelocityApp extends Application {

  private static final Path STANDAARD_CONFIG = Path
      .of(System.getProperty("user.home"), ".excel-velocity", "config.json");

  private final AppState state = new AppState();
  private final Label statusLabel = new Label("Gereed");

  private VerwerkingTab verwerkingTab;
  private File laatsteConfigBestand;

  @Override
  public void start(Stage stage) {
    // Callbacks
    Runnable onSave = () -> opslaanConfiguratie(stage);
    Runnable onLoad = () -> ladenConfiguratie(stage);
    Runnable onTemplatesMapChanged = this::laadTemplatesUitMap;

    verwerkingTab = new VerwerkingTab(state, stage, this::startVerwerking,
        onTemplatesMapChanged);

    ConfigTab configTab = new ConfigTab(state, stage, onSave, onLoad,
        onTemplatesMapChanged);

    TabPane tabs = new TabPane();
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.getTabs().addAll(new Tab("Verwerking", verwerkingTab.bouwInhoud()),
        new Tab("Configuratie", configTab.bouwInhoud()));

    VBox root = new VBox(tabs, bouwStatusBar());
    VBox.setVgrow(tabs, Priority.ALWAYS);

    Scene scene = new Scene(root, 1050, 800);
    stage.setTitle("Excel → Velocity verwerking");
    stage.setScene(scene);

    // Automatisch laden
    if (Files.exists(STANDAARD_CONFIG)) {
      try {
        AppConfig cfg = ConfigService.load(STANDAARD_CONFIG.toFile());
        state.vanuitConfig(cfg);
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
        ConfigService.save(state.naarConfig(), STANDAARD_CONFIG.toFile());
      } catch (IOException ignored) {
      }
    });

    stage.show();
  }

  private HBox bouwStatusBar() {
    HBox statusBar = new HBox(statusLabel);
    statusBar.setPadding(new Insets(5, 10, 5, 10));
    statusBar.setStyle("-fx-background-color: #e0e0e0;");
    statusBar.setAlignment(Pos.CENTER_LEFT);
    return statusBar;
  }

  /**
   * Leest de templates-map; voegt nieuwe .vm-bestanden toe zonder bestaande te
   * verwijderen.
   */
  private void laadTemplatesUitMap() {
    String dir = state.getTemplatesMap();
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

    // Verzamel bestaande paden (canoniek waar mogelijk)
    Set<String> bestaand = new HashSet<>();
    for (ConfigEntry ce : state.getRegels()) {
      String t = ce.getTemplate();
      if (t == null || t.isBlank()) {
        continue;
      }
      File f = new File(t);
      if (!f.isAbsolute()) {
        f = new File(map, t);
      }
      try {
        bestaand.add(f.getCanonicalPath());
      } catch (IOException ex) {
        bestaand.add(f.getAbsolutePath());
      }
    }

    for (File f : bestanden) {
      String canoniek;
      try {
        canoniek = f.getCanonicalPath();
      } catch (IOException ex) {
        canoniek = f.getAbsolutePath();
      }
      if (!bestaand.contains(canoniek)) {
        state.getRegels().add(new ConfigEntry(f.getAbsolutePath(), // volledig
                                                                   // pad
            ConfigEntry.defaultOutputVoor(f.getName()), true));
      }
    }
    if (verwerkingTab != null) {
      verwerkingTab.ververs();
    }
  }

  private void startVerwerking(int nummer) {
    if (state.getExcelBestand().isBlank()) {
      toonFout("Selecteer eerst een Excel bestand (tab Configuratie).");
      return;
    }
    if (state.getTemplatesMap().isBlank()) {
      toonFout("Selecteer eerst de templates map (tab Configuratie).");
      return;
    }
    if (state.getOutputMap().isBlank()) {
      toonFout("Selecteer eerst de output map (tab Configuratie).");
      return;
    }
    if (state.getRegels().isEmpty()) {
      toonFout("Voeg minimaal één configuratieregel toe (tab Configuratie).");
      return;
    }

    ConfigEntry gekozen = verwerkingTab.getGekozenTemplate();
    String overrideOutput = verwerkingTab.getGekozenOutput();

    statusLabel.setText("Verwerking " + nummer + " gestart...");
    System.out.println("=== Verwerking " + nummer + " ===");
    System.out.println("Excel:     " + state.getExcelBestand());
    System.out.println("Templates: " + state.getTemplatesMap());
    System.out.println("Output:    " + state.getOutputMap());
    if (gekozen != null) {
      System.out.println(
          "Focus template: " + gekozen.getTemplate() + " → " + overrideOutput);
    }
    for (ConfigEntry ce : state.getRegels()) {
      File templateFile = new File(ce.getTemplate());
      if (!templateFile.isAbsolute()) {
        templateFile = new File(state.getTemplatesMap(), ce.getTemplate());
      }
      System.out.printf("  %s → %s (%s)%n", templateFile.getAbsolutePath(),
          ce.getOutput(), ce.isEenBestand() ? "één bestand" : "per rij");
    }

    // TODO: roep hier je bestaande verwerkingsservice aan
    statusLabel.setText("Verwerking " + nummer + " gereed.");
  }

  private void toonFout(String msg) {
    Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
    a.setHeaderText(null);
    a.showAndWait();
  }

  // === Save/load van de VOLLEDIGE config ===

  private void opslaanConfiguratie(Stage stage) {
    FileChooser fc = new FileChooser();
    fc.setTitle("Configuratie opslaan");
    fc.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("JSON", "*.json"));
    if (laatsteConfigBestand != null
        && laatsteConfigBestand.getParentFile() != null) {
      fc.setInitialDirectory(laatsteConfigBestand.getParentFile());
      fc.setInitialFileName(laatsteConfigBestand.getName());
    } else {
      fc.setInitialFileName("config.json");
    }
    File f = fc.showSaveDialog(stage);
    if (f == null) {
      return;
    }
    if (!f.getName().toLowerCase().endsWith(".json")) {
      f = new File(f.getAbsolutePath() + ".json");
    }
    try {
      ConfigService.save(state.naarConfig(), f);
      laatsteConfigBestand = f;
      statusLabel.setText("Configuratie opgeslagen: " + f.getName());
    } catch (IOException ex) {
      new Alert(Alert.AlertType.ERROR, "Opslaan mislukt: " + ex.getMessage())
          .showAndWait();
    }
  }

  private void ladenConfiguratie(Stage stage) {
    FileChooser fc = new FileChooser();
    fc.setTitle("Configuratie laden");
    fc.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("JSON", "*.json"));
    if (laatsteConfigBestand != null
        && laatsteConfigBestand.getParentFile() != null) {
      fc.setInitialDirectory(laatsteConfigBestand.getParentFile());
    }
    File f = fc.showOpenDialog(stage);
    if (f == null) {
      return;
    }
    try {
      AppConfig cfg = ConfigService.load(f);
      state.vanuitConfig(cfg);
      laatsteConfigBestand = f;
      statusLabel.setText("Configuratie geladen: " + f.getName());
    } catch (IOException ex) {
      new Alert(Alert.AlertType.ERROR, "Laden mislukt: " + ex.getMessage())
          .showAndWait();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}