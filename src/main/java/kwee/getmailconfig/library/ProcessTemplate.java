package kwee.getmailconfig.library;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class ProcessTemplate {
  private final VelocityEngine velocityEngine;

  public ProcessTemplate(String templatePath) {
    Properties props = new Properties();
    props.setProperty("resource.loader", "file");
    props.setProperty("file.resource.loader.path", templatePath);
    this.velocityEngine = new VelocityEngine(props);
    this.velocityEngine.init();
  }

  /**
   * Patroon A: Java doet de iteratie, template krijgt één rij per merge
   */
  public void processWithJavaIteration(String a_template, List<Map<String, Object>> excelRows) throws IOException {
    Template template = velocityEngine.getTemplate(a_template);

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
  public void processWithVelocityIteration(String templatePath, List<Map<String, Object>> excelRows)
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
