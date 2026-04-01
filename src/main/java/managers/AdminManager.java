package managers;

import java.time.LocalDate;
import database.Database;
import database.DbStatus;
import models.Admin;
import models.Facility;
import models.Reservation;
import models.Student;

public class AdminManager {

    private Database db;

    public AdminManager(Database db) {
        this.db = db;
    }

    public DbStatus banStudent(Admin admin, Student student) {
        DbStatus status = db.updateStudentBanStatus(student.getStudentId(), true);
        
        if (status == DbStatus.SUCCESS) {
            student.setBanned(true);
            // student.setPublicProfile(false);
        }
        
        return status;
    }

    public DbStatus setFacilityMaintenanceStatus(Facility facility, boolean isUnderMaintenance) {
        DbStatus status = db.updateFacilityMaintenance(facility.getName(), isUnderMaintenance);
        
        if (status == DbStatus.SUCCESS) {
            facility.setUnderMaintenance(isUnderMaintenance);
        }
        
        return status;
    }

    public DbStatus sendSystemBroadcast(Admin admin, String message) {
        return db.insertNotification("BROADCAST", "System Update", message);
    }

    public DbStatus givePenaltyPoint(Student targetStudent, int points) {
        DbStatus status = db.updateStudentPenalty(targetStudent.getStudentId(), targetStudent.getPenaltyPoints() + points);
        
        if (status == DbStatus.SUCCESS) {
            targetStudent.setPenaltyPoints(targetStudent.getPenaltyPoints() + points);
        }
        
        return status;
    }

    public DbStatus editFieldAvailability(Reservation reservation, LocalDate newDate, String newTimeSlot) {
        boolean isAvailable = db.checkFacilityAvailability(reservation.getFacility().getName(), newDate, newTimeSlot);
        
        if (!isAvailable) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateReservationTime(reservation.getReservationId(), newDate, newTimeSlot);
        
        if (status == DbStatus.SUCCESS) {
            reservation.setDate(newDate);
            reservation.setTimeSlot(newTimeSlot);
        }
        
        return status;
    }

    public DbStatus updateNickname(Admin admin, String newNickname) {
        DbStatus status = db.updateUserNickname(admin.getBilkentEmail(), newNickname);
        
        if (status == DbStatus.SUCCESS) {
            admin.setNickname(newNickname);
        }
        
        return status;
    }

    public DbStatus updatePassword(Admin admin, String newPassword) {
        DbStatus status = db.updateUserPassword(admin.getBilkentEmail(), newPassword);
        
        if (status == DbStatus.SUCCESS) {
            admin.setPassword(newPassword);
        }
        
        return status;
    }
}