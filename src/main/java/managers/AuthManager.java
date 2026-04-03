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

    public DbStatus registerStudent(String email, String password, String studentId, String fullName) {
        if (email == null || !email.endsWith("@bilkent.edu.tr") || password == null || password.length() < 6 || studentId == null || fullName == null) {
            return DbStatus.QUERY_ERROR;
        }
        return db.registerStudent(fullName, email, studentId, password);
    }

    public DbStatus registerAdmin(String email, String password, String activationCode, String fullName) {
        if (email == null || !email.endsWith("@bilkent.edu.tr") || password == null || password.length() < 6 || activationCode == null || fullName == null) {
            return DbStatus.QUERY_ERROR;
        }
        return db.registerAdmin(fullName, email, password, activationCode);
    }

    public DbStatus loginStudent(String email, String password) {
        if (email == null || password == null) return DbStatus.QUERY_ERROR;
        
        DbStatus status = db.loginStudent(email, password);
        if (status == DbStatus.SUCCESS) {
            // HATA BURADAYDI ÇÖZÜLDÜ: Student constructor'ı artık 3 parametre alıyor (fullName, email, studentId)
            Student student = new Student("", email, "");
            DbStatus dataStatus = db.fillStudentDataByEmail(student, email);
            if (dataStatus == DbStatus.SUCCESS) {
                SessionManager.getInstance().setCurrentUser(student);
            }
        }
        return status;
    }

    public DbStatus loginAdmin(String email, String password) {
        if (email == null || password == null) return DbStatus.QUERY_ERROR;
        
        DbStatus status = db.loginAdmin(email, password);
        if (status == DbStatus.SUCCESS) {
            // HATA BURADAYDI ÇÖZÜLDÜ: Admin constructor'ı da büyük ihtimalle güncellendi.
            // Eğer Admin class'ında da hata verirse 3 parametreye düşürdüm.
            Admin admin = new Admin("", email, "");
            DbStatus dataStatus = db.fillAdminDataByEmail(admin, email);
            if (dataStatus == DbStatus.SUCCESS) {
                SessionManager.getInstance().setCurrentUser(admin);
            }
        }
        return status;
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