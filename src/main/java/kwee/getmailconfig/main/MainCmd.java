package kwee.getmailconfig.main;

import java.io.File;
import java.util.List;

import kwee.getmailconfig.gui.GenerationService;
import kwee.getmailconfig.gui.GenerationService.PerRowOptions;
import kwee.getmailconfig.library.AccountMigrator;
import kwee.getmailconfig.library.ExcelTemplateGenerator;
import kwee.getmailconfig.library.MailCheckAndClean;
import kwee.getmailconfig.library.MailMover;

public class MainCmd {
  public static UserSetting m_param = new UserSetting();

  /**
   * Main methode met voorbeeld
   */
  public static void main(String[] args) {
    // String rootdir = "./src/main/resources"; D:\Dev\LocalGit\GetmailConfig
    String rootdir = String.join(File.separator, "D:", "Dev", "LocalGit",
        "GetmailConfig");
    String templDir = rootdir + File.separator + "templates";
    String dataDir = rootdir + File.separator + "data" + File.separator;
    String outputDirectory = rootdir + File.separator
        + String.join(File.separator, ".", "output");

    String dataXlsx = "GetmailData.xlsx";
    String templateNaam = "mbsyncPassw.vm";

    boolean b_accountConfig = false;
    boolean b_aclConfig = false;
    boolean b_mailDirs = false;
    boolean b_mailMover = false;
    boolean b_mailCheck = false;

    // Per rij een file
    boolean b_pwdConfig = true;

    // Alles in één bestand
    boolean b_mbsyncConfig = false;
    boolean b_mbsyncTestConfig = false;
    boolean b_sslPwdConfig = false;
    boolean b_senderRelayConfig = false;

    try {
      // Voorbeeld parameters
      String excelPad = dataDir + dataXlsx;

      String outputPrefix = "document";
      if (b_accountConfig) {
        AccountMigrator accountmig = new AccountMigrator(excelPad);
        accountmig.generateAccountFile(templDir, "imap-folder-tree.vm",
            outputDirectory, "summary.tree", false);
      }
      if (b_aclConfig) {
        AccountMigrator accountmig = new AccountMigrator(excelPad);
        accountmig.generateAccountFile(templDir, "acl_dovecot.vm",
            outputDirectory, "doveAdmAcl.sh", true);
      }
      if (b_pwdConfig) {
        PerRowOptions opts = new PerRowOptions(new File(outputDirectory));
        opts.subdirColumn = "Subdir";
        opts.prefixColumn = "Prefix";
        opts.extColumn = "NoExtensie";
        opts.defaultExt = "html";
        opts.allowEmptyExtension = true;
        opts.exposeRowNumber = false;

        templDir = rootdir + File.separator + "templates" + File.separator
            + "Mbsync" + File.separator + "mbsyncPassw.vm";

        List<File> files = GenerationService.generatePerRow(new File(templDir),
            new File(excelPad), opts, null);

        System.out.println(files);
      }
      if (b_mbsyncConfig) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator
            + "Mbsync";
        templateNaam = "mbsync.vm";

        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        outputPrefix = ".mbsyncrc";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory,
            outputPrefix, "", false);
      }
      if (b_mbsyncTestConfig) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator
            + "Mbsync";
        templateNaam = "mbsyncTest.vm";

        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        outputPrefix = "test.mbsyncrc";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory,
            outputPrefix, "", false);
      }
      if (b_mailDirs) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates";
        templateNaam = "maildirs.vm";

        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        outputPrefix = "maildirs";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory,
            outputPrefix, "sh", false);
      }
      if (b_sslPwdConfig) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator
            + "smtp";
        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);

        templateNaam = "sasl_passwd.vm";
        outputPrefix = "sasl_passwd";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory,
            outputPrefix, "", false);
      }
      if (b_senderRelayConfig) {
        // Gebruik de constructor met template pad
        templDir = rootdir + File.separator + "templates" + File.separator
            + "smtp";
        ExcelTemplateGenerator generator = new ExcelTemplateGenerator(templDir);
        templateNaam = "sender_relay.vm";
        outputPrefix = "sender_relay";
        generator.genereerBestand(excelPad, templateNaam, outputDirectory,
            outputPrefix, "", false);
      }
      if (b_mailMover) {
        MailMover mover = new MailMover(excelPad, false);
        mover.checkPorviders();
      }
      if (b_mailCheck) {
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
