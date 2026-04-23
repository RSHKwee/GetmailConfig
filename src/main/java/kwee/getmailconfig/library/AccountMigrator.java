package kwee.getmailconfig.library;

import javax.mail.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.io.*;
import java.util.*;

public class AccountMigrator {
  List<Map<String, String>> excelData;
  String outputDirectory;

  public AccountMigrator(String excelPad) throws Exception {
    ExcelLezer excel = new ExcelLezer(excelPad);
    excelData = excel.getExcelData();
  }

  // Bestaande methode voor template processing
  public void generateAccountFile(String templatePath, String a_outputDirectory) throws Exception {
    // Lees accounts uit Excel (jouw bestaande code)
    List<Account> accounts = readAccountsFromExcel();
    Properties props = new Properties();
    outputDirectory = a_outputDirectory;

    VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.init();

    props.setProperty("resource.loader", "file");
    props.setProperty("file.resource.loader.path", templatePath);
    velocityEngine = new VelocityEngine(props);
    velocityEngine.init();

    for (Account account : accounts) {
      try {
        generateIMAPFolderFile(account, velocityEngine);
      } catch (Exception e) {
        System.out.println("Exc" + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private List<Account> readAccountsFromExcel() {
    List<Account> Accounts = new ArrayList<Account>();
    int rijNummer = 1;
    for (Map<String, String> rijData : excelData) {
      String email = "";
      String password = "";
      String imapHost = "";
      boolean ssl = true;
      for (Map.Entry<String, String> entry : rijData.entrySet()) {
        if (entry.getKey().toUpperCase().contains("RETRIEVERSERVER")) {
          imapHost = entry.getValue();
        }
        if (entry.getKey().toUpperCase().contains("RETRIEVERPASSWORD")) {
          password = entry.getValue();
        }
        if (entry.getKey().toUpperCase().contains("RETRIEVERUSER")) {
          email = entry.getValue();
        }
        if (entry.getKey().toUpperCase().contains("RETRIEVERTYPE")) {
          if (entry.getValue().contains("SSL")) {
            ssl = true;
          } else {
            ssl = false;
          }
        }
      }
      Account account = new Account();
      account.setEmail(email);
      account.setImapHost(imapHost);
      account.setPassword(password);
      account.setSsl(ssl);
      Accounts.add(account);
      rijNummer++;
    }
    return Accounts;
  }

  // NIEUWE methode: Haal IMAP folders op en genereer .secret bestand
  private void generateIMAPFolderFile(Account account, VelocityEngine velocityEngine) throws Exception {
    System.out.println("Verwerken: " + account.getEmail());

    // Verbind met IMAP server
    Properties props = new Properties();
    props.setProperty("mail.imap.host", account.getImapHost());

    if (account.isSsl()) {
      props.setProperty("mail.store.protocol", "imaps");
      props.setProperty("mail.imap.port", "993");
      props.setProperty("mail.imap.starttls.enable", "true");
    } else {
      props.setProperty("mail.store.protocol", "imap");
      props.setProperty("mail.imap.port", "143");
      props.setProperty("mail.imap.starttls.enable", "false");
      props.setProperty("mail.imap.ssl.enable", "false");
    }

    Session session = Session.getInstance(props);
    Store store = session.getStore();

    try {
      store.connect(account.getImapHost(), account.getEmail(), account.getPassword());

      // Verzamel folder informatie
      List<FolderInfo> folders = new ArrayList<>();
      Folder rootFolder = store.getDefaultFolder();

      for (Folder folder : rootFolder.list()) {
        collectFolderInfo(folder, folders);
      }

      // Velocity context voor template
      VelocityContext context = new VelocityContext();
      context.put("account", account);
      context.put("folders", folders);
      context.put("timestamp", new Date());

      // Genereer .secret bestand
      Template template = velocityEngine.getTemplate("imap-folder-secret.vm");
      String outputPath = outputDirectory + File.separator + account.getImapHost() + " " + account.getEmail()
          + ".secret";

      try (Writer writer = new FileWriter(outputPath)) {
        template.merge(context, writer);
      }

      System.out.println("  -> " + outputPath + " gegenereerd");

    } finally {
      store.close();
    }
  }

  private void collectFolderInfo(Folder folder, List<FolderInfo> folders) throws MessagingException {
    boolean wasOpen = false;
    try {
      // Alleen openen als het berichten kan bevatten
      if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
        wasOpen = folder.isOpen();
        if (!wasOpen) {
          folder.open(Folder.READ_ONLY);
        }

        FolderInfo info = new FolderInfo();
        info.setFullName(folder.getFullName());
        info.setTotalMessages(folder.getMessageCount());
        info.setUnreadMessages(folder.getUnreadMessageCount());
        info.setHasSubfolders(folder.list().length > 0);
        folders.add(info);
      }

      // Recursief door submappen
      for (Folder sub : folder.list()) {
        collectFolderInfo(sub, folders);
      }

    } finally {
      if (!wasOpen && folder.isOpen()) {
        folder.close(false);
      }
    }
  }

  // Helper classes
  public static class Account {
    private String email;
    private String password;
    private String imapHost;
    private boolean ssl = true;

    // getters/setters
    public String getEmail() {
      return email;
    }

    public String getPassword() {
      return password;
    }

    public String getImapHost() {
      return imapHost;
    }

    public boolean isSsl() {
      return ssl;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public void setImapHost(String imapHost) {
      this.imapHost = imapHost;
    }

    public void setSsl(boolean ssl) {
      this.ssl = ssl;
    }
  }

  public static class FolderInfo {
    private String fullName;
    private int totalMessages;
    private int unreadMessages;
    private boolean hasSubfolders;

    // getters/setters
    public String getFullName() {
      return fullName;
    }

    public int getTotalMessages() {
      return totalMessages;
    }

    public int getUnreadMessages() {
      return unreadMessages;
    }

    public boolean isHasSubfolders() {
      return hasSubfolders;
    }

    public void setFullName(String fullName) {
      this.fullName = fullName;
    }

    public void setTotalMessages(int totalMessages) {
      this.totalMessages = totalMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
      this.unreadMessages = unreadMessages;
    }

    public void setHasSubfolders(boolean hasSubfolders) {
      this.hasSubfolders = hasSubfolders;
    }
  }
}
