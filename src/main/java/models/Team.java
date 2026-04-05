package models;

import java.util.ArrayList;
import java.util.List;

public class Team {
    private String teamId;
    private String teamName;
    private String accessCode;
    private int maxCapacity;
    private boolean ge250Requested;
    private List<Student> members;
    private Student captain;

    public Team(String teamId, String teamName, String accessCode, int maxCapacity, boolean ge250Requested, Student captain) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.accessCode = accessCode;
        this.maxCapacity = maxCapacity;
        this.ge250Requested = ge250Requested;
        this.captain = captain;
        this.members = new ArrayList<>();
        this.members.add(captain);
    }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public boolean isGe250Requested() { return ge250Requested; }
    public void setGe250Requested(boolean ge250Requested) { this.ge250Requested = ge250Requested; }
    public List<Student> getMembers() { return members; }
    public void setMembers(List<Student> members) { this.members = members; }
    public Student getCaptain() { return captain; }
    public void setCaptain(Student captain) { this.captain = captain; }

    public String toString() {
        return "Team{" +
                "teamId='" + teamId + '\'' +
                ", teamName='" + teamName + '\'' +
                ", accessCode='" + accessCode + '\'' +
                ", maxCapacity=" + maxCapacity +
                ", ge250Requested=" + ge250Requested +
                ", captain=" + captain.getFullName() +
                '}';
    }
}