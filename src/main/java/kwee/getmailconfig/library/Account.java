package kwee.getmailconfig.library;

public class Account {
  // Helper classes
  private String email;
  private String password;
  private String imapHost;
  private boolean ssl = true;
  private String destination = "";
  private String provider = "";

  // getters/setters
  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getImapHost() {
    return imapHost;
  }

  public String getDestination() {
    return destination;
  }

  public String getProvider() {
    return provider;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public void setImapHost(String imapHost) {
    this.imapHost = imapHost;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

}
