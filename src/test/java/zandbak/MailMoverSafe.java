package zandbak;

import jakarta.mail.*;
import java.util.Properties;

public class MailMoverSafe {

    private static final String HOST = "imap.jouprovider.nl";
    private static final String USERNAME = "jouw@email.nl";
    private static final String PASSWORD = "jouwwachtwoord";
    
    // De foute folders
    private static final String[] WRONG_FOLDERS = {"INBOX.INBOX", "INBOX/INBOX"};
    
    // Tijdelijke folder als buffer
    private static final String TEMP_FOLDER = "Tijdelijk";

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

            // Stap 1: Maak tijdelijke folder aan als die nog niet bestaat
            Folder tempFolder = ensureTempFolderExists(store);
            
            // Stap 2: Verplaats alle berichten van foute folders naar Tijdelijk
            int totalMoved = 0;
            for (String wrongFolder : WRONG_FOLDERS) {
                totalMoved += moveMessagesToTemp(store, wrongFolder, tempFolder);
            }
            
            System.out.println("\n=== Overzicht ===");
            System.out.println("Totaal verplaatste berichten: " + totalMoved);
            
            if (totalMoved > 0) {
                System.out.println("\nBerichten staan nu in folder: " + TEMP_FOLDER);
                System.out.println("Controleer of alles correct is overgekomen.");
                System.out.println("Daarna kun je:");
                System.out.println("  1. Berichten handmatig naar INBOX verplaatsen");
                System.out.println("  2. De foute folders verwijderen");
                System.out.println("  3. De Tijdelijk folder legen of verwijderen");
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
            
            return messages.length;
            
        } catch (MessagingException e) {
            System.err.println("Fout bij verwerken van " + sourceName + ": " + e.getMessage());
            return 0;
        }
    }
}
