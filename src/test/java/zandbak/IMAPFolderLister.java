package zandbak;

import jakarta.mail.*;
import java.util.Properties;

public class IMAPFolderLister {
  public static void main(String[] args) throws Exception {
    String host = "192.168.2.210";
    String username = "wilma@kwee.cumail.nl";
    String password = "Baracuda2308";

    Properties props = new Properties();
    props.setProperty("mail.store.protocol", "imap");
    props.setProperty("mail.imap.host", host);
    props.setProperty("mail.imap.port", "143");
    props.setProperty("mail.imap.connectiontimeout", "15000");
    props.setProperty("mail.imap.timeout", "15000");
    props.put("mail.imap.starttls.enable", "false");

    // Dit kan helpen - forceer specifieke IMAP versie
    // props.setProperty("mail.imap.ssl.protocols", "TLSv1.2");

    Session session = Session.getInstance(props);
    // session.setDebug(true);

    Store store = session.getStore("imap");
    store.connect(host, username, password);
    // Debug aanzetten om te zien wat er gebeurt
    // Session session = Session.getInstance(props);
    // session.setDebug(true); // Zie alle IMAP commando's

    // Nu werkt het - protocol is bekend via de property
    // Store store = session.getStore();
    System.out.println("Protocol: " + store.getURLName().getProtocol());

    Folder rootFolder = store.getDefaultFolder();
    listAllFolders(rootFolder, "");

    store.close();
  }

  static void listAllFolders(Folder folder, String indent) throws MessagingException {
    System.out.println(indent + folder.getFullName());
    for (Folder sub : folder.list()) {
      listAllFolders(sub, indent + "  ");
    }
  }
}