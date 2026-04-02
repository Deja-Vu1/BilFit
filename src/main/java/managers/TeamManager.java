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
        DbStatus status = db.insertTeam(team.getTeamId(), team.getTeamName(), team.getCaptain().getBilkentEmail());
        
        if (status == DbStatus.SUCCESS) {
            db.insertTeamMember(team.getTeamId(), team.getCaptain().getBilkentEmail(), team.getAccessCode());
        }
        
        return status;
    }

    public DbStatus addMemberToTeam(Team team, Student student, String inputCode) {
        if (team.getMembers().size() >= team.getMaxCapacity() || !team.getAccessCode().equals(inputCode) || team.getMembers().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.insertTeamMember(team.getTeamId(), student.getBilkentEmail(), inputCode);
        
        if (status == DbStatus.SUCCESS) {
            team.getMembers().add(student);
        }
        
        return status;
    }

    public DbStatus removeMemberFromTeam(Team team, Student student) {
        if (student.equals(team.getCaptain())) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.deleteTeamMember(team.getTeamId(), student.getBilkentEmail());
        
        if (status == DbStatus.SUCCESS) {
            team.getMembers().remove(student);
        }
        
        return status;
    }

    public DbStatus disbandTeam(Team team, Student requester) {
        if (!requester.equals(team.getCaptain())) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.deleteTeam(team.getTeamId());
        
        if (status == DbStatus.SUCCESS) {
            team.getMembers().clear();
        }
        
        return status;
    }
}