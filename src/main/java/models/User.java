package models;

public abstract class User {
    protected String fullName;
    protected String bilkentEmail;
    protected String password;
    private String profilePictureUrl; 

    public User(String fullName, String bilkentEmail) {
        this.fullName = fullName;
        this.bilkentEmail = bilkentEmail;
        this.profilePictureUrl = null; 
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getBilkentEmail() { return bilkentEmail; }
    public void setBilkentEmail(String bilkentEmail) { this.bilkentEmail = bilkentEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
}