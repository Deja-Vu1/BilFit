package managers;

import database.Database;
import database.DbStatus;

public class AuthManager {

    private Database db;

    public AuthManager(Database db) {
        this.db = db;
    }

    public DbStatus registerStudent(String email, String password, String studentId, String fullName) {
        // Database'deki metod: registerStudent(String name, String bilkentMail, String studentId, String passwordHash)
        return db.registerStudent(fullName, email, studentId, password);
    }

   public DbStatus registerAdmin(String email, String password, String activationCode, String fullName) {
        // Adminlerin kayıt olabilmesi için önce bir aktivasyon kodunu doğrulaması gerekiyorsa bu kısım kalabilir.
        DbStatus verificationStatus = db.verifyActivationCode(email, activationCode);
        
        if (verificationStatus != DbStatus.SUCCESS) {
            return verificationStatus;
        }

        // Database'deki metod: registerAdmin(String name, String email, String passwordHash)
        return db.registerAdmin(fullName, email, password);
    }

    public DbStatus loginStudent(String email, String password) {
        // db.login yerine db.loginStudent kullanıyoruz
        return db.loginStudent(email, password);
    }

    public DbStatus loginAdmin(String email, String password) {
        // db.login yerine db.loginAdmin kullanıyoruz
        return db.loginAdmin(email, password);
    }

    public DbStatus activateAccount(String email, String activationCode) {
        return db.verifyActivationCode(email, activationCode);
    }
}