package kwee.getmailconfig.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Accounts {
  List<Map<String, String>> excelData;

  public Accounts(String excelPad) {
    ExcelLezer excel;
    try {
      excel = new ExcelLezer(excelPad);
      excelData = excel.getExcelData();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public List<Map<String, String>> getExceldata() {
    return excelData;
  }

  public List<Account> readAccountsFromExcel() {
    List<Account> Accounts = new ArrayList<Account>();
    int rijNummer = 1;
    for (Map<String, String> rijData : excelData) {
      String email = "";
      String password = "";
      String imapHost = "";
      String destination = "";
      String provider = "";
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
        if (entry.getKey().toUpperCase().contains("DESTINATIONADRESS")) {
          destination = entry.getValue();
        }
        if (entry.getKey().toUpperCase().contains("PREFIX")) {
          provider = entry.getValue();
        }
      }
      Account account = new Account();
      account.setEmail(email);
      account.setImapHost(imapHost);
      account.setPassword(password);
      account.setSsl(ssl);
      account.setDestination(destination);
      account.setProvider(provider);
      Accounts.add(account);
      rijNummer++;
    }
    return Accounts;
  }

}
