package models;

public class Admin extends User {
    private String activationCode;

    public Admin(String fullName, String bilkentEmail, String nickname, String password, String activationCode) {
        super(fullName, bilkentEmail, nickname, password);
        this.activationCode = activationCode;
    }

    public boolean verifyActivationCode(String code) {
        return this.activationCode.equals(code);
    }
    
    public void createTournament(String name, SportType sport, String startDate, int maxPlayers) {
        System.out.println("Tournament created: " + name);
    }

    public void givePenaltyPoint(Student targetStudent, int points) {
        targetStudent.addPenaltyPoint(points);
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public void banUser(Student targetStudent) {
        System.out.println("User banned: " + targetStudent.getNickname());
    }

    public void createNotification(String message) {
        System.out.println("SYSTEM DASHBOARD: " + message);
    }
}