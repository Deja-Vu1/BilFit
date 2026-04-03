package managers;

import java.time.LocalDate;
import java.util.ArrayList;
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
        if (student == null || facility == null || date == null || timeSlot == null || timeSlot.trim().isEmpty()) {
            return null;
        }
        
        if (date.isBefore(LocalDate.now())) {
            return null;
        }
        
        if (date.isAfter(LocalDate.now().plusDays(7))) {
            return null;
        }
        
        if (!student.isCanAttend() || student.isBanned()) {
            return null; 
        }

        if (facility.isUnderMaintenance()) {
            return null;
        }

        boolean isAvailable = db.checkFacilityAvailability(facility.getName(), date, timeSlot);
        if (!isAvailable) {
            return null;
        }

        DbStatus status = db.insertReservation(student.getBilkentEmail(), facility.getName(), date, timeSlot);
        
        if (status == DbStatus.SUCCESS) {
            String generatedResId = java.util.UUID.randomUUID().toString();
            
            ArrayList<Reservation> userReservations = db.getReservationsByEmail(student.getBilkentEmail());
            for (Reservation r : userReservations) {
                if (r.getDate().equals(date) && r.getTimeSlot().equals(timeSlot) && r.getFacility().getName().equals(facility.getName())) {
                    generatedResId = r.getReservationId();
                    break;
                }
            }

            Reservation newReservation = new Reservation(generatedResId, facility, date, timeSlot);
            if (newReservation.getAttendees() != null && !newReservation.getAttendees().contains(student)) {
                newReservation.getAttendees().add(student);
            }
            return newReservation;
        }
        
        return null;
    }

    public DbStatus cancelReservation(Reservation reservation) {
        if (reservation == null) {
            return DbStatus.DATA_NOT_FOUND;
        }

        if (reservation.getDate().isBefore(LocalDate.now())) {
            return DbStatus.QUERY_ERROR; 
        }

        if (reservation.isCancelled()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.deleteReservation(reservation.getReservationId());
        if (status == DbStatus.SUCCESS) {
            reservation.setCancelled(true);
        }
        
        return status;
    }
}