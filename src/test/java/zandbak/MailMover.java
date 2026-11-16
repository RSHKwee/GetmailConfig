package zandbak;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class MailMover {

  // private static final String HOST = "imap.jouprovider.nl";
  // private static final String USERNAME = "jouw@email.nl";
  // private static final String PASSWORD = "jouwwachtwoord";
  // private static String host = "imap.mail.yahoo.com";
  // private static String username = "rshkwee";
  // private static String password = "uzxheisteseoemvy";
  // private static String host = "imap.gmail.com";
  // private static String username = "cym.kwee@gmail.com";
  // private static String password = "ycgapizqflbgaibe";

  private static String host = "imap.hccnet.nl";
  private static String username = "rsh.kwee@hccnet.nl";
  private static String password = "2308Cori";

  private static final String FOLDER_WITH_DOT = "INBOX.INBOX";
  private static final String FOLDER_WITH_SLASH = "INBOX/INBOX";

  public static void main(String[] args) {
    Properties props = new Properties();
    props.setProperty("mail.store.protocol", "imaps");
    props.setProperty("mail.imaps.host", host);
    props.setProperty("mail.imaps.port", "993");
    props.setProperty("mail.imaps.ssl.enable", "true");
    props.setProperty("mail.imap.starttls.enable", "true");

    try {
      Session session = Session.getInstance(props);
      Store store = session.getStore("imaps");
      store.connect(host, username, password);

      Folder defaultFolder = store.getDefaultFolder();
      System.out.println("\nStandaardmap: " + defaultFolder.getName());

      // Alle mappen ophalen
      Folder[] folders = defaultFolder.list("*");

      // Probeer beide folder-namen
      String[] possibleFolders = { FOLDER_WITH_DOT, FOLDER_WITH_SLASH };
      boolean moved = false;

      for (String folderName : possibleFolders) {
        System.out.println("foldernName: " + folderName);
        if (moveMessagesToInbox(store, folderName)) {
          moved = true;
        }
      }

      if (!moved) {
        System.out.println("Geen van de verdachte mappen gevonden.");
      } else {
        System.out.println("Stap 1 voltooid: berichten verplaatst.");
      }

      store.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static boolean moveMessagesToInbox(Store store, String sourceFolderName) {
    try {
      Folder sourceFolder = store.getFolder(sourceFolderName);
      if (sourceFolder == null || !sourceFolder.exists()) {
        System.out.println("Folder " + sourceFolderName + " bestaat niet.");
        return false;
      }

      Folder inbox = store.getFolder("INBOX");
      if (inbox == null || !inbox.exists()) {
        System.out.println("INBOX bestaat niet?");
        return false;
      }

      sourceFolder.open(Folder.READ_WRITE);
      inbox.open(Folder.READ_WRITE);

      Message[] messages = sourceFolder.getMessages();
      if (messages.length == 0) {
        System.out.println("Geen berichten in " + sourceFolderName);
        sourceFolder.close(false);
        return false;
      }

      System.out.println(messages.length + " berichten gevonden in " + sourceFolderName);
      inbox.appendMessages(messages);
      System.out.println("Berichten gekopieerd naar INBOX");

      // Verwijder de originele berichten in de bronfolder
      sourceFolder.setFlags(messages, new Flags(Flags.Flag.DELETED), true);
      sourceFolder.expunge();
      System.out.println("Originele berichten verwijderd uit " + sourceFolderName);

      sourceFolder.close(true);
      inbox.close(false);
      return true;

    } catch (MessagingException e) {
      System.err.println("Fout bij verwerken van folder " + sourceFolderName + ": " + e.getMessage());
      return false;
    }
  }
}
