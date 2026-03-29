package managers;

import java.time.LocalDate;
import java.util.UUID;

import models.Facility;
import models.Reservation;
import models.Student;

public class ReservationManager {

    public Reservation makeReservation(Student student, Facility facility, LocalDate date, String timeSlot) {
        if (!facility.checkAvailability(date, timeSlot)) {
            return null;
        }

        String uniqueResId = UUID.randomUUID().toString();
        Reservation newReservation = new Reservation(uniqueResId, facility, date, timeSlot);
        
        boolean success = newReservation.createReservation(student);
        
        if (success) {
            return newReservation;
        }
        
        return null;
    }

    public void cancelReservation(Reservation reservation) {
        if (reservation != null) {
            reservation.cancelReservation();
        }
    }
}