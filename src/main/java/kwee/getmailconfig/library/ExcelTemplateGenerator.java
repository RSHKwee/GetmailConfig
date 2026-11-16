package kwee.getmailconfig.library;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ExcelTemplateGenerator {
  private final VelocityEngine velocityEngine;

  // Constructor met template path
  public ExcelTemplateGenerator(String templatePath) throws Exception {
    Properties props = new Properties();

    // Controleer of template directory bestaat
    File templateDir = new File(templatePath);
    if (!templateDir.exists()) {
      // Probeer alternatieve locaties
      String[] alternatieven = { "./templates", "./src/templates", "./src/main/resources/templates", templatePath };

      boolean found = false;
      for (String alt : alternatieven) {
        File testDir = new File(alt);
        if (testDir.exists() && testDir.isDirectory()) {
          templatePath = alt;
          found = true;
          System.out.println("Template directory gevonden: " + testDir.getAbsolutePath());
          break;
        }
      }

      if (!found) {
        throw new Exception("Template directory niet gevonden. Huidige directory: " + new File(".").getAbsolutePath());
      }
    }

    props.setProperty("resource.loader", "file");
    props.setProperty("file.resource.loader.path", templatePath);
    this.velocityEngine = new VelocityEngine(props);
    this.velocityEngine.init();
  }

  /**
   * Genereer bestanden op basis van Excel en template
   * 
   * @param excelPad        Pad naar Excel bestand
   * @param templateNaam    Naam van template bestand
   * @param outputDirectory Output directory voor gegenereerde bestanden
   * @param outputPrefix    Prefix voor gegenereerde bestanden
   * @throws Exception
   */
  public void genereerBestanden(String excelPad, String templateNaam, String outputDirectory, String outputPrefix,
      boolean bRijInFileName) throws Exception {
    // Lees Excel bestand
    ExcelLezer excel = new ExcelLezer(excelPad);
    List<Map<String, String>> excelData = excel.getExcelData();

    // Laad template
    Template template = velocityEngine.getTemplate(templateNaam, "UTF-8");

    // Genereer bestand voor elke rij
    int rijNummer = 1;
    for (Map<String, String> rijData : excelData) {
      String extentsion = "txt";
      String subdir = "";
      // Maak context met data uit de rij
      VelocityContext context = new VelocityContext();
      for (Map.Entry<String, String> entry : rijData.entrySet()) {
        context.put(entry.getKey(), entry.getValue());
        if (entry.getKey().toUpperCase().contains("PREFIX")) {
          outputPrefix = entry.getValue();
        }
        if (entry.getKey().toUpperCase().contains("EXT")) {
          extentsion = entry.getValue();
        }
        if (entry.getKey().toUpperCase().contains("SUBDIR")) {
          subdir = entry.getValue().toLowerCase();
        }
      }

      if (!subdir.isBlank()) {
        File directory = new File(outputDirectory + File.separator + subdir);
        if (!directory.exists()) {
          directory.mkdirs();
        }
        subdir = File.separator + subdir;
      } else {
        subdir = "";
      }

      // Voeg eventueel extra metadata toe
      context.put("rijNummer", String.valueOf(rijNummer));
      context.put("huidigeDatum", new Date());

      // Genereer output
      String outputBestand = "";
      String seperator = File.separator;
      if (!extentsion.isBlank()) {
        extentsion = "." + extentsion;
      }

      seperator = "-";
      extentsion = "";

      if (bRijInFileName) {
        outputBestand = outputDirectory + subdir + seperator + outputPrefix + "_" + rijNummer + extentsion;
      } else {
        outputBestand = outputDirectory + subdir + seperator + outputPrefix + extentsion;
      }
      genereerBestandVanTemplate(template, context, outputBestand);

      System.out.println("Gegenereerd: " + outputBestand);
      rijNummer++;
    }

    System.out.println("Klaar! " + (rijNummer - 1) + " bestanden gegenereerd.");
  }

  /**
   * Genereer bestand op basis van Excel en template
   * 
   * @param excelPad        Pad naar Excel bestand
   * @param templateNaam    Naam van template bestand
   * @param outputDirectory Output directory voor gegenereerde bestanden
   * @param outputPrefix    Prefix voor gegenereerde bestanden
   * @throws Exception
   */
  public void genereerBestand(String excelPad, String templateNaam, String outputDirectory, String outputPrefix,
      String extentsion, boolean bRijInFileName) throws Exception {
    // Lees Excel bestand
    ExcelLezer excel = new ExcelLezer(excelPad);
    List<Map<String, String>> excelData = excel.getExcelData();

    // Laad template
    Template template = velocityEngine.getTemplate(templateNaam, "UTF-8");
    // Zorg dat output directory bestaat
    Files.createDirectories(Paths.get(outputDirectory).getParent());
    // Genereer output
    String outputBestand = "combined";
    // String extentsion = "sh";
    outputBestand = outputDirectory + File.separator + outputPrefix + "." + extentsion;

    try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputBestand), "UTF-8")) {
      // Genereer in één bestand voor elke rij
      int rijNummer = 1;
      for (Map<String, String> rijData : excelData) {
        // Maak context met data uit de rij
        VelocityContext context = new VelocityContext();
        for (Map.Entry<String, String> entry : rijData.entrySet()) {
          String subdir = "";
          context.put(entry.getKey(), entry.getValue());
          if (entry.getKey().toUpperCase().contains("PREFIX")) {
            outputPrefix = entry.getValue();
          }
          if (entry.getKey().toUpperCase().contains("EXT")) {
            extentsion = entry.getValue();
          }
          if (entry.getKey().toUpperCase().contains("SUBDIR")) {
            subdir = entry.getValue().toLowerCase();
          }
        }
        // Voeg eventueel extra metadata toe
        context.put("huidigeDatum", new Date());

        template.merge(context, writer);
        rijNummer++;
      }
      System.out.println("Gegenereerd: " + outputBestand);
      System.out.println("Klaar! 1 bestand gegenereerd, met " + (rijNummer - 1) + " blokken.");
    } catch (Exception e) {

    }
  }

  /**
   * Genereer bestand op basis van template
   */
  private void genereerBestandVanTemplate(Template template, VelocityContext context, String outputPad)
      throws Exception {
    // Zorg dat output directory bestaat
    Files.createDirectories(Paths.get(outputPad).getParent());

    try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputPad), "UTF-8")) {
      template.merge(context, writer);
    }
  }

}