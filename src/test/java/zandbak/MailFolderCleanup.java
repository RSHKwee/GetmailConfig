package zandbak;

import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;

public class MailFolderCleanup {
  private static final String HOST = "imap.jouprovider.nl";
  private static final String USERNAME = "jouw@email.nl";
  private static final String PASSWORD = "jouwwachtwoord";

  public static void main(String[] args) {
    Properties props = new Properties();
    props.setProperty("mail.store.protocol", "imaps");
    props.setProperty("mail.imaps.host", HOST);
    props.setProperty("mail.imaps.port", "993");
    props.setProperty("mail.imaps.ssl.enable", "true");

    try {
      Session session = Session.getInstance(props, null);
      Store store = session.getStore("imaps");
      store.connect(HOST, USERNAME, PASSWORD);

      String[] wrongFolders = { "INBOX.INBOX", "INBOX/INBOX", "Tijdelijk" };

      for (String folderName : wrongFolders) {
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
