package kwee.getmailconfig.main;

import java.util.logging.Level;
import java.util.logging.Logger;

import kwee.getmailconfig.gui.GenerationListener;
import kwee.getmailconfig.gui.GenerationService;
import kwee.logger.MyLogger;

/**
 * CLI-entrypoint: parse argumenten en roep {@link GenerationService} aan. Alle
 * logica zit in GenerationService; deze klasse regelt alleen de console-output
 * en de exit-codes.
 */
public final class CliRunner {

  private static final Logger LOGGER = MyLogger.getLogger();

  private CliRunner() {
  }

  public static int run(CliOptions opts) {
    GenerationListener listener = new GenerationListener() {
      @Override
      public void status(String message) {
        LOGGER.log(Level.INFO, message);
      }

      @Override
      public void progress(long done, long total) {
        // CLI: geen progressbar; eventueel periodiek printen
      }

      @Override
      public boolean isCancelled() {
        return false; // CLI kent geen annulering
      }

      @Override
      public void log(String message) {
        LOGGER.log(Level.INFO, message);
      }
    };

    try {
      LOGGER.log(Level.INFO, "🔄 Start generatie...");
      LOGGER.log(Level.INFO, "   Template: " + opts.template.getAbsolutePath());
      LOGGER.log(Level.INFO, "   Excel   : " + opts.excel.getAbsolutePath());
      LOGGER.log(Level.INFO, "   Output  : " + opts.output.getAbsolutePath());

      String result = GenerationService.generate(opts.template, opts.excel,
          opts.output, listener);

      LOGGER.log(Level.INFO, "✅ Opgeslagen: " + opts.output.getAbsolutePath());
      LOGGER.log(Level.INFO, "📏 Grootte: " + result.length() + " tekens");
      return 0;
    } catch (GenerationService.CancelledException ex) {
      LOGGER.log(Level.INFO, "⏹️ " + ex.getMessage());
      return 4;
    } catch (Exception ex) {
      LOGGER.log(Level.INFO, "❌ Fout: " + ex.getMessage());
      ex.printStackTrace(System.err);
      return 1;
    }
  }
}