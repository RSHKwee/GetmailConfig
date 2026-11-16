package kwee.getmailconfig.main;

import java.io.File;

public class CliOptions {
  public File template;
  public File excel;
  public File output;

  /**
   * Ondersteunt:
   *   <template> <excel> <output>
   *   --template X --excel Y --output Z
   *   een mix (positionele vullen de eerste vrije sleutel)
   */
  public static CliOptions parse(String[] args) {
    CliOptions o = new CliOptions();

    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      switch (a) {
      case "--template" -> o.template = new File(requireValue(args, ++i, a));
      case "--excel"    -> o.excel    = new File(requireValue(args, ++i, a));
      case "--output"   -> o.output   = new File(requireValue(args, ++i, a));
      default -> {
        if (a.startsWith("-")) {
          throw new IllegalArgumentException("Onbekende optie: " + a);
        }
        // positioneel
        if (o.template == null) {
          o.template = new File(a);
        } else if (o.excel == null) {
          o.excel = new File(a);
        } else if (o.output == null) {
          o.output = new File(a);
        } else {
          throw new IllegalArgumentException(
              "Te veel positionele argumenten: " + a);
        }
      }
      }
    }

    if (o.template == null || o.excel == null || o.output == null) {
      throw new IllegalArgumentException(
          "Template, Excel en output zijn alle drie verplicht in CLI-modus.");
    }
    if (!o.template.isFile()) {
      throw new IllegalArgumentException(
          "Template bestand niet gevonden: " + o.template.getAbsolutePath());
    }
    if (!o.excel.isFile()) {
      throw new IllegalArgumentException(
          "Excel bestand niet gevonden: " + o.excel.getAbsolutePath());
    }
    return o;
  }

  private static String requireValue(String[] args, int i, String opt) {
    if (i >= args.length || args[i].startsWith("-")) {
      throw new IllegalArgumentException("Optie " + opt + " vereist een waarde.");
    }
    return args[i];
  }
}
