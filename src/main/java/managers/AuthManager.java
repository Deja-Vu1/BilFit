package managers;

import database.Database;
import database.DbStatus;

public class AuthManager {

    private Database db;

    public AuthManager() {
        this.db = Database.getInstance();
    }

    public DbStatus registerStudent(String email, String password, String studentId, String fullName) {
        return db.registerStudent(fullName, email, studentId, password);
    }

    public DbStatus registerAdmin(String email, String password, String activationCode, String fullName) {
        DbStatus verificationStatus = db.verifyActivationCode(email, activationCode);
        
        if (verificationStatus != DbStatus.SUCCESS) {
            return verificationStatus;
        }

        return db.registerAdmin(fullName, email, password);
    }

    public DbStatus loginStudent(String email, String password) {
        return db.loginStudent(email, password);
    }

    public DbStatus loginAdmin(String email, String password) {
        return db.loginAdmin(email, password);
    }

    public DbStatus activateAccount(String email, String activationCode) {
        return db.verifyActivationCode(email, activationCode);
    }
}