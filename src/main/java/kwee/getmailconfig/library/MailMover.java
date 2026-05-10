package kwee.getmailconfig.library;

import jakarta.mail.*;
//import jakarta.mail.internet.*;

import java.util.List;

public class MailMover {
  private List<Account> accounts;
  // private String outputDirectory;
  private static boolean move = true;

  // private static final String FOLDER_WITH_DOT = "INBOX.INBOX";
  // private static final String FOLDER_WITH_SLASH = "INBOX/INBOX";
  // Tijdelijke folder als buffer
  private static final String TEMP_FOLDER = "Tijdelijk";
  // De foute folders
  private static final String[] WRONG_FOLDERS = { "INBOX.INBOX", "INBOX/INBOX" };

  public MailMover(String excelPad) throws Exception {
    Accounts acc = new Accounts(excelPad);
    accounts = acc.readAccountsFromExcel();
  }

  public void checkPorviders() {
    for (Account account : accounts) {
      try {
        System.out.println("\nCheck: " + account.getEmail());
        moveMessages(account);
      } catch (Exception e) {
        System.out.println("Exc" + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public void moveMessages(Account account) {
    Store store = null;
    try {
      store = MailStore.getMailStore(account);
      store.connect(account.getImapHost(), account.getEmail(), account.getPassword());

      Folder defaultFolder = store.getDefaultFolder();
      System.out.println("\nStandaardmap: " + defaultFolder.getName());

      // Stap 1: Maak tijdelijke folder aan als die nog niet bestaat
      Folder tempFolder = ensureTempFolderExists(store);

      // Stap 2: Verplaats alle berichten van foute folders naar Tijdelijk
      int totalMoved = 0;
      for (String wrongFolder : WRONG_FOLDERS) {
        totalMoved += moveMessagesToTemp(store, wrongFolder, tempFolder);
      }

      System.out.println("\n=== Overzicht " + account.getEmail() + " ===");
      System.out.println("Totaal verplaatste berichten: " + totalMoved);

      if (totalMoved > 0) {
        System.out.println("\nBerichten staan nu in folder: " + TEMP_FOLDER);
        System.out.println("Controleer of alles correct is overgekomen.");
        System.out.println("Daarna kun je:");
        System.out.println("  1. Berichten handmatig naar INBOX verplaatsen");
        System.out.println("  2. De foute folders verwijderen");
        System.out.println("  3. De Tijdelijk folder legen of verwijderen");
      } else {
        tempFolder.delete(true);
        System.out.println("Tijdelijke folder verwijderd.");
      }

      store.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Folder ensureTempFolderExists(Store store) throws MessagingException {
    Folder tempFolder = store.getFolder(TEMP_FOLDER);
    if (!tempFolder.exists()) {
      System.out.println("Aanmaken van tijdelijke folder: " + TEMP_FOLDER);
      tempFolder.create(Folder.HOLDS_MESSAGES);
    }
    return tempFolder;
  }

  private static int moveMessagesToTemp(Store store, String sourceName, Folder tempFolder) {
    try {
      Folder sourceFolder = store.getFolder(sourceName);
      if (sourceFolder == null || !sourceFolder.exists()) {
        System.out.println("Folder bestaat niet: " + sourceName);
        return 0;
      }

      sourceFolder.open(Folder.READ_WRITE);
      Message[] messages = sourceFolder.getMessages();

      if (messages.length == 0) {
        System.out.println("Geen berichten in: " + sourceName);
        sourceFolder.close(false);
        return 0;
      }

      System.out.println("\n" + messages.length + " berichten gevonden in: " + sourceName);
      if (move) {
        // Open temp folder voor schrijven
        tempFolder.open(Folder.READ_WRITE);

        // Kopiëren naar tijdelijke folder
        tempFolder.appendMessages(messages);
        System.out.println("  -> Gekopieerd naar: " + TEMP_FOLDER);

        // Originele berichten verwijderen uit bronfolder
        sourceFolder.setFlags(messages, new Flags(Flags.Flag.DELETED), true);
        sourceFolder.expunge();
        System.out.println("  -> Origineel verwijderd uit: " + sourceName);

        sourceFolder.close(true);
        tempFolder.close(false);
      } else {
        System.out.println("Nog geen berichten verplaatst.");
      }
      return messages.length;

    } catch (MessagingException e) {
      System.err.println("Fout bij verwerken van " + sourceName + ": " + e.getMessage());
      return 0;
    }
  }
}
