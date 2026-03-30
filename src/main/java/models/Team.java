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

    public boolean addMember(Student student, String code) {
        boolean isCodeValid = (this.accessCode == null || this.accessCode.isEmpty() || this.accessCode.equals(code));
        
        if (!isFull() && isCodeValid && !members.contains(student)) {
            members.add(student);
            return true;
        }
        return false;
    }

    public void removeMember(Student student) {
        if (!student.equals(captain)) { 
            members.remove(student);
        }
    }

    public boolean isFull() {
        return members.size() >= maxCapacity;
    }

    public Student getCaptain() { return captain; }
    public List<Student> getMembers() { return members; }
    public String getTeamName() { return teamName; }
}