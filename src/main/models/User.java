package models;

public abstract class User {
    protected String fullName;
    protected String bilkentEmail;
    protected String nickname;
    protected String password;

<<<<<<< Updated upstream

=======
>>>>>>> Stashed changes
    public User(String fullName, String bilkentEmail, String nickname, String password) {
        this.fullName = fullName;
        this.bilkentEmail = bilkentEmail;
        this.nickname = nickname;
        this.password = password;
    }

<<<<<<< Updated upstream

    public boolean login(String email, String pass) {
        // Will be authenticated via DB 
=======
    public boolean login(String email, String pass) {
>>>>>>> Stashed changes
        return this.bilkentEmail.equals(email) && this.password.equals(pass);
    }

    public void logout() {
<<<<<<< Updated upstream
        System.out.println(this.nickname + " has logged out successfully.");
=======
        System.out.println(this.nickname + " logged out from the system.");
>>>>>>> Stashed changes
    }

    public void changePassword(String newPass) {
        this.password = newPass;
<<<<<<< Updated upstream
        System.out.println("Password changed successfully.");
=======
>>>>>>> Stashed changes
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
<<<<<<< Updated upstream
        System.out.println("New nickname set to: " + this.nickname);
    }


    public String getFullName() { return fullName; }
    public String getBilkentEmail() { return bilkentEmail; }
    public String getNickname() { return nickname; }
    public String getPassword() { return password; }
=======
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
>>>>>>> Stashed changes
}