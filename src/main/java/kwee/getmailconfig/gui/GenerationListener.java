package kwee.getmailconfig.gui;

import java.io.File;

/**
 * Callback voor voortgang en annulering tijdens het genereren.
 * Implementaties mogen niet aannemen dat ze op de FX-thread draaien.
 */
public interface GenerationListener {

  /** Korte statusregel (voor GUI: statusLabel; voor CLI: System.out). */
  void status(String message);

  /** Voortgang: done van total. total <= 0 betekent "onbepaald". */
  void progress(long done, long total);

  /** Moet de generatie stoppen? */
  boolean isCancelled();

  /** Een logregel (voor GUI: LOGGER; voor CLI: System.out). */
  void log(String message);

  /** Een fout die niet fataal is (wordt gelogd, generatie gaat door). */
  default void warn(String message) {
    log("⚠️ " + message);
  }

  /** Een no-op listener, handig als je geen voortgang nodig hebt. */
  GenerationListener NONE = new GenerationListener() {
    @Override public void status(String message) { }
    @Override public void progress(long done, long total) { }
    @Override public boolean isCancelled() { return false; }
    @Override public void log(String message) { }
  };
}