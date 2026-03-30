package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Facility {

    private String facilityId;
    private String name;
    private String campusLocation;
    private SportType sportType;
    private int capacity;
    private boolean isUnderMaintenance;

    public Facility(String facilityId, String name, String campusLocation, SportType sportType, int capacity) {

        this.facilityId = facilityId;
        this.name = name;
        this.campusLocation = campusLocation;
        this.sportType = sportType;
        this.capacity = capacity;
        this.isUnderMaintenance = false; // Default : Open
    }

    public String getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCampusLocation() {
        return campusLocation;
    }

    public void setCampusLocation(String campusLocation) {
        this.campusLocation = campusLocation;
    }

    public SportType getSportType() {
        return sportType;
    }

    public void setSportType(SportType sportType) {
        this.sportType = sportType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isUnderMaintenance() {
        return isUnderMaintenance;
    }

    public void setUnderMaintenance(boolean isUnderMaintenance) {
        this.isUnderMaintenance = isUnderMaintenance;
    }

    public boolean checkAvailability(LocalDate date, String timeSlot) {
        if (isUnderMaintenance) {

            System.out.println(this.name + " is currently under maintenance. Booking denied.");
            return false;
        }
        // Logic to verify database records for overlaps will be added here!!!!
        return true; 
    }

    public void setMaintenanceStatus(boolean status) {
        this.isUnderMaintenance = status;
        System.out.println(this.name + " maintenance status updated to: " + status);
    }

    public List<String> getAvailableTimeSlots(LocalDate date) {
        
        // Will return from DB later !!!
        List<String> availableSlots = new ArrayList<>();
        availableSlots.add("18.45-19.45");
        availableSlots.add("19.45-20.45");
        return availableSlots;
    }
    
    public String getName() { return name; }
}