package managers;

import java.time.LocalDate;
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
        if (student == null || facility == null || date == null || timeSlot == null) return null;
        if (!student.isCanAttend() || facility.isUnderMaintenance()) {
            return null; 
        }

        boolean isAvailable = db.checkFacilityAvailability(facility.getName(), date, timeSlot);
        if (!isAvailable) {
            return null;
        }

        String generatedResId = db.insertReservation(student.getBilkentEmail(), facility.getName(), date, timeSlot);
        if (generatedResId != null) {
            Reservation newReservation = new Reservation(generatedResId, facility, date, timeSlot);
            newReservation.getAttendees().add(student);
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
            reservation.setCancelled(true);
        }
        return status;
    }
}