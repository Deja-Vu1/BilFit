package managers;

import java.time.LocalDate;
import java.util.UUID;
import database.Database;
import database.DbStatus;
import models.Facility;
import models.Reservation;
import models.Student;

public class ReservationManager {

    private Database db;

    public ReservationManager(Database db) {
        this.db = db;
    }

    public Reservation makeReservation(Student student, Facility facility, LocalDate date, String timeSlot) {
        boolean isAvailable = db.checkFacilityAvailability(facility.getName(), date, timeSlot);
        
        if (!isAvailable) {
            return null;
        }

        String uniqueResId = UUID.randomUUID().toString();
        DbStatus status = db.insertReservation(uniqueResId, student.getStudentId(), facility.getName(), date, timeSlot);
        
        if (status == DbStatus.SUCCESS) {
            Reservation newReservation = new Reservation(uniqueResId, facility, date, timeSlot);
            newReservation.createReservation(student);
            return newReservation;
        }
        
        return null;
    }

    public DbStatus cancelReservation(Reservation reservation) {
        if (reservation == null) {
            return DbStatus.DATA_NOT_FOUND;
        }

        DbStatus status = db.deleteReservation(reservation.getReservationId());
        if (status == DbStatus.SUCCESS) {
            reservation.cancelReservation();
        }
        return status;
    }
}