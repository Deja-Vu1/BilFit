package models;

public class Admin extends User {
    private String activationCode;

    public Admin(String fullName, String bilkentEmail, String activationCode) {
        super(fullName, bilkentEmail);
        this.activationCode = activationCode;
    }

    public String getActivationCode() { return activationCode; }
    public void setActivationCode(String activationCode) { this.activationCode = activationCode; }
}