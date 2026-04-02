package managers;

import database.Database;
import database.DbStatus;

public class AuthManager {

    private Database db;

    public AuthManager(Database db) {
        this.db = db;
    }

    public DbStatus registerStudent(String email, String password, String studentId, String fullName) {
        return db.registerStudent(fullName, email, studentId, password);
    }

    public DbStatus registerAdmin(String email, String password, String activationCode, String fullName) {
        return db.registerAdmin(fullName, email, password, activationCode);
    }

    public DbStatus loginStudent(String email, String password) {
        return db.loginStudent(email, password);
    }

    public DbStatus loginAdmin(String email, String password) {
        return db.loginAdmin(email, password);
    }

    public DbStatus activateAccount(String email, String activationCode) {
        DbStatus status = db.verifyActivationCode(email, activationCode);
        if (status == DbStatus.SUCCESS){
            return db.setProfileActivation(email);
        }
        return status;
    }
    
}