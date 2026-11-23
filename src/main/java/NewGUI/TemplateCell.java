package NewGUI;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Supplier;

public class TemplateCell extends TableCell<ConfigEntry, String> {

  private final TextField textField = new TextField();
  private final Button browseBtn = new Button("...");
  private final HBox box = new HBox(5, textField, browseBtn);

  private final Supplier<String> templatesMapSupplier;

  public TemplateCell(Supplier<String> templatesMapSupplier) {
    this.templatesMapSupplier = templatesMapSupplier;
    HBox.setHgrow(textField, Priority.ALWAYS);
    box.setAlignment(Pos.CENTER_LEFT);
    textField.setEditable(true);

    textField.setOnAction(e -> commitEdit(textField.getText()));
    textField.focusedProperty().addListener((obs, oud, nieuw) -> {
      if (!nieuw) {
        commitEdit(textField.getText());
      }
    });

    browseBtn.setOnAction(e -> kiesBestand());
  }

  private void kiesBestand() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Kies Velocity template");
    fc.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Velocity templates", "*.vm"),
        new FileChooser.ExtensionFilter("Alle bestanden", "*.*"));

    // Start-map bepalen: eerst huidige waarde, dan templatesMap
    String huidig = getItem();
    File startDir = null;

    if (huidig != null && !huidig.isBlank()) {
      File f = new File(huidig);
      if (f.isAbsolute() && f.getParentFile() != null
          && f.getParentFile().isDirectory()) {
        startDir = f.getParentFile();
        fc.setInitialFileName(f.getName());
      }
    }
    if (startDir == null) {
      String map = templatesMapSupplier.get();
      if (map != null && !map.isBlank()) {
        File dir = new File(map);
        if (dir.isDirectory()) {
          startDir = dir;
        }
      }
    }
    if (startDir != null) {
      fc.setInitialDirectory(startDir);
    }

    File gekozen = fc
        .showOpenDialog(getScene() == null ? null : getScene().getWindow());
    if (gekozen == null) {
      return;
    }

    // Waarschuwing als buiten de templates-map gekozen
    String map = templatesMapSupplier.get();
    if (map != null && !map.isBlank()) {
      File mapDir = new File(map);
      if (mapDir.isDirectory() && !gekozen.getParentFile().equals(mapDir)) {
        Alert a = new Alert(Alert.AlertType.WARNING,
            "Het gekozen bestand ligt buiten de templates-map:\n"
                + mapDir.getAbsolutePath() + "\n\nToch gebruiken?",
            javafx.scene.control.ButtonType.YES,
            javafx.scene.control.ButtonType.NO);
        a.setHeaderText(null);
        if (a.showAndWait().orElse(
            javafx.scene.control.ButtonType.NO) != javafx.scene.control.ButtonType.YES) {
          return;
        }
      }
    }

    // Volledig pad opslaan
    commitEdit(gekozen.getAbsolutePath());
  }

  @Override
  protected void updateItem(String item, boolean empty) {
    super.updateItem(item, empty);
    if (empty) {
      setGraphic(null);
      setText(null);
    } else {
      textField.setText(item == null ? "" : item);
      setGraphic(box);
      setText(null);
    }
    // In TemplateCell.updateItem:
    if (!empty && item != null && !item.isBlank()) {
      setTooltip(new javafx.scene.control.Tooltip(item));
    } else {
      setTooltip(null);
    }
  }

  @Override
  public void startEdit() {
    super.startEdit();
    textField.setText(getItem() == null ? "" : getItem());
    textField.requestFocus();
    textField.selectAll();
  }

  @Override
  public void cancelEdit() {
    super.cancelEdit();
    textField.setText(getItem() == null ? "" : getItem());
  }
}