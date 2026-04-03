package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Reservation {
    protected String reservationId;
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

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public boolean isCancelled() { return isCancelled; }
    public void setCancelled(boolean cancelled) { isCancelled = cancelled; }
    public boolean isHasAttended() { return hasAttended; }
    public void setHasAttended(boolean hasAttended) { this.hasAttended = hasAttended; }
    public Facility getFacility() { return facility; }
    public void setFacility(Facility facility) { this.facility = facility; }
    public List<Student> getAttendees() { return attendees; }
    public void setAttendees(List<Student> attendees) { this.attendees = attendees; }

    public String toString() {
        return "Reservation{" +
                "reservationId='" + reservationId + '\'' +
                ", date=" + date +
                ", timeSlot='" + timeSlot + '\'' +
                ", isCancelled=" + isCancelled +
                ", hasAttended=" + hasAttended +
                ", facility=" + facility +
                ", attendees=" + attendees +
                '}';
    }
}