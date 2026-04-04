package models;

public class Admin extends User {
    private int actionsPerformed;

    public Admin(String fullName, String bilkentEmail, int actionsPerformed) {
        super(fullName, bilkentEmail);
        this.actionsPerformed = actionsPerformed;
    }
    public int getActionsPerformed() {
        return actionsPerformed;
    }
    public void setActionsPerformed(int actionPerformed) {
        this.actionsPerformed = actionPerformed;
    }

    public String toString() {
        return "Admin{" +
                "fullName='" + getFullName() + '\'' +
                ", bilkentEmail='" + getBilkentEmail() + '\'' +
                ", actionPerformed=" + actionsPerformed +
                '}';
    }
}