package models;

import java.time.LocalDate;

public class Duello extends Reservation {
    private String accessCode;
    private String requiredSkillLevel;
    private int emptySlots;
    private boolean isMatched;
    private Match scheduledMatch;





    

    public Duello(String reservationId, Facility facility, LocalDate date, String timeSlot, String accessCode, String requiredSkillLevel, int emptySlots) {
        super(reservationId, facility, date, timeSlot); // parent reservation claass call
        this.accessCode = accessCode;
        this.requiredSkillLevel = requiredSkillLevel;
        this.emptySlots = emptySlots;
        this.isMatched = false;
        this.scheduledMatch = null;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getRequiredSkillLevel() {
        return requiredSkillLevel;
    }

    public void setRequiredSkillLevel(String requiredSkillLevel) {
        this.requiredSkillLevel = requiredSkillLevel;
    }

    public int getEmptySlots() {
        return emptySlots;
    }

    public void setEmptySlots(int emptySlots) {
        this.emptySlots = emptySlots;
    }

    public boolean isMatched() {
        return isMatched;
    }

    public void setMatched(boolean isMatched) {
        this.isMatched = isMatched;
    }

    public Match getScheduledMatch() {
        return scheduledMatch;
    }

    public void setScheduledMatch(Match scheduledMatch) {
        this.scheduledMatch = scheduledMatch;
    }

    public void publishDuello() {
        System.out.println("Duello published publicly! Looking for opponents with skill level: " + this.requiredSkillLevel);
    }

    public void requestToJoin(Student student) {
        if (!isMatched && emptySlots > 0) {
            System.out.println(student.getNickname() + " has requested to join the duello.");
            // trigger of notification to the host will be createed by the controller
        }
    }

    public void acceptRequest(Student student) {
        if (emptySlots > 0 && !isMatched) {
            this.attendees.add(student);
            this.emptySlots--;
            System.out.println(student.getNickname() + " join request was accepted.");
            
            if (emptySlots == 0) {
                this.isMatched = true;
                System.out.println("Duello slots are full. Match is ready!");
            }
        }
    }

    public boolean joinWithCode(Student student, String code) {
        if (this.accessCode != null && this.accessCode.equals(code) && emptySlots > 0 && !isMatched) {
            this.attendees.add(student);
            this.emptySlots--;
            System.out.println(student.getNickname() + " successfully joined the private duello using the access code.");
            
            if (emptySlots == 0) {
                this.isMatched = true;
                System.out.println("Private duello is fully booked and ready!");
            }
            return true;
        }
        System.out.println("Failed to join duello. Incorrect code or no available slots.");
        return false;
    }
}