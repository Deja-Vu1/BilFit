package models;

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
        this.isUnderMaintenance = false;
    }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCampusLocation() { return campusLocation; }
    public void setCampusLocation(String campusLocation) { this.campusLocation = campusLocation; }
    public SportType getSportType() { return sportType; }
    public void setSportType(SportType sportType) { this.sportType = sportType; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public boolean isUnderMaintenance() { return isUnderMaintenance; }
    public void setUnderMaintenance(boolean underMaintenance) { isUnderMaintenance = underMaintenance; }

    public String toString() {
        return "Facility{" +
                "facilityId='" + facilityId + '\'' +
                ", name='" + name + '\'' +
                ", campusLocation='" + campusLocation + '\'' +
                ", sportType=" + sportType +
                ", capacity=" + capacity +
                ", isUnderMaintenance=" + isUnderMaintenance +
                '}';
    }
}