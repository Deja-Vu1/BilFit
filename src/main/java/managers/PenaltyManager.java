package managers;

import database.Database;
import database.DbStatus;
import models.Reservation;
import models.Student;

public class PenaltyManager {

    private Database db;
    private NotificationManager notifManager;

    public PenaltyManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
    }

    public DbStatus processNoShow(Student student, Reservation reservation) {
        if (student == null || reservation == null || reservation.isCancelled()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateReservationAttendance(reservation.getReservationId(), false);
        if (status != DbStatus.SUCCESS) {
            return status;
        }

        int newPoints = student.getPenaltyPoints() + 1;
        DbStatus penaltyStatus = db.updateStudentPenalty(student.getBilkentEmail(), newPoints);
        
        if (penaltyStatus == DbStatus.SUCCESS) {
            reservation.setHasAttended(false);
            student.setPenaltyPoints(newPoints);
            
            if (newPoints >= 3) {
                DbStatus banStatus = db.updateStudentBanStatus(student.getBilkentEmail(), true);
                if (banStatus == DbStatus.SUCCESS) {
                    student.setBanned(true);
                    student.setPenaltyPoints(0); 
                    db.updateStudentPenalty(student.getBilkentEmail(), 0);
                    notifManager.sendToUser(student, "Account Suspended", "You have reached the maximum penalty limit. Your account is banned for 7 days.");
                }
            } else {
                notifManager.sendToUser(student, "Penalty Applied", "You did not attend your reservation. Penalty points: " + newPoints + "/3");
            }
        }
        
        return penaltyStatus;
    }

    public DbStatus processAttendance(Student student, Reservation reservation) {
        if (student == null || reservation == null || reservation.isCancelled()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateReservationAttendance(reservation.getReservationId(), true);
        if (status == DbStatus.SUCCESS) {
            reservation.setHasAttended(true);
        }
        return status;
    }
}