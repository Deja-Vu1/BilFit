package managers;

import database.Database;
import database.DbStatus;
import models.Admin;
import models.Facility;
import models.Student;

public class AdminManager {

    private Database db;

    public AdminManager(Database db) {
        this.db = db;
    }

    public DbStatus banStudent(Admin admin, Student student) {
        DbStatus status = db.updateStudentBanStatus(student.getStudentId(), true);
        
        if (status == DbStatus.SUCCESS) {
            admin.banUser(student);
        }
        
        return status;
    }

    public DbStatus setFacilityMaintenanceStatus(Facility facility, boolean isUnderMaintenance) {
        DbStatus status = db.updateFacilityMaintenance(facility.getName(), isUnderMaintenance);
        
        if (status == DbStatus.SUCCESS) {
            facility.setMaintenanceStatus(isUnderMaintenance);
        }
        
        return status;
    }

    public DbStatus sendSystemBroadcast(Admin admin, String message) {
        DbStatus status = db.insertNotification("BROADCAST", message);
        
        if (status == DbStatus.SUCCESS) {
            admin.createNotification(message);
        }
        
        return status;
    }
}