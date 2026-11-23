package NewGUI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

public class ConfigEntry {

  private final StringProperty template = new SimpleStringProperty("");
  private final StringProperty output = new SimpleStringProperty("");
  private final BooleanProperty eenBestand = new SimpleBooleanProperty(true);

  public ConfigEntry() {
  }

  public ConfigEntry(String template, String output, boolean eenBestand) {
    this.template.set(template);
    this.output.set(output);
    this.eenBestand.set(eenBestand);
  }

  @JsonIgnore
  public StringProperty templateProperty() {
    return template;
  }

  @JsonIgnore
  public StringProperty outputProperty() {
    return output;
  }

  @JsonIgnore
  public BooleanProperty eenBestandProperty() {
    return eenBestand;
  }

  public String getTemplate() {
    return template.get();
  }

  public void setTemplate(String v) {
    template.set(v);
  }

  public String getOutput() {
    return output.get();
  }

  public void setOutput(String v) {
    output.set(v);
  }

  public boolean isEenBestand() {
    return eenBestand.get();
  }

  public void setEenBestand(boolean v) {
    eenBestand.set(v);
  }

  /**
   * Leidt een standaard output-bestandsnaam af uit de template-naam.
   * Bijvoorbeeld: "factuur.vm" -> "factuur.txt"
   */
  public static String defaultOutputVoor(String templateNaam) {
    if (templateNaam == null || templateNaam.isBlank()) {
      return "";
    }
    String base = templateNaam;
    int dot = base.lastIndexOf('.');
    if (dot > 0) {
      base = base.substring(0, dot);
    }
    return base + ".txt";
  }

  // --- Jackson ---

  @JsonProperty("template")
  private String jsonTemplate() {
    return template.get();
  }

  @JsonProperty("template")
  private void jsonTemplate(String v) {
    template.set(v);
  }

  @JsonProperty("output")
  private String jsonOutput() {
    return output.get();
  }

  @JsonProperty("output")
  private void jsonOutput(String v) {
    output.set(v);
  }

  @JsonProperty("eenBestand")
  private boolean jsonEenBestand() {
    return eenBestand.get();
  }

  @JsonProperty("eenBestand")
  private void jsonEenBestand(boolean v) {
    eenBestand.set(v);
  }
}