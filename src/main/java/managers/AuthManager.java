package managers;

import database.Database;
import database.DbStatus;

public class AuthManager {

    private Database db;

    public AuthManager(Database db) {
        this.db = db;
    }

    public DbStatus registerStudent(String email, String password, String studentId, String fullName) {
        if (email == null || password == null || studentId == null || fullName == null) return DbStatus.QUERY_ERROR;
        return db.registerStudent(fullName, email, studentId, password);
    }

    public DbStatus registerAdmin(String email, String password, String activationCode, String fullName) {
        if (email == null || password == null || activationCode == null || fullName == null) return DbStatus.QUERY_ERROR;
        return db.registerAdmin(fullName, email, password, activationCode);
    }

    public DbStatus loginStudent(String email, String password) {
        if (email == null || password == null) return DbStatus.QUERY_ERROR;
        return db.loginStudent(email, password);
    }

    public DbStatus loginAdmin(String email, String password) {
        if (email == null || password == null) return DbStatus.QUERY_ERROR;
        return db.loginAdmin(email, password);
    }

    public DbStatus activateAccount(String email, String activationCode) {
        if (email == null || activationCode == null) return DbStatus.QUERY_ERROR;
        DbStatus status = db.verifyActivationCode(email, activationCode);
        if (status == DbStatus.SUCCESS){
            return db.setProfileActivation(email);
        }
        return status;
    }
}