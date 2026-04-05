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
    private NotificationManager notifManager;

    public AdminManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
    }

    public DbStatus addFacility(Admin admin, Facility facility) {
        if (admin == null || facility == null) return DbStatus.QUERY_ERROR;
        
        return db.insertFacility(facility.getName(), facility.getCampusLocation(), facility.getSportType().name(), facility.getCapacity());
    }

    public DbStatus removeFacility(Admin admin, Facility facility) {
        if (admin == null || facility == null) return DbStatus.QUERY_ERROR;
        
        return db.deleteFacility(facility.getName());
    }

    public DbStatus banStudent(Admin admin, Student student) {
        if (admin == null || student == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateStudentBanStatus(student.getBilkentEmail(), true);
        if (status == DbStatus.SUCCESS) {
            student.setBanned(true);
            notifManager.sendToUser(student, "Account Banned", "Your account has been banned by an administrator.");
        }
        return status;
    }

    // YENİ GÜNCELLEME: Ban kalktığında ceza puanı 0'lanır!
    public DbStatus unbanStudent(Admin admin, Student student) {
        if (admin == null || student == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateStudentBanStatus(student.getBilkentEmail(), false);
        if (status == DbStatus.SUCCESS) {
            student.setBanned(false);
            
            // Banı açılan öğrenciye temiz sayfa açıyoruz (Ceza puanını veritabanında ve modelde 0 yap)
            db.updateStudentPenalty(student.getBilkentEmail(), 0);
            student.setPenaltyPoints(0);
            
            notifManager.sendToUser(student, "Account Unbanned", "Your account ban has been lifted and your penalty points have been reset to 0.");
        }
        return status;
    }

    public DbStatus setFacilityMaintenanceStatus(Facility facility, boolean isUnderMaintenance) {
        if (facility == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateFacilityMaintenance(facility.getName(), isUnderMaintenance);
        if (status == DbStatus.SUCCESS) {
            facility.setUnderMaintenance(isUnderMaintenance);
            if (isUnderMaintenance) {
                notifManager.broadcastToAll("Facility Maintenance", facility.getName() + " is now under maintenance. Related reservations might be affected.");
            }
        }
        return status;
    }

    public Notification sendSystemBroadcast(Admin admin, String message) {
        if (admin == null || message == null || message.trim().isEmpty()) return null;
        return notifManager.broadcastToAll("System Update by " + admin.getNickname(), message);
    }

    public DbStatus givePenaltyPoint(Admin admin, Student targetStudent, int points) {
        if (admin == null || targetStudent == null || points <= 0) return DbStatus.QUERY_ERROR;

        int newPoints = targetStudent.getPenaltyPoints() + points;
        DbStatus status = db.updateStudentPenalty(targetStudent.getBilkentEmail(), newPoints);
        if (status == DbStatus.SUCCESS) {
            targetStudent.setPenaltyPoints(newPoints);
            notifManager.sendToUser(targetStudent, "Penalty Issued", "You have received " + points + " penalty points.");
        }
        return status;
    }

    public DbStatus reducePenaltyPoint(Admin admin, Student student, int amount) {
        if (admin == null || student == null || amount <= 0) return DbStatus.QUERY_ERROR;
        
        int newPoints = Math.max(0, student.getPenaltyPoints() - amount);
        DbStatus status = db.updateStudentPenalty(student.getBilkentEmail(), newPoints);
        
        if (status == DbStatus.SUCCESS) {
            student.setPenaltyPoints(newPoints);
            notifManager.sendToUser(student, "Penalty Reduced", "Your penalty points have been reduced by " + amount + ".");
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
            
            for (Student attendee : reservation.getAttendees()) {
                notifManager.sendToUser(attendee, "Reservation Updated", "Your reservation at " + reservation.getFacility().getName() + " has been moved to " + newDate + " " + newTimeSlot);
            }
        }
        return status;
    }

    public DbStatus updateNickname(Admin admin, String newNickname) {
        if (admin == null || newNickname == null || newNickname.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateUserNickname(admin.getBilkentEmail(), newNickname);
        if (status == DbStatus.SUCCESS) {
            admin.setFullName(newNickname); // YENİ: setNickname yerine setFullName olmalı, çünkü üst sınıf User
        }
        return status;
    }

    public DbStatus updatePassword(Admin admin, String newPassword) {
        if (admin == null || newPassword == null || newPassword.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updatePassword(admin.getBilkentEmail(), newPassword);
        if (status == DbStatus.SUCCESS) {
            admin.setPassword(newPassword);
        }
        return status;
    }
}