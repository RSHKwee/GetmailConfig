package kwee.getmailconfig.library;

import jakarta.mail.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import java.io.*;
import java.util.*;

public class AccountMigrator {

  private String outputDirectory;
  private List<Account> accounts;

  public AccountMigrator(String excelPad) throws Exception {
    Accounts acc = new Accounts(excelPad);
    accounts = acc.readAccountsFromExcel();
  }

  // Bestaande methode voor template processing
  public void generateAccountFile(String templatePath, String a_templateName, String a_outputDirectory,
      String a_OuputFile, boolean bSummaryOnly) throws Exception {
    // Lees accounts uit Excel (jouw bestaande code)
    Properties props = new Properties();
    outputDirectory = a_outputDirectory;

    VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.init();

    props.setProperty("resource.loader", "file");
    props.setProperty("file.resource.loader.path", templatePath);
    velocityEngine = new VelocityEngine(props);
    velocityEngine.init();

//    String outputPath = "";
    String outputPath = outputDirectory + File.separator + a_OuputFile;

    try (Writer writer = new FileWriter(outputPath)) {
      // Template template = velocityEngine.getTemplate("imap-folder-tree.vm");
      Template template = velocityEngine.getTemplate(a_templateName);

      for (Account account : accounts) {
        try {
          VelocityContext context = new VelocityContext();
          context = generateIMAPFolderFile(account, a_templateName, velocityEngine, bSummaryOnly);
          template.merge(context, writer);
        } catch (Exception e) {
          System.out.println("Exc" + e.getMessage());
          e.printStackTrace();
        }
      }

    }
  }

  // NIEUWE methode: Haal IMAP folders op en genereer .secret bestand
  private VelocityContext generateIMAPFolderFile(Account account, String a_templateName, VelocityEngine velocityEngine,
      boolean bSummaryOnly) throws Exception {
    System.out.println("Verwerken: " + account.getEmail());

    Store store = null;
    VelocityContext context = new VelocityContext();
    try {
      store = MailStore.getMailStore(account);
      store.connect(account.getImapHost(), account.getEmail(), account.getPassword());

      // Verzamel folder informatie
      List<FolderInfo> folders = new ArrayList<>();
      Folder rootFolder = store.getDefaultFolder();

      for (Folder folder : rootFolder.list()) {
        collectFolderInfo(folder, folders);
      }

      // Velocity context voor template
      context.put("account", account);
      context.put("folders", folders);
      context.put("timestamp", new Date());
      if (!bSummaryOnly) {
        // Genereer .tree bestand
        Template template = velocityEngine.getTemplate(a_templateName);
        String outputPath = outputDirectory + File.separator + account.getImapHost() + " " + account.getEmail()
            + ".tree";

        try (Writer writer = new FileWriter(outputPath)) {
          template.merge(context, writer);
        }

        System.out.println("  -> " + outputPath + " gegenereerd");
      }
    } finally {
      store.close();
    }
    return context;
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
