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
        return this.bilkentEmail.equals(email) && this.password.equals(pass);
    }

    public void logout() {
        System.out.println(this.nickname + " logged out from the system.");
    }

    public void changePassword(String newPass) {
        this.password = newPass;
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public String getFullName() { 
        return fullName; 
    }
    public String getBilkentEmail() {
         return bilkentEmail; 
        }
    public String getNickname() {
         return nickname; 
        }
    public String getPassword() {
         return password; 
        }
}