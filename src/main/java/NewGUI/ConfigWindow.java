package NewGUI;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ConfigWindow {

  private final ObservableList<ConfigEntry> configData;
  private final Stage stage;
  private final Runnable onSaved;
  private File laatsteConfigBestand;

  public ConfigWindow(ObservableList<ConfigEntry> configData, Stage owner) {
    this.configData = configData;
    this.stage = new Stage();
    this.stage.initOwner(owner);
    this.stage.setTitle("Configuratie beheren");
    this.stage.setScene(bouwScene());
    this.stage.setWidth(800);
    this.stage.setHeight(500);
    this.onSaved = null;
  }

  private Scene bouwScene() {
    TableView<ConfigEntry> table = new TableView<>(configData);
    table.setEditable(true);
    table.setColumnResizePolicy(
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

    table.getColumns().addAll(templateCol, outputCol, singleCol);

    Button addBtn = new Button("+ Rij toevoegen");
    addBtn.setOnAction(e -> configData.add(new ConfigEntry("", "", true)));

    Button removeBtn = new Button("- Rij verwijderen");
    removeBtn.setOnAction(e -> {
      ConfigEntry sel = table.getSelectionModel().getSelectedItem();
      if (sel != null) {
        configData.remove(sel);
      }
    });

    Button saveBtn = new Button("Opslaan...");
    saveBtn.setOnAction(e -> opslaan());

    Button loadBtn = new Button("Laden...");
    loadBtn.setOnAction(e -> laden());

    Button closeBtn = new Button("Sluiten");
    closeBtn.setOnAction(e -> stage.close());

    HBox knoppen = new HBox(10, addBtn, removeBtn, saveBtn, loadBtn, closeBtn);
    knoppen.setPadding(new Insets(5, 0, 5, 0));

    VBox root = new VBox(5, new Label("Configuratie (template → output):"),
        knoppen, table);
    root.setPadding(new Insets(10));
    VBox.setVgrow(table, Priority.ALWAYS);

    return new Scene(root);
  }

  public void show() {
    stage.show();
    stage.toFront();
  }

  /**
   * Als save/load gebruikt worden, kan de hoofdapp zijn "laatste bestand"
   * bijwerken.
   */
  public File getLaatsteConfigBestand() {
    return laatsteConfigBestand;
  }

  private void opslaan() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Configuratie opslaan");
    fc.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("JSON", "*.json"));
    fc.setInitialFileName("config.json");
    if (laatsteConfigBestand != null
        && laatsteConfigBestand.getParentFile() != null) {
      fc.setInitialDirectory(laatsteConfigBestand.getParentFile());
    }
    File f = fc.showSaveDialog(stage);
    if (f == null) {
      return;
    }
    if (!f.getName().toLowerCase().endsWith(".json")) {
      f = new File(f.getAbsolutePath() + ".json");
    }
    try {
      ConfigService.save(verzamelConfig(), f);
      laatsteConfigBestand = f;
      if (onSaved != null) {
        onSaved.run();
      }
      new Alert(Alert.AlertType.INFORMATION, "Opgeslagen: " + f.getName())
          .showAndWait();
    } catch (IOException ex) {
      new Alert(Alert.AlertType.ERROR, "Opslaan mislukt: " + ex.getMessage())
          .showAndWait();
    }
  }

  private void laden() {
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
      configData.setAll(cfg.getRegels());
      laatsteConfigBestand = f;
      if (onSaved != null) {
        onSaved.run();
      }
    } catch (IOException ex) {
      new Alert(Alert.AlertType.ERROR, "Laden mislukt: " + ex.getMessage())
          .showAndWait();
    }
  }

  private AppConfig verzamelConfig() {
    AppConfig cfg = new AppConfig();
    cfg.setRegels(new ArrayList<>(configData));
    return cfg;
  }
}
