package managers;

import database.Database;
import database.DbStatus;
import models.Reservation;
import models.Student;

public class PenaltyManager {

    private Database db;

    public PenaltyManager() {
        this.db = Database.getInstance();
    }

    public DbStatus processNoShow(Student student, Reservation reservation) {
        if (reservation.isCancelled()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateReservationAttendance(reservation.getReservationId(), false);
        if (status != DbStatus.SUCCESS) {
            return status;
        }

        int newPoints = student.getPenaltyPoints() + 1;
        DbStatus penaltyStatus = db.updateStudentPenalty(student.getStudentId(), newPoints);
        if (penaltyStatus != DbStatus.SUCCESS) {
            return penaltyStatus;
        }

        reservation.setHasAttended(false);
        student.setPenaltyPoints(newPoints);
        
        return DbStatus.SUCCESS;
    }

    public DbStatus processAttendance(Student student, Reservation reservation) {
        if (reservation.isCancelled()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateReservationAttendance(reservation.getReservationId(), true);
        if (status == DbStatus.SUCCESS) {
            reservation.setHasAttended(true);
        }
        return status;
    }
}