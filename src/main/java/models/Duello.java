package models;

import java.time.LocalDate;

public class Duello extends Reservation {
    private String accessCode;
    private String requiredSkillLevel;
    private int emptySlots;
    private boolean isMatched;
    private Match scheduledMatch;

    public Duello(String reservationId, Facility facility, LocalDate date, String timeSlot, String accessCode, String requiredSkillLevel, int emptySlots) {
        super(reservationId, facility, date, timeSlot);
        this.accessCode = accessCode;
        this.requiredSkillLevel = requiredSkillLevel;
        this.emptySlots = emptySlots;
        this.isMatched = false;
        this.scheduledMatch = null;
    }

    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public String getRequiredSkillLevel() { return requiredSkillLevel; }
    public void setRequiredSkillLevel(String requiredSkillLevel) { this.requiredSkillLevel = requiredSkillLevel; }
    public int getEmptySlots() { return emptySlots; }
    public void setEmptySlots(int emptySlots) { this.emptySlots = emptySlots; }
    public boolean isMatched() { return isMatched; }
    public void setMatched(boolean matched) { isMatched = matched; }
    public Match getScheduledMatch() { return scheduledMatch; }
    public void setScheduledMatch(Match scheduledMatch) { this.scheduledMatch = scheduledMatch; }

    public String toString() {
        return "Duello{" +
                "reservationId='" + getReservationId() + '\'' +
                ", facility=" + getFacility().getName() +
                ", date=" + getDate() +
                ", timeSlot='" + getTimeSlot() + '\'' +
                ", accessCode='" + accessCode + '\'' +
                ", requiredSkillLevel='" + requiredSkillLevel + '\'' +
                ", emptySlots=" + emptySlots +
                ", isMatched=" + isMatched +
                '}';
    }
}