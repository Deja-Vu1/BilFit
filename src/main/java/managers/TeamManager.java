package managers;

import database.Database;
import database.DbStatus;
import models.Student;

public class TeamManager {

    private Database db;

    public TeamManager(Database db) {
        this.db = db;
    }

    public DbStatus addMemberToTeam(String teamId, Student student, String inputCode) {
        if (teamId == null || student == null || inputCode == null) {
            return DbStatus.QUERY_ERROR;
        }
        return db.beTeamMember(teamId, student, inputCode);
    }

    public DbStatus removeMemberFromTeam(String teamId, Student student) {
        if (teamId == null || student == null) {
            return DbStatus.QUERY_ERROR;
        }
        return db.deleteTeamMember(teamId, student.getBilkentEmail());
    }

    public DbStatus disbandTeam(String teamId) {
        if (teamId == null) {
            return DbStatus.QUERY_ERROR;
        }
        return db.deleteTeam(teamId);
    }
    public java.util.List<Student> getTeamMembers(String teamId) {
        if (teamId == null) return new java.util.ArrayList<>();
        return db.getTeamMembers(teamId);
    }
}