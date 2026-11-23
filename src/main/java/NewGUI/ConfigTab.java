package NewGUI;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

public class ConfigTab {

  private final AppState state;
  private final Stage owner;
  private final Runnable onSave;
  private final Runnable onLoad;
  private final Runnable onTemplatesMapChanged;

  public ConfigTab(AppState state, Stage owner, Runnable onSave,
      Runnable onLoad, Runnable onTemplatesMapChanged) {
    this.state = state;
    this.owner = owner;
    this.onSave = onSave;
    this.onLoad = onLoad;
    this.onTemplatesMapChanged = onTemplatesMapChanged;
  }

  public Node bouwInhoud() {
    // === Paden ===
    GridPane inputGrid = new GridPane();
    inputGrid.setHgap(10);
    inputGrid.setVgap(10);
    inputGrid.setPadding(new Insets(10));

    TextField excelField = new TextField();
    excelField.textProperty().bindBidirectional(state.excelBestandProperty());
    Button excelBtn = new Button("...");
    excelBtn.setOnAction(e -> {
      FileChooser fc = new FileChooser();
      fc.setTitle("Kies Excel bestand");
      fc.getExtensionFilters().addAll(
          new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"),
          new FileChooser.ExtensionFilter("Alle bestanden", "*.*"));
      File f = fc.showOpenDialog(owner);
      if (f != null) {
        state.setExcelBestand(f.getAbsolutePath());
      }
    });
    HBox excelBox = new HBox(5, excelField, excelBtn);
    HBox.setHgrow(excelField, Priority.ALWAYS);

    TextField templateDirField = new TextField();
    templateDirField.textProperty()
        .bindBidirectional(state.templatesMapProperty());
    Button templateBtn = new Button("...");
    templateBtn.setOnAction(e -> {
      DirectoryChooser dc = new DirectoryChooser();
      dc.setTitle("Kies templates map");
      File f = dc.showDialog(owner);
      if (f != null) {
        state.setTemplatesMap(f.getAbsolutePath());
        if (onTemplatesMapChanged != null) {
          onTemplatesMapChanged.run();
        }
      }
    });
    HBox templateBox = new HBox(5, templateDirField, templateBtn);
    HBox.setHgrow(templateDirField, Priority.ALWAYS);

    TextField outputDirField = new TextField();
    outputDirField.textProperty().bindBidirectional(state.outputMapProperty());
    Button outputBtn = new Button("...");
    outputBtn.setOnAction(e -> {
      DirectoryChooser dc = new DirectoryChooser();
      dc.setTitle("Kies output map");
      File f = dc.showDialog(owner);
      if (f != null) {
        state.setOutputMap(f.getAbsolutePath());
      }
    });
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

    // === Tabel ===
    TableView<ConfigEntry> table = new TableView<>(state.getRegels());
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

    // === Knoppen ===
    Button addBtn = new Button("+ Rij toevoegen");
    addBtn
        .setOnAction(e -> state.getRegels().add(new ConfigEntry("", "", true)));

    Button removeBtn = new Button("- Rij verwijderen");
    removeBtn.setOnAction(e -> {
      ConfigEntry sel = table.getSelectionModel().getSelectedItem();
      if (sel != null) {
        state.getRegels().remove(sel);
      }
    });

    Button defaultOutputBtn = new Button("Output automatisch invullen");
    defaultOutputBtn.setOnAction(e -> {
      for (ConfigEntry ce : state.getRegels()) {
        if (ce.getOutput() == null || ce.getOutput().isBlank()) {
          ce.setOutput(ConfigEntry.defaultOutputVoor(ce.getTemplate()));
        }
      }
    });

    Button saveBtn = new Button("Configuratie opslaan...");
    saveBtn.setOnAction(e -> {
      if (onSave != null) {
        onSave.run();
      }
    });

    Button loadBtn = new Button("Configuratie laden...");
    loadBtn.setOnAction(e -> {
      if (onLoad != null) {
        onLoad.run();
      }
    });

    HBox tabelKnoppen = new HBox(10, addBtn, removeBtn, defaultOutputBtn,
        saveBtn, loadBtn);
    tabelKnoppen.setPadding(new Insets(5, 0, 5, 0));

    // === Layout ===
    VBox root = new VBox(10, new Label("Instellingen en configuratie:"),
        inputGrid, new Separator(),
        new Label("Configuratie (template → output):"), tabelKnoppen, table);
    root.setPadding(new Insets(10));
    VBox.setVgrow(table, Priority.ALWAYS);
    return root;
  }
}