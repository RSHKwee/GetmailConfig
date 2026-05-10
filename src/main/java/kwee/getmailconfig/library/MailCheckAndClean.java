package kwee.getmailconfig.library;

import jakarta.mail.*;
import java.util.List;

public class MailCheckAndClean {
  private List<Account> accounts;
  String outputDirectory;

  // De foute folders
  private static final String[] WRONG_FOLDERS = { "INBOX.INBOX", "INBOX/INBOX", "Tijdelijk" };

  public MailCheckAndClean(String excelPad) throws Exception {
    Accounts acc = new Accounts(excelPad);
    accounts = acc.readAccountsFromExcel();
  }

  public void checkPorviders() {
    for (Account account : accounts) {
      try {
        System.out.println("\nCheck: " + account.getEmail());
        checkAndCleanFolders(account);
      } catch (Exception e) {
        System.out.println("Exc" + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public void checkAndCleanFolders(Account account) {
    Store store = null;
    try {
      store = MailStore.getMailStore(account);
      store.connect(account.getImapHost(), account.getEmail(), account.getPassword());

      for (String folderName : WRONG_FOLDERS) {
        Folder folder = store.getFolder(folderName);
        if (folder.exists()) {
          folder.open(Folder.READ_WRITE);
          int count = folder.getMessageCount();
          folder.close(false);

          if (count == 0) {
            System.out.println("Verwijder lege folder: " + folderName);
            folder.delete(true);
          } else {
            System.out.println("WAARSCHUWING: " + folderName + " bevat nog " + count + " berichten!");
          }
        }
      }
      store.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
