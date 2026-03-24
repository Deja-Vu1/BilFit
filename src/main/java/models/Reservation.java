package java.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Reservation {

    protected String reservationId; // For Duello protected
    protected LocalDate date;
    protected String timeSlot;
    protected boolean isCancelled;
    protected boolean hasAttended;
    protected Facility facility;
    protected List<Student> attendees;

    public Reservation(String reservationId, Facility facility, LocalDate date, String timeSlot) {

        this.reservationId = reservationId;
        this.facility = facility;
        this.date = date;
        this.timeSlot = timeSlot;
        this.isCancelled = false;
        this.hasAttended = false;
        this.attendees = new ArrayList<>();
    }

    public boolean createReservation(Student student) {
        if (facility.checkAvailability(this.date, this.timeSlot)) {
            attendees.add(student);
            System.out.println("Reservation successfully created for " + student.getNickname() + " at " + facility.getName());
            return true;
        }
        return false;
    }

    public void cancelReservation() {
        this.isCancelled = true;
        System.out.println("Reservation ID " + this.reservationId + " has been cancelled. Slot is now free.");
    }

    public void markAttendance(boolean status) {
        this.hasAttended = status;


        for (Student s : attendees) {
            s.updateReliabilityScore(status); // reliability score update based on the studentd attendience
        }
        System.out.println("Attendance marked as: " + status + ". Reliability scores updated.");
    }
    
    public String getReservationId() { return reservationId; }
}