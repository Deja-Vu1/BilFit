package managers;

import database.Database;
import database.DbStatus;
import models.Student;
import models.Team;

public class TeamManager {

    private Database db;

    public TeamManager(Database db) {
        this.db = db;
    }

    public DbStatus createTeam(Team team) {
        return db.insertTeam(team.getTeamName(), team.getCaptain().getStudentId());
    }

    public DbStatus addMemberToTeam(Team team, Student student, String inputCode) {
        if (team.isFull()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.insertTeamMember(team.getTeamName(), student.getStudentId(), inputCode);
        
        if (status == DbStatus.SUCCESS) {
            team.addMember(student, inputCode);
        }
        
        return status;
    }

    public DbStatus removeMemberFromTeam(Team team, Student student) {
        if (student.equals(team.getCaptain())) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.deleteTeamMember(team.getTeamName(), student.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            team.removeMember(student);
        }
        
        return status;
    }
}