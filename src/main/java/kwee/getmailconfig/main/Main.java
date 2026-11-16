package kwee.getmailconfig.main;

import java.util.logging.Logger;

import kwee.library.JarInfo;
import kwee.logger.MyLogger;
import kwee.getmailconfig.gui.GuiLayout;

/**
 * Main program for GetMailConfig.
 *
 * <pre>
 * Gebruik:
 *   (geen args)                         -> GUI starten
 *   --help | -h                         -> deze uitleg tonen
 *   <template> <excel> <output>         -> headless genereren
 *
 * Opties (in elke volgorde, vóór of na de bestanden):
 *   --template <file.vm>                -> template bestand
 *   --excel    <file.xlsx>              -> Excel databron
 *   --output   <file.txt>               -> output bestand
 * </pre>
 */
public class Main {
  private static final Logger LOGGER = MyLogger.getLogger();
  static public String m_creationtime;
  static public String c_CopyrightYear;

  /**
   * Hoofdprogramma.
   * 
   * @param args
   */
  public static void main(String[] args) {
    m_creationtime = JarInfo.getProjectVersion(Main.class);
    c_CopyrightYear = JarInfo.getYear(Main.class);

    if (args == null || args.length == 0) {
      // Geen argumenten -> GUI
      GuiLayout.main(new String[0]);
      return;
    }

    // Help?
    for (String a : args) {
      if ("--help".equals(a) || "-h".equals(a) || "-?".equals(a)) {
        printUsage();
        return;
      }
    }

    // CLI-modus: parse args
    CliOptions opts;
    try {
      opts = CliOptions.parse(args);
    } catch (IllegalArgumentException ex) {
      System.err.println("❌ " + ex.getMessage());
      System.err.println();
      printUsage();
      System.exit(2);
      return;
    }

    int exit = CliRunner.run(opts);
    System.exit(exit);
  }

  /**
   * Print de Help
   */
  private static void printUsage() {
    System.out.println(
        """
            Gebruik:
              (geen args)                     Start de GUI
              --help | -h | -?                Toon deze uitleg
              <template> <excel> <output>     Genereer output zonder GUI

            Opties (vrije volgorde):
              --template <file.vm|.txt>       Velocity template
              --excel    <file.xlsx|.xls>     Excel databron
              --output   <file.txt>           Output bestand

            Voorbeelden:
              java -jar GetMailConfig.jar
              java -jar GetMailConfig.jar --template mail.vm --excel data.xlsx --output out.txt
              java -jar GetMailConfig.jar mail.vm data.xlsx out.txt
            """);
  }
}