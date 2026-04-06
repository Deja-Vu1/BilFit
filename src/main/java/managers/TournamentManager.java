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
        
        return db.insertTournament(
                tournament.getTournamentName(), 
                tournament.getSportType().name(), 
                tournament.getStartDate(), 
                tournament.getEndDate(), 
                tournament.getMaxPlayersPerTeam(),
                tournament.isHasGe250(),
                tournament.getCampusLocation()
        );
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

    // İŞTE GÜNCELLENEN KISIM BURASI: Senin yeni Database metoduna %100 uyumlu hale getirildi!
    public DbStatus joinTeamWithCode(Student student, String code) {
        if (student == null || code == null) return DbStatus.QUERY_ERROR;
        // Artık sadece Student ve Code nesnelerini senin yeni beTeamMember metoduna gönderiyor.
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

    public void fillTournamentFixtures(Tournament tournament) {
        if (tournament == null || tournament.getTournamentId() == null) return;
        
        java.util.List<models.Match> matches = db.getAllTournamentMatches(tournament.getTournamentId());
        
        if (tournament.getTournamentFixture() == null) {
            tournament.setTournamentFixture(new models.Fixture());
        }
        
        tournament.getTournamentFixture().setScheduledMatches(matches);
    }

    public DbStatus generateFixtures(Tournament tournament, int pointChange) {
        if (tournament == null || tournament.getTournamentId() == null) return DbStatus.QUERY_ERROR;

        java.util.List<Team> teams = db.getTournamentTeams(tournament.getTournamentId());
        if (teams == null || teams.size() < 2) {
            return DbStatus.QUERY_ERROR;
        }

        java.util.Collections.shuffle(teams);

        java.time.OffsetDateTime currentMatchTime = java.time.OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        for (int i = 0; i < teams.size(); i += 2) {
            if (i + 1 < teams.size()) {
                Team team1 = teams.get(i);
                Team team2 = teams.get(i + 1);
                
                DbStatus status = db.insertMatch(tournament, team1, team2, currentMatchTime, pointChange);
                if (status != DbStatus.SUCCESS) {
                    return status; 
                }
                currentMatchTime = currentMatchTime.plusHours(2);
            }
        }
        
        return DbStatus.SUCCESS;
    }
    public Tournament getTournamentByTeamId(String teamId) {
        if (teamId == null) return null;
        return db.getTournamentByTeamId(teamId);
    }
}