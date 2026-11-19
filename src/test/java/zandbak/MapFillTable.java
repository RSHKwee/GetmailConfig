package zandbak;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class MapFillTable extends Application {

  // Uiteindelijke doel-Map
  private final Map<String, String> targetMap = new HashMap<>();

  public static void main(String[] args) {
    launch(args);
  }

  // Rijmodel gebruikt door de tabel
  public static class MapRow {
    private final SimpleStringProperty key;
    private final SimpleStringProperty value;

    public MapRow(String key, String value) {
      this.key = new SimpleStringProperty(key);
      this.value = new SimpleStringProperty(value);
    }

    public String getKey() {
      return key.get();
    }

    public SimpleStringProperty keyProperty() {
      return key;
    }

    public void setKey(String k) {
      key.set(k);
    }

    public String getValue() {
      return value.get();
    }

    public SimpleStringProperty valueProperty() {
      return value;
    }

    public void setValue(String v) {
      value.set(v);
    }
  }

  @Override
  public void start(Stage stage) {
    TableView<MapRow> table = new TableView<>();
    table.setEditable(true);

    // ObservableList van het tabelmodel
    ObservableList<MapRow> tableData = FXCollections.observableArrayList();

    // === Kolom: Sleutel (String A) ===
    TableColumn<MapRow, String> keyCol = new TableColumn<>("Sleutel (A)");
    keyCol.setCellValueFactory(cell -> cell.getValue().keyProperty());
    keyCol.setCellFactory(TextFieldTableCell.forTableColumn());
    keyCol.setEditable(true);

    // Wanneer de sleutel wordt bewerkt, synchroniseer naar de Map
    keyCol.setOnEditCommit(event -> {
      MapRow row = event.getRowValue();
      String oldKey = row.getKey();
      String newKey = event.getNewValue();
      row.setKey(newKey);
      // Verwijder de oude sleutel en voeg de nieuwe toe (let op: als de nieuwe
      // sleutel al bestaat, wordt de oude waarde overschreven)
      targetMap.remove(oldKey);
      targetMap.put(newKey, row.getValue());
    });

    // === Kolom: Waarde (String B) ===
    TableColumn<MapRow, String> valueCol = new TableColumn<>("Waarde (B)");
    valueCol.setCellValueFactory(cell -> cell.getValue().valueProperty());
    valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
    valueCol.setEditable(true);

    // Wanneer de waarde wordt bewerkt, synchroniseer naar de Map
    valueCol.setOnEditCommit(event -> {
      MapRow row = event.getRowValue();
      row.setValue(event.getNewValue());
      targetMap.put(row.getKey(), event.getNewValue());
    });

    table.getColumns().addAll(keyCol, valueCol);
    table.setItems(tableData);

    // Knoppen: nieuwe rij toevoegen / Map afdrukken
    Button addBtn = new Button("Nieuwe rij toevoegen");
    addBtn.setOnAction(e -> {
      // Voeg standaard een lege rij toe; de gebruiker vult deze in en de
      // gebeurtenis wordt geactiveerd
      tableData.add(new MapRow("", ""));
    });

    Button printBtn = new Button("Huidige Map afdrukken");
    printBtn.setOnAction(e -> {
      System.out.println("=== Huidige Map ===");
      targetMap.forEach((k, v) -> System.out.println(k + " -> " + v));
    });

    VBox root = new VBox(10, table, addBtn, printBtn);
    stage.setScene(new Scene(root, 400, 300));
    stage.show();
  }
}
