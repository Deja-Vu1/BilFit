package managers;

import database.Database;
import database.DbStatus;

public class AuthManager {

    private Database db;

    public AuthManager(Database db) {
        this.db = db;
    }

    public DbStatus registerStudent(String email, String password, String studentId, String fullName) {
        return db.registerUser(email, password, null, "STUDENT");
    }

    public DbStatus registerAdmin(String email, String password, String activationCode, String fullName) {
        DbStatus verificationStatus = db.verifyActivationCode(email, activationCode);
        
        if (verificationStatus != DbStatus.SUCCESS) {
            return verificationStatus;
        }

        return db.registerUser(email, password, activationCode, "ADMIN");
    }

    public DbStatus loginStudent(String email, String password) {
        return db.login(email, password);
    }

    public DbStatus loginAdmin(String email, String password) {
        return db.login(email, password);
    }

    public DbStatus activateAccount(String email, String activationCode) {
        return db.verifyActivationCode(email, activationCode);
    }
}