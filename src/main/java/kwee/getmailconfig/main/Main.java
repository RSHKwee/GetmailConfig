package kwee.getmailconfig.main;

import java.io.File;

import kwee.getmailconfig.main.UserSetting;
import kwee.getmailconfig.library.AccountMigrator;
import kwee.getmailconfig.library.ExcelTemplateGenerator;
import kwee.getmailconfig.library.MailCheckAndClean;
import kwee.getmailconfig.library.MailMover;

public class Main {
  public static UserSetting m_param = new UserSetting();

  /**
   * Main methode met voorbeeld
   */
  public static void main(String[] args) {
    // String rootdir = "./src/main/resources";
    String rootdir = String.join(File.separator, ".", "src", "main", "resources");
    String templDir = rootdir + File.separator + "templates";
    String dataDir = rootdir + File.separator + "data" + File.separator;

    String dataXlsx = "GetmailData.xlsx";
    String templateNaam = "mbsyncPassw.vm";

    try {
      // Voorbeeld parameters
      String excelPad = dataDir + dataXlsx;
      String outputDirectory = String.join(File.separator, ".", "target", "output");
      String outputPrefix = "document";
      if (false) {
        AccountMigrator accountmig = new AccountMigrator(excelPad);
        accountmig.generateAccountFile(templDir, outputDirectory);
      }
      if (false) {
        // Roep de methode aan
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator + "Mbsync";
        templateNaam = "mbsyncPassw.vm";

        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        generator.genereerBestanden(excelPad, templateNaam, outputDirectory, outputPrefix, false);
      }
      if (true) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator + "Mbsync";
        templateNaam = "mbsync.vm";

        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        outputPrefix = ".mbsyncrc";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory, outputPrefix, "", false);
      }
      if (true) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator + "Mbsync";
        templateNaam = "mbsyncTest.vm";

        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        outputPrefix = "test.mbsyncrc";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory, outputPrefix, "", false);
      }
      if (false) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates";
        templateNaam = "maildirs.vm";

        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        outputPrefix = "maildirs";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory, outputPrefix, "sh", false);
      }
      if (false) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator + "smtp";
        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);

        templateNaam = "sasl_passwd.vm";
        outputPrefix = "sasl_passwd";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory, outputPrefix, "", false);
      }
      if (false) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator + "smtp";
        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        templateNaam = "sender_relay.vm";
        outputPrefix = "sender_relay";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory, outputPrefix, "", false);
      }
      if (false) {
        MailMover mover = new MailMover(excelPad);
        mover.checkPorviders();
      }
      if (false) {
        MailCheckAndClean mailCheck = new MailCheckAndClean(excelPad);
        mailCheck.checkPorviders();
      }
    } catch (

    Exception e) {
      System.err.println("Fout bij genereren: " + e.getMessage());
      e.printStackTrace();
    }
    System.out.println("Klaar. ");

  }

}
