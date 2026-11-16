package kwee.getmailconfig.gui;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import kwee.getmailconfig.main.CliRunner;
import kwee.getmailconfig.main.GenerationListener;
import kwee.getmailconfig.main.GenerationService;
import kwee.getmailconfig.main.Main;
import kwee.getmailconfig.main.UserSetting;
import kwee.getmailconfig.library.MessageConstants;

import kwee.library.ApplicationMessages;
import kwee.library.FX.AboutWindow;
import kwee.library.FX.JavaFXLogHandler;
import kwee.library.FX.ShowPreferences;
import kwee.logger.LoggerPrintErrStream;
import kwee.logger.LoggerPrintOutStream;
import kwee.logger.MyLogger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

public class GuiLayout extends Application {
  private static final Logger LOGGER = MyLogger.getLogger();
  static final String m_HelpFile = "MailConfig.chm";
  private Level m_Level = Level.INFO;
  private final String[] c_levels = { "OFF", "SEVERE", "WARNING", "INFO",
      "CONFIG", "FINE", "FINER", "FINEST", "ALL" };

  static String m_creationtime = Main.m_creationtime;
  static final String c_CopyrightYear = Main.c_CopyrightYear;

  private static final String c_reponame = "GetMailConfig";
  public static UserSetting m_param = new UserSetting();

  private String m_Language = "en";
  private ApplicationMessages bundle = ApplicationMessages.getInstance();

  private String m_Logdir = "c:\\";
  private boolean m_toDisk = false;

  private File m_templateFile = null;
  private File m_excelFile = null;
  private File m_outputFile = null;

  // UI-elementen
  private TextArea logArea;
  private Label statusLabel;

  // Task-state
  private Task<String> currentTask = null;
  private Button outputButton;
  private Button cancelButton;
  private ProgressBar progressBar;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("Apache Velocity + Excel Generator");
    m_param = UserSetting.getInstance();

    m_templateFile = new File(m_param.get_TemplateFile());
    m_excelFile = new File(m_param.get_ExcelFile());
    m_outputFile = new File(m_param.get_OutputFile());

    // Redirect Out- and Error stream to Logger
    try {
      LoggerPrintOutStream.redirectOutStreamToLogger(LOGGER, Level.WARNING);
      LoggerPrintErrStream.redirectErrorStreamToLogger(LOGGER, Level.WARNING);
    } catch (Exception e) {
      // ignore
    }

    // Defaults
    m_Language = m_param.get_Language();
    LOGGER.log(Level.FINE, "m_param.get_Language: " + m_Language);
    bundle = ApplicationMessages.getInstance();
    bundle.changeLanguage(m_Language);
    if ((m_Level == Level.FINE) || (m_Level == Level.FINER)
        || (m_Level == Level.FINEST)) {
      bundle.dumpBundle();
    }

    m_Logdir = m_param.get_LogDir();
    m_toDisk = m_param.is_toDisk();

    // Eén TextArea voor zowel logger-output als eigen log(...)-berichten.
    logArea = new TextArea();
    logArea.setEditable(false);
    logArea.setPrefHeight(200);

    // Logger setup + JavaFX handler op dezelfde TextArea
    try {
      MyLogger.setup(m_Level, m_Logdir, m_toDisk);
      JavaFXLogHandler fxLogHandler = new JavaFXLogHandler(logArea);
      fxLogHandler.setLevel(Level.ALL);
      LOGGER.addHandler(fxLogHandler);
    } catch (Exception e1) {
      e1.printStackTrace();
      log("⚠️ Logger setup mislukt: " + e1.getMessage());
    }

    // Menubar
    MenuBar menubar = doTheMenuBar(primaryStage);

    // Buttons & Labels
    Label templateLabel = new Label("Template bestand:");
    Button templateButton = new Button("Kies template (.vm/.txt)");
    Label templatePath = new Label("Geen bestand gekozen");

    Label excelLabel = new Label("Excel databron:");
    Button excelButton = new Button("Kies Excel bestand (.xlsx/.xls)");
    Label excelPath = new Label("Geen bestand gekozen");

    Label outputLabel = new Label("Output bestand:");
    outputButton = new Button("Genereer en opslaan als...");
    outputButton.setDisable(true);

    cancelButton = new Button("Annuleer");
    cancelButton.setDisable(true);
    cancelButton.setOnAction(e -> {
      if (currentTask != null) {
        currentTask.cancel();
      }
    });

    // Status + voortgang
    statusLabel = new Label("Gereed.");
    statusLabel.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(statusLabel, Priority.ALWAYS);

    progressBar = new ProgressBar(0);
    progressBar.setPrefWidth(200);

    HBox statusRow = new HBox(10, statusLabel, progressBar);
    statusRow.setPadding(new Insets(0));

    excelButton.setOnAction(e -> {
      FileChooser fileChooser = new FileChooser();
      if (m_excelFile != null && m_excelFile.getParentFile() != null) {
        fileChooser.setInitialDirectory(m_excelFile.getParentFile());
      } else if (m_templateFile != null
          && m_templateFile.getParentFile() != null) {
        fileChooser.setInitialDirectory(m_templateFile.getParentFile());
      }

      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
          "Excel bestanden", "*.xlsx", "*.xls"));
      File selected = fileChooser.showOpenDialog(primaryStage);
      if (selected != null) {
        m_excelFile = selected;
        excelPath.setText(selected.getAbsolutePath());
        m_param.set_ExcelFile(selected.getAbsolutePath());
        m_param.save();
        outputButton
            .setDisable(!(m_templateFile != null && m_excelFile != null));
      }
    });

    templateButton.setOnAction(e -> {
      FileChooser fileChooser = new FileChooser();
      if (m_templateFile != null && m_templateFile.getParentFile() != null) {
        fileChooser.setInitialDirectory(m_templateFile.getParentFile());
      } else if (m_excelFile != null && m_excelFile.getParentFile() != null) {
        fileChooser.setInitialDirectory(m_excelFile.getParentFile());
      }

      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
          "Velocity templates", "*.vm", "*.txt", "*.*"));
      File selected = fileChooser.showOpenDialog(primaryStage);
      if (selected != null) {
        m_templateFile = selected;
        templatePath.setText(selected.getAbsolutePath());
        m_param.set_TemplateFile(selected.getAbsolutePath());
        m_param.save();
        outputButton
            .setDisable(!(m_templateFile != null && m_excelFile != null));
      }
    });

    outputButton.setOnAction(e -> onGenerateClicked(primaryStage));

    VBox content = new VBox(10, excelLabel, excelButton, excelPath,
        templateLabel, templateButton, templatePath, new Separator(),
        outputLabel, new HBox(10, outputButton, cancelButton), statusRow,
        new Label("Log:"), logArea);
    content.setPadding(new Insets(15));
    VBox.setVgrow(logArea, Priority.ALWAYS);

    // Initiele waarden tonen indien reeds bekend.
    if (m_excelFile != null && m_excelFile.exists()) {
      excelPath.setText(m_excelFile.getAbsolutePath());
    }
    if (m_templateFile != null && m_templateFile.exists()) {
      templatePath.setText(m_templateFile.getAbsolutePath());
    }
    outputButton.setDisable(!(m_templateFile != null && m_excelFile != null));

    // --- Root ---
    BorderPane root = new BorderPane();
    root.setTop(menubar);
    root.setCenter(content);
    Scene scene = new Scene(root, 720, 650);

    primaryStage.setScene(scene);
    primaryStage.setOnCloseRequest(ev -> {
      if (currentTask != null && currentTask.isRunning()) {
        currentTask.cancel();
      }
    });
    primaryStage.show();
  }

  /**
   * Start de generatie-Task. De file chooser en UI-koppelingen blijven op de
   * FX-thread; het zware werk gebeurt in de Task.
   */
  private void onGenerateClicked(Stage primaryStage) {
    if (m_templateFile == null || m_excelFile == null) {
      LOGGER.log(Level.WARNING, "Eerst template én Excel bestand kiezen!");
      return;
    }

    FileChooser saver = new FileChooser();
    if (m_outputFile != null && m_outputFile.getParentFile() != null) {
      saver.setInitialDirectory(m_outputFile.getParentFile());
    } else if (m_templateFile.getParentFile() != null) {
      saver.setInitialDirectory(m_templateFile.getParentFile());
    }
    saver.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Tekstbestand", "*.txt", "*.*"));
    saver.setInitialFileName(m_outputFile.getName());

    File outputFile = saver.showSaveDialog(primaryStage);
    if (outputFile == null) {
      return;
    }

    m_param.set_OutputFile(outputFile.getAbsolutePath());
    m_param.save();

    final Task<String> task = createGenerateTask(outputFile);
    currentTask = task;

    // ---- Optie A: updateMessage -> statusLabel (geen logboek) ----
    statusLabel.textProperty().bind(task.messageProperty());

    // Voortgang: indeterminate tijdens Excel-lezen, daarna echt
    progressBar.progressProperty().bind(task.progressProperty());

    task.setOnSucceeded(ev -> {
      progressBar.progressProperty().unbind();
      progressBar.setProgress(1.0);
      statusLabel.textProperty().unbind();
      statusLabel.setText("✅ Klaar.");
      String result = task.getValue();
      LOGGER.log(Level.INFO, "Klaar. Grootte: "
          + (result != null ? result.length() : 0) + " tekens");
      finishTask();
    });

    task.setOnFailed(ev -> {
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      statusLabel.textProperty().unbind();
      statusLabel.setText("❌ Fout.");
      Throwable ex = task.getException();
      String msg = (ex != null) ? ex.getMessage() : "onbekend";
      LOGGER.log(Level.SEVERE, "Fout bij genereren: " + msg, ex);
      showAlert("Fout bij genereren", msg);
      finishTask();
    });

    task.setOnCancelled(ev -> {
      progressBar.progressProperty().unbind();
      progressBar.setProgress(0);
      statusLabel.textProperty().unbind();
      statusLabel.setText("⏹️ Geannuleerd.");
      LOGGER.log(Level.INFO, "Generatie geannuleerd");
      finishTask();
    });

    outputButton.setDisable(true);
    cancelButton.setDisable(false);

    Thread t = new Thread(task, "velocity-generate");
    t.setDaemon(true);
    t.start();
  }

  private void finishTask() {
    outputButton.setDisable(!(m_templateFile != null && m_excelFile != null));
    cancelButton.setDisable(true);
    currentTask = null;
  }

  /**
   * Bouwt de Task die het zware werk doet.
   *
   * <p>
   * Belangrijk onderscheid:
   * </p>
   * <ul>
   * <li>{@code updateMessage(...)} → korte statusregel (statusLabel).</li>
   * <li>{@code LOGGER.log(...)} → logboek (logArea via JavaFXLogHandler).</li>
   * </ul>
   */
  private Task<String> createGenerateTask(File outputFile) {
    return new Task<>() {
      @Override
      protected String call() throws Exception {
        GenerationListener listener = new GenerationListener() {
          @Override
          public void status(String message) {
            updateMessage(message);
          }

          @Override
          public void progress(long done, long total) {
            if (total <= 0) {
              updateProgress(ProgressBar.INDETERMINATE_PROGRESS, 1);
            } else {
              updateProgress(done, total);
            }
          }

          @Override
          public boolean isCancelled() {
            return GuiLayout.this.currentTask != null
                && GuiLayout.this.currentTask.isCancelled();
          }

          @Override
          public void log(String message) {
            LOGGER.log(Level.INFO, message);
          }
        };

        return GenerationService.generate(m_templateFile, m_excelFile,
            outputFile, listener);
      }
    };
  }

  private void showAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  /**
   * Log naar het logboek. Gaat via de Logger, dus de JavaFXLogHandler schrijft
   * thread-safe naar de logArea. Ook op de FX-thread veilig.
   */
  private void log(String msg) {
    LOGGER.log(Level.INFO, msg);
  }

  // Menubar
  private int il = 0;

  /**
   * Opbouwen menu balk
   * 
   * @param primaryStage
   * @return Menubalk
   */
  private MenuBar doTheMenuBar(Stage primaryStage) {
    MenuBar menuBar = new MenuBar();

    // Settings Menu
    Menu mnSettings = new Menu(bundle.getMessage(MessageConstants.C_Settings));
    mnSettings.setDisable(false);

    // Loglevel:
    Menu mntmLoglevel = new Menu(
        bundle.getMessage(MessageConstants.C_Loglevel));
    ToggleGroup toggleGroup = new ToggleGroup();
    RadioMenuItem[] loglevelitems = new RadioMenuItem[c_levels.length];

    ArrayList<String> ca_Levels = new ArrayList<String>();
    for (il = 0; il < c_levels.length; il++) {
      ca_Levels.add(c_levels[il]);
    }

    il = 0;
    ca_Levels.forEach(level -> {
      loglevelitems[il] = new RadioMenuItem(c_levels[il]);
      toggleGroup.getToggles().add(loglevelitems[il]);
      mntmLoglevel.getItems().add(loglevelitems[il]);
      loglevelitems[il].setOnAction(_ -> {
        m_Level = Level.parse(level.toUpperCase());
        m_param.set_Level(m_Level);
        MyLogger.changeLogLevel(m_Level);
        LOGGER.log(Level.INFO, level + " Selected");
      });
      if (c_levels[il].toLowerCase().equals(m_Level.toString().toLowerCase())) {
        toggleGroup.selectToggle(loglevelitems[il]);
      }
      il++;
    });
    mnSettings.getItems().add(mntmLoglevel);

    // Language
    ToggleGroup toggleLanguagesGroup = new ToggleGroup();
    Set<String> langs = bundle.getTranslations();
    Menu mntmLanguages = new Menu("Languages");
    il = 0;
    RadioMenuItem[] mnLanguages = new RadioMenuItem[langs.size()];
    langs.forEach(language -> {
      mnLanguages[il] = new RadioMenuItem(language);
      toggleLanguagesGroup.getToggles().add(mnLanguages[il]);
      mntmLanguages.getItems().add(mnLanguages[il]);
      mnLanguages[il].setOnAction(_ -> {
        LOGGER.log(Level.INFO, language + " Selected");
        m_Language = language;
        m_param.set_Language(m_Language);
        m_param.save();
        bundle.changeLanguage(language);
        restartGUI(primaryStage);
      });
      if (language.toLowerCase().equals(m_Language.toLowerCase())) {
        toggleLanguagesGroup.selectToggle(mnLanguages[il]);
      }
      il++;
    });
    mnSettings.getItems().add(mntmLanguages);

    // Logfiles
    CheckMenuItem checkMenuItem = new CheckMenuItem("Logfiles");
    checkMenuItem.setOnAction(_ -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setTitle("Output Directory");
      File selectedDirectory = directoryChooser.showDialog(primaryStage);

      if (selectedDirectory != null) {
        LOGGER.log(Level.INFO, bundle.getMessage(MessageConstants.C_LogFolder,
            selectedDirectory.getAbsolutePath()));
        m_Logdir = selectedDirectory.getAbsolutePath() + "/";
        m_param.set_LogDir(m_Logdir);
        m_param.set_toDisk(true);
        m_toDisk = checkMenuItem.isSelected();
        try {
          MyLogger.setup(m_Level, m_Logdir, m_toDisk);
        } catch (IOException es) {
          LOGGER.log(Level.SEVERE,
              Class.class.getName() + ": " + es.toString());
        }
      } else {
        LOGGER.log(Level.INFO,
            bundle.getMessage(MessageConstants.C_NoDirectorySelected));
      }
    });
    mnSettings.getItems().add(checkMenuItem);

    // Stored preferences
    MenuItem menuPreferences = new MenuItem("Preferences");
    menuPreferences.setOnAction(_ -> {
      ShowPreferences showPref = new ShowPreferences(UserSetting.NodePrefName);
      showPref.showAllPreferences(true);
    });
    mnSettings.getItems().add(menuPreferences);

    // Menu "?" and Help
    Menu questMenu = new Menu("?");
    MenuItem menuHelp = new MenuItem("Help");
    menuHelp.setOnAction(_ -> {
      File helpFile = new File("help\\" + m_Language + "\\" + m_HelpFile);
      if (helpFile.exists()) {
        try {
          Desktop.getDesktop().open(helpFile);
        } catch (IOException e1) {
          LOGGER.log(Level.INFO, e1.getMessage());
          e1.printStackTrace();
        }
      } else {
        LOGGER.log(Level.INFO, bundle.getMessage(
            MessageConstants.C_HelpFileNotFound, helpFile.getAbsolutePath()));
      }
    });
    questMenu.getItems().add(menuHelp);

    // About
    MenuItem menuAbout = new MenuItem(
        bundle.getMessage(MessageConstants.C_About));
    menuAbout.setOnAction(_ -> {
      @SuppressWarnings("unused")
      AboutWindow about = new AboutWindow(c_reponame, m_creationtime,
          c_CopyrightYear);
    });
    questMenu.getItems().add(menuAbout);

    // MenuBar build up
    menuBar.getMenus().addAll(mnSettings, questMenu);
    return menuBar;
  }

  /**
   * Restart GUI for Language change purpose...
   *
   * @param primaryStage Stage
   */
  private void restartGUI(Stage primaryStage) {
    primaryStage.close();
    start(new Stage());
  }
}