package managers;

import database.Database;
import database.DbStatus;
import models.Admin;
import models.Student;

public class AuthManager {

    private Database db;

    public AuthManager(Database db) {
        this.db = db;
    }

    private boolean isValidBilkentEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();
        // Sadece Bilkent uzantılarını kabul et
        return cleanEmail.endsWith("@ug.bilkent.edu.tr") || 
               cleanEmail.endsWith("@alumni.bilkent.edu.tr") || 
               cleanEmail.endsWith("@bilkent.edu.tr");
    }

    public DbStatus registerStudent(String email, String password, String studentId, String fullName) {
        // E-postayı kesinlikle küçük harfe çeviriyoruz ki SQL patlamasın
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String cleanId = studentId != null ? studentId.trim() : "";
        String cleanName = fullName != null ? fullName.trim() : "";

        if (!isValidBilkentEmail(cleanEmail) || password == null || password.length() < 6 || cleanId.isEmpty() || cleanName.isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }
        
        // Veritabanına tertemiz veriler gidiyor
        return db.registerStudent(cleanName, cleanEmail, cleanId, password);
    }

    public DbStatus registerAdmin(String email, String password, String activationCode, String fullName) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        
        if (!isValidBilkentEmail(cleanEmail) || password == null || password.length() < 6 || activationCode == null || fullName == null) {
            return DbStatus.QUERY_ERROR;
        }
        return db.registerAdmin(fullName.trim(), cleanEmail, password, activationCode.trim());
    }

    public DbStatus loginStudent(String email, String password) {
        if (email == null || password == null) return DbStatus.QUERY_ERROR;
        
        String cleanEmail = email.trim().toLowerCase();
        DbStatus status = db.loginStudent(cleanEmail, password);
        
        if (status == DbStatus.SUCCESS) {
            Student student = new Student("", cleanEmail, "");
            DbStatus dataStatus = db.fillStudentDataByEmail(student, cleanEmail);
            if (dataStatus == DbStatus.SUCCESS) {
                SessionManager.getInstance().setCurrentUser(student);
            }
        }
        return status;
    }

    public DbStatus loginAdmin(String email, String password) {
        if (email == null || password == null) return DbStatus.QUERY_ERROR;
        
        String cleanEmail = email.trim().toLowerCase();
        DbStatus status = db.loginAdmin(cleanEmail, password);
        
        if (status == DbStatus.SUCCESS) {
            Admin admin = new Admin("", cleanEmail, "");
            DbStatus dataStatus = db.fillAdminDataByEmail(admin, cleanEmail);
            if (dataStatus == DbStatus.SUCCESS) {
                SessionManager.getInstance().setCurrentUser(admin);
            }
        }
        return status;
    }

    public DbStatus activateAccount(String email, String activationCode) {
        if (email == null || activationCode == null) return DbStatus.QUERY_ERROR;
        
        String cleanEmail = email.trim().toLowerCase();
        DbStatus status = db.verifyActivationCode(cleanEmail, activationCode.trim());
        if (status == DbStatus.SUCCESS) {
            return db.setProfileActivation(cleanEmail);
        }
        return status;
    }
}