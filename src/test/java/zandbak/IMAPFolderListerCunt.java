package zandbak;

import jakarta.mail.*;
import java.util.Properties;

public class IMAPFolderListerCunt {
  public static void main(String[] args) throws Exception {
    String host = "192.168.2.210";
    String username = "wilma@kwee.cumail.nl";
    String password = "Baracuda2308";

    Properties props = new Properties();
    props.setProperty("mail.store.protocol", "imap");
    props.setProperty("mail.imap.host", host);
    props.setProperty("mail.imap.port", "143");
    props.setProperty("mail.imap.starttls.enable", "false");
    props.setProperty("mail.imap.ssl.enable", "false");
    props.put("mail.imap.starttls.enable", "false");

    Session session = Session.getInstance(props);
    // session.setDebug(true); // Kun je uitzetten nu het werkt

    Store store = session.getStore();
    store.connect(host, username, password);

    System.out.println("Verbonden! Mappen worden opgehaald...\n");

    // Start met de root folder, maar open hem niet
    Folder rootFolder = store.getDefaultFolder();

    // Doorloop ALLEEN de submappen van root (niet root zelf)
    for (Folder folder : rootFolder.list()) {
      listAllFoldersWithCounts(folder, "");
    }

    store.close();
  }

  static void listAllFoldersWithCounts(Folder folder, String indent) throws MessagingException {
    // Open de folder om berichtentellingen te kunnen lezen
    boolean wasOpen = folder.isOpen();
    if (!wasOpen) {
      folder.open(Folder.READ_ONLY);
    }

    int totalMessages = folder.getMessageCount();
    int unreadMessages = folder.getUnreadMessageCount();

    // Toon mapnaam met aantallen
    System.out.printf("%s%s - %d berichten (%d ongelezen)%n", indent, folder.getFullName(), totalMessages,
        unreadMessages);

    // Recursief door submappen
    for (Folder sub : folder.list()) {
      listAllFoldersWithCounts(sub, indent + "  ");
    }

    if (!wasOpen) {
      folder.close(false);
    }
  }
}