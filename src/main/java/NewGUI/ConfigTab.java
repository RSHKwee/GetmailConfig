package NewGUI;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ConfigTab {

    private final ObservableList<ConfigEntry> configData;

    public ConfigTab(ObservableList<ConfigEntry> configData) {
        this.configData = configData;
    }

    public Node bouwInhoud() {
        TableView<ConfigEntry> table = new TableView<>(configData);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ConfigEntry, String> templateCol = new TableColumn<>("Velocity template");
        templateCol.setCellValueFactory(c -> c.getValue().templateProperty());
        templateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        templateCol.setOnEditCommit(ev -> ev.getRowValue().setTemplate(ev.getNewValue()));
        templateCol.setPrefWidth(250);

        TableColumn<ConfigEntry, String> outputCol = new TableColumn<>("Output bestand");
        outputCol.setCellValueFactory(c -> c.getValue().outputProperty());
        outputCol.setCellFactory(TextFieldTableCell.forTableColumn());
        outputCol.setOnEditCommit(ev -> ev.getRowValue().setOutput(ev.getNewValue()));
        outputCol.setPrefWidth(250);

        TableColumn<ConfigEntry, Boolean> singleCol = new TableColumn<>("Eén bestand");
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
            if (sel != null) configData.remove(sel);
        });

        Button defaultOutputBtn = new Button("Output automatisch invullen");
        defaultOutputBtn.setOnAction(e -> {
            for (ConfigEntry ce : configData) {
                if (ce.getOutput() == null || ce.getOutput().isBlank()) {
                    ce.setOutput(ConfigEntry.defaultOutputVoor(ce.getTemplate()));
                }
            }
        });

        HBox knoppen = new HBox(10, addBtn, removeBtn, defaultOutputBtn);
        knoppen.setPadding(new Insets(5, 0, 5, 0));

        VBox root = new VBox(5,
                new Label("Configuratie (template → output):"),
                knoppen,
                table);
        root.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        return root;
    }
}