package managers;

import database.Database;
import database.DbStatus;
import models.Reservation;
import models.Student;

public class PenaltyManager {

    
    private static final double SUSPENSION_THRESHOLD = 50.0;
    private static final int MAX_PENALTY_POINTS = 3;
    private Database db;

    public PenaltyManager(Database db) {
        this.db = db;
    }

    public DbStatus processNoShow(Student student, Reservation reservation) {
        DbStatus status = db.updateReservationAttendance(reservation.getReservationId(), false);
        if (status != DbStatus.SUCCESS) {
            return status;
        }

        DbStatus penaltyStatus = db.updateStudentPenalty(student.getStudentId(), student.getPenaltyPoints() + 1);
        if (penaltyStatus != DbStatus.SUCCESS) {
            return penaltyStatus;
        }

        reservation.markAttendance(false);
        student.addPenaltyPoint(1);
        return checkSuspensionStatus(student);
    }

    public DbStatus processAttendance(Student student, Reservation reservation) {
        DbStatus status = db.updateReservationAttendance(reservation.getReservationId(), true);
        if (status == DbStatus.SUCCESS) {
            reservation.markAttendance(true);
        }
        return status;
    }

    private DbStatus checkSuspensionStatus(Student student) {
        if (student.getReliabilityScore() < SUSPENSION_THRESHOLD || student.getPenaltyPoints() >= MAX_PENALTY_POINTS) {
            DbStatus status = db.updateStudentProfileVisibility(student.getStudentId(), false);
            if (status == DbStatus.SUCCESS) {
                student.updateProfileVisibility(false);
            }
            return status;
        }
        return DbStatus.SUCCESS;
    }
}