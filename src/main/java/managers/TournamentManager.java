package managers;

import java.time.LocalDate;
import java.util.List;

import database.Database;
import database.DbStatus;
import models.Student;
import models.Team;
import models.Tournament;

public class TournamentManager {

    private Database db;

    public TournamentManager(Database db) {
        this.db = db;
    }

    public DbStatus createTournament(Tournament tournament) {
        if (tournament == null) return DbStatus.QUERY_ERROR;
        DbStatus status = db.insertTournament(
                tournament.getTournamentName(), 
                tournament.getSportType().name(), 
                tournament.getStartDate(), 
                tournament.getEndDate(), 
                tournament.getMaxPlayersPerTeam(),
                tournament.isHasGe250(),
                tournament.getCampusLocation()
        );
        if (status == DbStatus.SUCCESS) {
            db.insertNotification("BROADCAST", 
                            "New Tournament Added", 
                            "A new tournament named '" + tournament.getTournamentName() + "' has been added.");
        }
        return status;
    }

    public DbStatus editDetails(Tournament tournament, String newName, int newMaxPlayers) {
        if (tournament == null || newName == null || newMaxPlayers <= 0) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateTournamentDetails(tournament.getTournamentId(), newName, newMaxPlayers);
        if (status == DbStatus.SUCCESS) {
            tournament.setTournamentName(newName);
            tournament.setMaxPlayersPerTeam(newMaxPlayers);
        }
        return status;
    }

    public DbStatus updateTournamentStatus(Tournament tournament, boolean isActive) {
        if (tournament == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateTournamentStatus(tournament.getTournamentId(), isActive);
        if (status == DbStatus.SUCCESS) {
            tournament.setActive(isActive);
        }
        return status;
    }

    public DbStatus updateTournamentSchedule(Tournament tournament, LocalDate newStart, LocalDate newEnd) {
        if (tournament == null || newStart == null || newEnd == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateTournamentDates(tournament.getTournamentId(), newStart, newEnd);
        if (status == DbStatus.SUCCESS) {
            tournament.setStartDate(newStart);
            tournament.setEndDate(newEnd);
        }
        return status;
    }

    public DbStatus createTeamAndJoinTournament(Student creator, Tournament tournament, String teamName) {
        if (creator == null || tournament == null || teamName == null) return DbStatus.QUERY_ERROR;
        
        if (LocalDate.now().isAfter(tournament.getStartDate()) || LocalDate.now().isEqual(tournament.getStartDate())) {
            return DbStatus.QUERY_ERROR;
        }

        return db.insertTeam(creator, tournament, teamName);
    }

    
    public DbStatus joinTeamWithCode(Student student, String code) {
        if (student == null || code == null) return DbStatus.QUERY_ERROR;
        
        return db.beTeamMember(student, code);
    }

    public DbStatus leaveTeam(String teamId, Student student) {
        if (teamId == null || student == null) return DbStatus.QUERY_ERROR;
        return db.deleteTeamMember(teamId, student.getBilkentEmail());
    }

    public DbStatus withdrawTeam(String teamId) {
        if (teamId == null) return DbStatus.QUERY_ERROR;
        return db.deleteTeam(teamId);
    }

    public DbStatus sendTeamInvite(String teamId, Student receiver) {
        if (teamId == null || receiver == null) return DbStatus.QUERY_ERROR;
        return db.sendTeamInvite(teamId, receiver.getBilkentEmail());
    }

    public DbStatus acceptTeamInvite(String teamId, Student student) {
        if (teamId == null || student == null) return DbStatus.QUERY_ERROR;
        return db.acceptTeamInvite(teamId, student.getBilkentEmail());
    }

    public DbStatus rejectTeamInvite(String teamId, Student student) {
        if (teamId == null || student == null) return DbStatus.QUERY_ERROR;
        return db.rejectTeamInvite(teamId, student.getBilkentEmail());
    }

    public List<Tournament> getAllActiveTournaments() {
        return db.getAllActiveTournaments();
    }

    public List<Tournament> getUserTournaments(Student student) {
        if (student == null) return new java.util.ArrayList<>();
        return db.getUserTournaments(student);
    }

    public List<Team> getTeamIncomingRequests(String teamId, Student student) {
        if (student == null) return new java.util.ArrayList<>();
        return db.getTeamIncomingRequests(teamId, student);
    }

    public List<Student> getTeamOutgoingRequests(String teamId, Student student) {
        if (student == null) return new java.util.ArrayList<>();
        return db.getTeamOutgoingRequests(teamId, student);
    }

    public List<Team> getMyTeams(Student student) {
        if (student == null) return new java.util.ArrayList<>();
        return db.getMyTeams(student);
    }

    public List<Team> getTournamentTeams(String tournamentId) {
        if (tournamentId == null) return new java.util.ArrayList<>();
        return db.getTournamentTeams(tournamentId);
    }
    public void fillTournamentFixtures(models.Tournament tournament) {
        if (tournament == null) {
            return;
        }
    }
}