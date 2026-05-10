package kwee.getmailconfig.main;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import kwee.logger.MyLogger;

/**
 * User setting persistence.
 * 
 * @author rshkw
 *
 */
public class UserSetting {
  private static final Logger LOGGER = MyLogger.getLogger();
  public static String NodePrefName = "kwee.getmailConfig";

  private String c_Level = "Level";
  private String c_LevelValue = "INFO";

  private String c_ConfirmOnExit = "ConfirmOnExit";
  private String c_toDisk = "ToDisk";
  private String c_OutputFolder = "OutputFolder";
  private String c_LookAndFeel = "LookAndFeel";
  private String c_LookAndFeelVal = "Nimbus";
  private String c_LogDir = "LogDir";
  private String c_InputFolder = "InputFolder";
  private String c_Append = "Append";
  private String c_Language = "Language";

  private String m_Level = c_LevelValue;
  private String m_LookAndFeel;
  private String m_OutputFolder = "";
  private String m_LogDir = "";
  private String m_InputFolder = "";
  private String m_Language = "nl";

  private boolean m_ConfirmOnExit = false;
  private boolean m_toDisk = false;
  private boolean m_Append = false;

  private Preferences userPrefs = Preferences.userRoot();
  private Preferences pref;

  /**
   * Constructor Initialize settings
   */
  public UserSetting() {
    // Navigate to the preference node that stores the user setting
    pref = userPrefs.node(NodePrefName);

    m_toDisk = pref.getBoolean(c_toDisk, false);
    m_ConfirmOnExit = pref.getBoolean(c_ConfirmOnExit, false);
    m_Append = pref.getBoolean(c_Append, false);
    m_Language = pref.get(c_Language, "nl");

    m_LookAndFeel = pref.get(c_LookAndFeel, c_LookAndFeelVal);
    m_OutputFolder = pref.get(c_OutputFolder, "");
    m_InputFolder = pref.get(c_InputFolder, "");
    m_Level = pref.get(c_Level, c_LevelValue);
    m_LogDir = pref.get(c_LogDir, "");
  }

  public void set_Append(boolean m_Append) {
    this.m_Append = m_Append;
  }

  public String get_LogDir() {
    return m_LogDir;
  }

  public void set_LogDir(String m_LogDir) {
    this.m_LogDir = m_LogDir;
  }

  public String get_OutputFolder() {
    return m_OutputFolder;
  }

  public String get_InputFolder() {
    return m_InputFolder;
  }

  public String get_Language() {
    return m_Language;
  }

  public Level get_Level() {
    return Level.parse(m_Level);
  }

  public String get_LookAndFeel() {
    return m_LookAndFeel;
  }

  public boolean is_Append() {
    return m_Append;
  }

  public boolean is_toDisk() {
    return m_toDisk;
  }

  public boolean is_ConfirmOnExit() {
    return m_ConfirmOnExit;
  }

  public void set_OutputFolder(File a_OutputFolder) {
    pref.put(c_OutputFolder, a_OutputFolder.getAbsolutePath());
    m_OutputFolder = a_OutputFolder.getAbsolutePath();
  }

  public void set_Language(String m_Language) {
    this.m_Language = m_Language;
  }

  public void set_InputFolder(File a_InputFolder) {
    pref.put(c_InputFolder, a_InputFolder.getAbsolutePath());
    m_InputFolder = a_InputFolder.getAbsolutePath();
  }

  public void set_toDisk(boolean a_toDisk) {
    pref.putBoolean(c_toDisk, a_toDisk);
    this.m_toDisk = a_toDisk;
  }

  public void set_Level(Level a_Level) {
    pref.put(c_Level, a_Level.toString());
    this.m_Level = a_Level.toString();
  }

  public void set_LookAndFeel(String a_LookAndFeel) {
    pref.put(c_LookAndFeel, a_LookAndFeel);
    this.m_LookAndFeel = a_LookAndFeel;
  }

  public void set_ConfirmOnExit(boolean a_ConfirmOnExit) {
    pref.putBoolean(c_ConfirmOnExit, a_ConfirmOnExit);
    this.m_ConfirmOnExit = a_ConfirmOnExit;
  }

  /**
   * Save all settings
   */
  public void save() {
    try {
      pref.putBoolean(c_toDisk, m_toDisk);

      pref.putBoolean(c_ConfirmOnExit, m_ConfirmOnExit);
      pref.putBoolean(c_Append, m_Append);

      pref.put(c_LookAndFeel, m_LookAndFeel);
      pref.put(c_OutputFolder, m_OutputFolder);
      pref.put(c_InputFolder, m_InputFolder);
      pref.put(c_Level, m_Level);
      pref.put(c_LogDir, m_LogDir);
      pref.put(c_Language, m_Language);

      pref.flush();
    } catch (BackingStoreException e) {
      LOGGER.log(Level.INFO, e.getMessage());
    }
  }

  public String print() {
    String l_line = "User setting \n";
    l_line = l_line + "Name: " + pref.name() + "\n";
    l_line = l_line + c_toDisk + ": " + m_toDisk + "\n";
    l_line = l_line + c_ConfirmOnExit + ": " + m_ConfirmOnExit + "\n";
    l_line = l_line + c_InputFolder + ": " + m_InputFolder + "\n";
    l_line = l_line + c_Append + ": " + m_Append + "\n";
    l_line = l_line + c_Language + ": " + m_Language + "\n";

    l_line = l_line + c_LookAndFeel + ": " + m_LookAndFeel + "\n";
    l_line = l_line + c_OutputFolder + ": " + m_OutputFolder + "\n";
    l_line = l_line + c_Level + ": " + m_Level + "\n";
    l_line = l_line + c_LogDir + ": " + m_LogDir + "\n";

    return l_line;
  }

  /**
   * Copy UserSetings
   * 
   * @return Copy of UserSetting
   */
  public UserSetting copy() {
    UserSetting l_UserSetting = new UserSetting();
    l_UserSetting.set_toDisk(m_toDisk);
    l_UserSetting.set_ConfirmOnExit(m_ConfirmOnExit);
    l_UserSetting.set_InputFolder(new File(m_InputFolder));
    l_UserSetting.set_Append(m_Append);

    l_UserSetting.set_LookAndFeel(m_LookAndFeel);
    l_UserSetting.set_OutputFolder(new File(m_OutputFolder));
    l_UserSetting.set_Level(Level.parse(m_Level));
    l_UserSetting.set_LogDir(m_LogDir);
    l_UserSetting.set_Language(m_Language);

    return l_UserSetting;
  }
}
