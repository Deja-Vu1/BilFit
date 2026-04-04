package models;

public class Admin extends User {
    private int actionPerformed;

    public Admin(String fullName, String bilkentEmail, int actionsPerformed) {
        super(fullName, bilkentEmail);
        this.actionPerformed = actionsPerformed;
    }
    public int getActionPerformed() {
        return actionPerformed;
    }
    public void setActionPerformed(int actionPerformed) {
        this.actionPerformed = actionPerformed;
    }

    public String toString() {
        return "Admin{" +
                "fullName='" + getFullName() + '\'' +
                ", bilkentEmail='" + getBilkentEmail() + '\'' +
                ", actionPerformed=" + actionPerformed +
                '}';
    }
}