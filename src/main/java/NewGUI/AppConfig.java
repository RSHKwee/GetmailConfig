package NewGUI;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class AppConfig {

  @JsonProperty("excelBestand")
  private String excelBestand = "";

  @JsonProperty("templatesMap")
  private String templatesMap = "";

  @JsonProperty("outputMap")
  private String outputMap = "";

  @JsonProperty("regels")
  private List<ConfigEntry> regels = new ArrayList<>();

  public String getExcelBestand() {
    return excelBestand;
  }

  public void setExcelBestand(String v) {
    this.excelBestand = v;
  }

  public String getTemplatesMap() {
    return templatesMap;
  }

  public void setTemplatesMap(String v) {
    this.templatesMap = v;
  }

  public String getOutputMap() {
    return outputMap;
  }

  public void setOutputMap(String v) {
    this.outputMap = v;
  }

  public List<ConfigEntry> getRegels() {
    return regels;
  }

  public void setRegels(List<ConfigEntry> v) {
    this.regels = v != null ? v : new ArrayList<>();
  }
}