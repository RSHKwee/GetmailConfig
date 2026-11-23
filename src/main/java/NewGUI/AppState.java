package NewGUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AppState {

    private final StringProperty excelBestand = new SimpleStringProperty("");
    private final StringProperty templatesMap = new SimpleStringProperty("");
    private final StringProperty outputMap = new SimpleStringProperty("");

    private final ObservableList<ConfigEntry> regels = FXCollections.observableArrayList();

    // Getters voor properties
    public StringProperty excelBestandProperty() { return excelBestand; }
    public StringProperty templatesMapProperty() { return templatesMap; }
    public StringProperty outputMapProperty() { return outputMap; }
    public ObservableList<ConfigEntry> getRegels() { return regels; }

    // Handige accessors
    public String getExcelBestand() { return excelBestand.get(); }
    public void setExcelBestand(String v) { excelBestand.set(v); }
    public String getTemplatesMap() { return templatesMap.get(); }
    public void setTemplatesMap(String v) { templatesMap.set(v); }
    public String getOutputMap() { return outputMap.get(); }
    public void setOutputMap(String v) { outputMap.set(v); }

    // --- Conversie naar/van AppConfig ---
    public AppConfig naarConfig() {
        AppConfig cfg = new AppConfig();
        cfg.setExcelBestand(getExcelBestand());
        cfg.setTemplatesMap(getTemplatesMap());
        cfg.setOutputMap(getOutputMap());
        cfg.setRegels(new java.util.ArrayList<>(regels));
        return cfg;
    }

    public void vanuitConfig(AppConfig cfg) {
        setExcelBestand(cfg.getExcelBestand());
        setTemplatesMap(cfg.getTemplatesMap());
        setOutputMap(cfg.getOutputMap());
        regels.setAll(cfg.getRegels());
    }
}
