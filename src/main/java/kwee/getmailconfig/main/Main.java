package kwee.getmailconfig.main;

import java.io.File;

import kwee.getmailconfig.library.AccountMigrator;
import kwee.getmailconfig.library.ExcelTemplateGenerator;

public class Main {
  /**
   * Main methode met voorbeeld
   */
  public static void main(String[] args) {
    // String rootdir = "./src/main/resources";
    String rootdir = String.join(File.separator, ".", "src", "main", "resources");
    String templDir = rootdir + File.separator + "templates";
    String dataDir = rootdir + File.separator + "data" + File.separator;

//	      String dataXlsx = "Data.xlsx";
//	      String templateNaam = "Template_1.vm";

    String dataXlsx = "GetmailData.xlsx";
//    String templateNaam = "GetmailConf.vm";
//    String templateNaam2 = "GetMailCommand.vm";

    String templateNaam = "mbsyncPassw.vm";
    String templateNaam2 = "mbsyncTest.vm";
    String templateNaam3 = "maildirs.vm";

    try {
      // Gebruik de constructor met template pad
      ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);

      // Voorbeeld parameters
      String excelPad = dataDir + dataXlsx;
      String outputDirectory = String.join(File.separator, ".", "target", "output");
      String outputPrefix = "document";

      // Roep de methode aan
      generator.genereerBestanden(excelPad, templateNaam, outputDirectory, outputPrefix, false);

      outputPrefix = ".mbsyncrc";
      generator.genereerBestand(excelPad, templateNaam2, outputDirectory, outputPrefix, "", false);

      outputPrefix = "maildirs";
      generator.genereerBestand(excelPad, templateNaam3, outputDirectory, outputPrefix, "sh", false);

      AccountMigrator accountmig = new AccountMigrator(excelPad);
      accountmig.generateAccountFile(templDir, outputDirectory);

    } catch (Exception e) {
      System.err.println("Fout bij genereren: " + e.getMessage());
      e.printStackTrace();
    }
    System.out.println("Klaar. ");

  }

}
