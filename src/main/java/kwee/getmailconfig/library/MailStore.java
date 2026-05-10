package kwee.getmailconfig.library;

import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;

import jakarta.mail.Session;
import jakarta.mail.Store;
//import kwee.getmailconfig.library.Account;

public class MailStore {
  static public Store getMailStore(Account account) throws Exception {
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
    return store;
  }
}
