package models;

public abstract class User {
    protected String fullName;
    protected String bilkentEmail;
    protected String nickname;
    protected String password;

    public User(String fullName, String bilkentEmail, String nickname, String password) {
        this.fullName = fullName;
        this.bilkentEmail = bilkentEmail;
        this.nickname = nickname;
        this.password = password;
    }


    public boolean login(String email, String pass) {
        // Will be authenticated via DB 
        return this.bilkentEmail.equals(email) && this.password.equals(pass);
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }


    public void setBilkentEmail(String bilkentEmail) {
        this.bilkentEmail = bilkentEmail;
    }


    public void setNickname(String nickname) {
        this.nickname = nickname;
    }


    public void setPassword(String password) {
        this.password = password;
    }


    public void logout() {
        System.out.println(this.nickname + " has logged out successfully.");
    }

    public void changePassword(String newPass) {
        this.password = newPass;
        System.out.println("Password changed successfully.");
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
        System.out.println("New nickname set to: " + this.nickname);
    }


    public String getFullName() { return fullName; }
    public String getBilkentEmail() { return bilkentEmail; }
    public String getNickname() { return nickname; }
    public String getPassword() { return password; }
}