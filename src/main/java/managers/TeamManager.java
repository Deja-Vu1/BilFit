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
        if (team == null || team.getCaptain() == null || team.getCaptain().isBanned() || !team.getCaptain().isCanAttend()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.insertTeam(team.getTeamId(), team.getTeamName(), team.getCaptain().getBilkentEmail());
        
        if (status == DbStatus.SUCCESS) {
            DbStatus memberStatus = db.insertTeamMember(team.getTeamId(), team.getCaptain().getBilkentEmail(), team.getAccessCode());
            if (memberStatus == DbStatus.SUCCESS && !team.getMembers().contains(team.getCaptain())) {
                team.getMembers().add(team.getCaptain());
            }
        }
        return status;
    }

    public DbStatus addMemberToTeam(Team team, Student student, String inputCode) {
        if (team == null || student == null || inputCode == null || student.isBanned() || !student.isCanAttend()) {
            return DbStatus.QUERY_ERROR;
        }
        
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
        if (team == null || student == null || !team.getMembers().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }

        if (student.getBilkentEmail().equals(team.getCaptain().getBilkentEmail())) {
            if (team.getMembers().size() > 1) {
                Student newCaptain = null;
                for (Student s : team.getMembers()) {
                    if (!s.getBilkentEmail().equals(student.getBilkentEmail()) && !s.isBanned()) {
                        newCaptain = s;
                        break;
                    }
                }
                if (newCaptain != null) {
                    DbStatus transferStatus = transferCaptaincy(team, student, newCaptain);
                    if (transferStatus != DbStatus.SUCCESS) {
                        return transferStatus;
                    }
                } else {
                    return disbandTeam(team, student);
                }
            } else {
                return disbandTeam(team, student);
            }
        }

        DbStatus status = db.deleteTeamMember(team.getTeamId(), student.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            team.getMembers().remove(student);
        }
        return status;
    }

    public DbStatus transferCaptaincy(Team team, Student oldCaptain, Student newCaptain) {
        if (team == null || oldCaptain == null || newCaptain == null || newCaptain.isBanned()) {
            return DbStatus.QUERY_ERROR;
        }
        
        if (!team.getCaptain().getBilkentEmail().equals(oldCaptain.getBilkentEmail()) || !team.getMembers().contains(newCaptain)) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateTeamCaptain(team.getTeamId(), newCaptain.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            team.setCaptain(newCaptain);
        }
        return status;
    }

    public DbStatus disbandTeam(Team team, Student requester) {
        if (team == null || requester == null || !requester.getBilkentEmail().equals(team.getCaptain().getBilkentEmail())) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.deleteTeam(team.getTeamId());
        if (status == DbStatus.SUCCESS) {
            team.getMembers().clear();
        }
        return status;
    }
}