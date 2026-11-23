package NewGUI;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.util.function.IntConsumer;

public class VerwerkingTab {

  private final AppState state;
  private final Stage owner;
  private final IntConsumer onVerwerking;
  private final Runnable onTemplatesMapChanged;

  private final ListView<ConfigEntry> templateList;
  private final TextField gekozenOutputField = new TextField();
  private final Label gekozenTemplateLabel = new Label("(geen selectie)");

  public VerwerkingTab(AppState state, Stage owner, IntConsumer onVerwerking,
      Runnable onTemplatesMapChanged) {
    this.state = state;
    this.owner = owner;
    this.onVerwerking = onVerwerking;
    this.onTemplatesMapChanged = onTemplatesMapChanged;
    this.templateList = new ListView<>(state.getRegels());
  }

  public Node bouwInhoud() {
    // Cell factory
    templateList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(ConfigEntry ce, boolean empty) {
        super.updateItem(ce, empty);
        if (empty || ce == null) {
          setText(null);
        } else {
          String naam = new File(ce.getTemplate()).getName();
          String richting = ce.isEenBestand() ? "één bestand" : "per rij";
          setText(naam + "  →  " + ce.getOutput() + "  (" + richting + ")");
          setTooltip(new javafx.scene.control.Tooltip(ce.getTemplate()));
        }
      }
    });
    templateList.getSelectionModel().selectedItemProperty()
        .addListener((obs, oud, nieuw) -> toonGekozenTemplate(nieuw));

    // Ververs de lijst-cellen als de tabel in de andere tab wijzigt
    // (JavaFX ListView luistert standaard niet naar property-wijzigingen
    // binnen items; forceer een refresh via een listener op de lijst.)
    state.getRegels().addListener(
        (javafx.collections.ListChangeListener<ConfigEntry>) c -> templateList
            .refresh());

    VBox templateBox = new VBox(5,
        new Label("Kies een template uit de configuratie:"), templateList,
        new Label("Gekozen template:"), gekozenTemplateLabel,
        new Label("Output bestand (aanpasbaar):"), gekozenOutputField);
    VBox.setVgrow(templateList, Priority.ALWAYS);

    // 10 verwerkingsknoppen
    GridPane buttonGrid = new GridPane();
    buttonGrid.setHgap(10);
    buttonGrid.setVgap(10);
    buttonGrid.setPadding(new Insets(10, 0, 0, 0));

    for (int i = 1; i <= 10; i++) {
      final int nr = i;
      Button b = new Button("Verwerking " + nr);
      b.setMaxWidth(Double.MAX_VALUE);
      b.setPrefHeight(40);
      b.setOnAction(e -> onVerwerking.accept(nr));
      buttonGrid.add(b, (i - 1) % 5, (i - 1) / 5);
    }
    for (int c = 0; c < 5; c++) {
      ColumnConstraints cc = new ColumnConstraints();
      cc.setHgrow(Priority.ALWAYS);
      cc.setFillWidth(true);
      buttonGrid.getColumnConstraints().add(cc);
    }

    VBox root = new VBox(10, templateBox, buttonGrid);
    root.setPadding(new Insets(10));
    VBox.setVgrow(templateBox, Priority.ALWAYS);
    return root;
  }

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

  /** Wordt door de hoofdapp aangeroepen na het inlezen van de templates-map. */
  public void ververs() {
    templateList.refresh();
  }

  // Handig voor de hoofdapp om de gekozen waarden op te halen
  public ConfigEntry getGekozenTemplate() {
    return templateList.getSelectionModel().getSelectedItem();
  }

  public String getGekozenOutput() {
    return gekozenOutputField.getText();
  }
}
