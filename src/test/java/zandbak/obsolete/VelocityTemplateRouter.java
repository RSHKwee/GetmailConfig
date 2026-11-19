package kwee.getmailconfig.library;

import java.util.regex.Pattern;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class VelocityTemplateRouter {

  private final VelocityEngine velocityEngine;

  public VelocityTemplateRouter(VelocityEngine velocityEngine) {
    this.velocityEngine = velocityEngine;
  }

  /**
   * Hoofdmethode: verwerkt een template op basis van de inhoud
   * 
   * @param templatePath pad naar template-bestand
   * @param excelRows    Lijst van Maps, elke Map is één Excel-rij (kolomnaam ->
   *                     waarde)
   */
  public void processTemplate(String templatePath, List<Map<String, Object>> excelRows) throws IOException {

    // Laad template inhoud als String
    String templateContent = loadTemplateContent(templatePath);

    if (usesFormalNotation(templateContent)) {
      // Patroon A: Iteratie in Java
      processWithJavaIteration(templatePath, excelRows);
    } else if (usesInformalNotation(templateContent)) {
      // Patroon B: Iteratie in Velocity
      processWithVelocityIteration(templatePath, excelRows);
    } else {
      // Geen duidelijke indicatie - default naar Java iteratie
      System.out.println("Geen duidelijke notatie gevonden, gebruik Java iteratie");
      processWithJavaIteration(templatePath, excelRows);
    }
  }

  /**
   * Leest template bestand volledig in als String
   */
  private String loadTemplateContent(String templatePath) throws IOException {
    // Probeer eerst vanuit classpath
    InputStream is = getClass().getResourceAsStream(templatePath);
    if (is == null) {
      // Dan vanuit bestandssysteem
      return new String(Files.readAllBytes(Paths.get(templatePath)));
    }

    try (Scanner scanner = new Scanner(is, "UTF-8")) {
      return scanner.useDelimiter("\\A").next();
    }
  }

  /**
   * Detecteert formele notatie: ${VariabeleNaam} Let op: sluit $row.get("...")
   * constructies uit van detectie
   */
  private boolean usesFormalNotation(String content) {
    // Zoekt naar ${...} maar niet als er .get(" achter komt
    Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}(?!\\s*\\.get\\()");
    return pattern.matcher(content).find();
  }

  /**
   * Detecteert informele notatie: $row.get("...") of $variabele.get("...")
   */
  private boolean usesInformalNotation(String content) {
    Pattern pattern = Pattern.compile("\\$[a-zA-Z_][a-zA-Z0-9_]*\\.get\\([\"'][^\"']+[\"']\\)");
    return pattern.matcher(content).find();
  }

  /**
   * Patroon A: Java doet de iteratie, template krijgt één rij per merge
   */
  private void processWithJavaIteration(String templatePath, List<Map<String, Object>> excelRows) throws IOException {

    Template template = velocityEngine.getTemplate(templatePath);

    for (int i = 0; i < excelRows.size(); i++) {
      Map<String, Object> row = excelRows.get(i);
      VelocityContext context = new VelocityContext();

      // Plaats alle velden uit deze rij direct in de context
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        context.put(entry.getKey(), entry.getValue());
      }

      // Optioneel: voeg rijnummer toe
      context.put("rowNumber", i + 1);

      StringWriter writer = new StringWriter();
      template.merge(context, writer);

      // Opslaan per rij (voorbeeld: naar losse bestanden)
      saveOutput(writer.toString(), "output_row_" + (i + 1) + ".txt");
    }
  }

  /**
   * Patroon B: Velocity doet de iteratie, template krijgt hele lijst
   */
  private void processWithVelocityIteration(String templatePath, List<Map<String, Object>> excelRows)
      throws IOException {

    Template template = velocityEngine.getTemplate(templatePath);
    VelocityContext context = new VelocityContext();

    // Zet hele lijst in de context (template doorloopt met #foreach)
    context.put("rows", excelRows);

    // Optioneel: voeg totaal aantal rijen toe
    context.put("totalRows", excelRows.size());

    StringWriter writer = new StringWriter();
    template.merge(context, writer);

    // Opslaan als één bestand
    saveOutput(writer.toString(), "output_all.txt");
  }

  /**
   * Hulp methode om output op te slaan (aanpasbaar naar wens)
   */
  private void saveOutput(String content, String filename) throws IOException {
    Files.write(Paths.get(filename), content.getBytes());
    System.out.println("Geschreven: " + filename);
  }
}