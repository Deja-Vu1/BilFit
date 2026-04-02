package managers;

import java.time.LocalDate;
import database.Database;
import database.DbStatus;
import models.Admin;
import models.Facility;
import models.Notification;
import models.Reservation;
import models.Student;

public class AdminManager {

    private Database db;
    private NotificationManager notifManager; // DI eklendi

    public AdminManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
    }

    public DbStatus banStudent(Admin admin, Student student) {
        if (admin == null || student == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateStudentBanStatus(student.getBilkentEmail(), true);
        if (status == DbStatus.SUCCESS) {
            student.setBanned(true);
        }
        return status;
    }

    public DbStatus setFacilityMaintenanceStatus(Facility facility, boolean isUnderMaintenance) {
        if (facility == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateFacilityMaintenance(facility.getName(), isUnderMaintenance);
        if (status == DbStatus.SUCCESS) {
            facility.setUnderMaintenance(isUnderMaintenance);
        }
        return status;
    }

    public Notification sendSystemBroadcast(Admin admin, String message) {
        if (admin == null || message == null || message.isEmpty()) return null;
        // İçeride yeniden üretmek yerine DI üzerinden kullanıyoruz
        return notifManager.broadcastToAll("System Update by " + admin.getNickname(), message);
    }

    public DbStatus givePenaltyPoint(Student targetStudent, int points) {
        if (targetStudent == null || points <= 0) return DbStatus.QUERY_ERROR;

        int newPoints = targetStudent.getPenaltyPoints() + points;
        DbStatus status = db.updateStudentPenalty(targetStudent.getBilkentEmail(), newPoints);
        if (status == DbStatus.SUCCESS) {
            targetStudent.setPenaltyPoints(newPoints);
        }
        return status;
    }

    public DbStatus editFieldAvailability(Reservation reservation, LocalDate newDate, String newTimeSlot) {
        if (reservation == null || newDate == null || newTimeSlot == null) return DbStatus.QUERY_ERROR;

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
        if (admin == null || newNickname == null || newNickname.isEmpty()) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateUserNickname(admin.getBilkentEmail(), newNickname);
        if (status == DbStatus.SUCCESS) {
            admin.setNickname(newNickname);
        }
        return status;
    }

    public DbStatus updatePassword(Admin admin, String newPassword) {
        if (admin == null || newPassword == null || newPassword.isEmpty()) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updatePassword(admin.getBilkentEmail(), newPassword);
        if (status == DbStatus.SUCCESS) {
            admin.setPassword(newPassword);
        }
        return status;
    }
}