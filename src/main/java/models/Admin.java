package models;

public class Admin extends User {
    private String activationCode;

    public Admin(String fullName, String bilkentEmail, String nickname, String password, String activationCode) {
        super(fullName, bilkentEmail, nickname, password);
        this.activationCode = activationCode;
    }

    public String getActivationCode() { return activationCode; }
    public void setActivationCode(String activationCode) { this.activationCode = activationCode; }
}