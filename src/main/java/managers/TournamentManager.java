package managers;

import database.Database;
import database.DbStatus;
import models.Student;
import models.Team;
import models.Tournament;
import models.Match;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TournamentManager {

    private Database db;
    private NotificationManager notifManager;

    private static final int MIN_TEAMS = 3;
    private static final int MAX_TEAMS = 32;

    public TournamentManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
    }

    public DbStatus createTournament(Tournament tournament) {
        if (tournament == null) return DbStatus.QUERY_ERROR;
        return db.insertTournament(
                tournament.getTournamentId(), 
                tournament.getTournamentName(), 
                tournament.getSportType().name(), 
                tournament.getStartDate(), 
                tournament.getEndDate(), 
                tournament.getMaxPlayersPerTeam(),
                tournament.isHasGe250(),
                tournament.getAccessCode(),
                tournament.getCampusLocation()
        );
    }

    public DbStatus registerTeamToTournament(Tournament tournament, Team team) {
        if (tournament == null || team == null || !tournament.isActive() || team.getMembers().size() > tournament.getMaxPlayersPerTeam()) {
            return DbStatus.QUERY_ERROR;
        }

        if (tournament.getParticipatingTeams().size() >= MAX_TEAMS) {
            return DbStatus.QUERY_ERROR;
        }

        if (LocalDate.now().isAfter(tournament.getStartDate()) || LocalDate.now().isEqual(tournament.getStartDate())) {
            return DbStatus.QUERY_ERROR;
        }

        for (Student s : team.getMembers()) {
            if (s == null || s.isBanned() || !s.isCanAttend()) {
                return DbStatus.QUERY_ERROR;
            }
        }

        for (Team t : tournament.getParticipatingTeams()) {
            if (t.getTeamId().equals(team.getTeamId())) {
                return DbStatus.QUERY_ERROR;
            }
        }

        DbStatus status = db.insertTournamentParticipant(tournament.getTournamentId(), team.getTeamId());
        if (status == DbStatus.SUCCESS) {
            tournament.getParticipatingTeams().add(team);
        }
        return status;
    }

    public DbStatus applyWithCode(Tournament tournament, Student student, String code) {
        if (tournament == null || student == null || code == null || !tournament.isActive() || !student.isCanAttend() || student.isBanned() || !tournament.getAccessCode().equals(code)) {
            return DbStatus.QUERY_ERROR;
        }

        if (tournament.getParticipatingTeams().size() >= MAX_TEAMS) {
            return DbStatus.QUERY_ERROR;
        }

        if (LocalDate.now().isAfter(tournament.getStartDate()) || LocalDate.now().isEqual(tournament.getStartDate())) {
            return DbStatus.QUERY_ERROR;
        }

        for (Team t : tournament.getParticipatingTeams()) {
            for(Student s : t.getMembers()) {
                if(s.getBilkentEmail().equals(student.getBilkentEmail())) {
                    return DbStatus.QUERY_ERROR;
                }
            }
        }

        String uniqueTeamId = UUID.randomUUID().toString();
        Team soloTeam = new Team(uniqueTeamId, student.getNickname(), "SOLO", 1, false, student);
        
        DbStatus teamInsertStatus = db.insertTeam(soloTeam.getTeamId(), soloTeam.getTeamName(), student.getBilkentEmail());
        if(teamInsertStatus == DbStatus.SUCCESS) {
            db.insertTeamMember(soloTeam.getTeamId(), student.getBilkentEmail(), soloTeam.getAccessCode());
            
            DbStatus status = db.insertTournamentParticipant(tournament.getTournamentId(), soloTeam.getTeamId());
            if (status == DbStatus.SUCCESS) {
                tournament.getParticipatingTeams().add(soloTeam);
                return DbStatus.SUCCESS;
            }
        }
        return DbStatus.QUERY_ERROR;
    }

    public DbStatus generateAndSaveFixture(Tournament tournament) {
        if (tournament == null) return DbStatus.QUERY_ERROR;

        List<Team> teams = tournament.getParticipatingTeams();

        if (teams.size() < MIN_TEAMS || teams.size() > MAX_TEAMS) {
            return DbStatus.QUERY_ERROR; 
        }

        DbStatus status = db.insertFixture(tournament.getTournamentId());
        if (status != DbStatus.SUCCESS) return status;

        List<Team> roundTeams = new ArrayList<>(teams);
        return generateRoundMatches(tournament, roundTeams, 1);
    }

    public DbStatus generateRoundMatches(Tournament tournament, List<Team> activeTeams, int roundNumber) {
        if (activeTeams == null || activeTeams.size() < 2) return DbStatus.QUERY_ERROR;

        Collections.shuffle(activeTeams);
        
        tournament.getTournamentFixture().setCurrentStage("Round " + roundNumber);

        Team byeTeam = null;
        if (activeTeams.size() % 2 != 0) {
            byeTeam = activeTeams.remove(0); 
            notifManager.sendToUser(byeTeam.getCaptain(), "Tournament Update", "Congratulations! You received a bye and advanced to the next round!");
        }

        for (int i = 0; i < activeTeams.size(); i += 2) {
            Team t1 = activeTeams.get(i);
            Team t2 = activeTeams.get(i + 1);
            String matchId = UUID.randomUUID().toString();
            
            DbStatus matchStatus = db.insertMatch(matchId, t1.getCaptain().getBilkentEmail(), t2.getCaptain().getBilkentEmail(), tournament.getSportType().name());
            
            if (matchStatus == DbStatus.SUCCESS) {
                Match m = new Match(matchId, LocalDateTime.now().plusDays(1), tournament.getSportType(), t1, t2);
                tournament.getTournamentFixture().getScheduledMatches().add(m);
                
                notifManager.sendToUser(t1.getCaptain(), "Tournament Fixture", "Round " + roundNumber + " match is set! Opponent: " + t2.getTeamName());
                notifManager.sendToUser(t2.getCaptain(), "Tournament Fixture", "Round " + roundNumber + " match is set! Opponent: " + t1.getTeamName());
            }
        }
        return DbStatus.SUCCESS;
    }

    public DbStatus withdrawTeam(Tournament tournament, Team team, Student requester) {
        if (tournament == null || team == null || requester == null || !requester.getBilkentEmail().equals(team.getCaptain().getBilkentEmail())) {
            return DbStatus.QUERY_ERROR;
        }
        
        if (LocalDate.now().isAfter(tournament.getStartDate()) || LocalDate.now().isEqual(tournament.getStartDate())) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.deleteTournamentParticipant(tournament.getTournamentId(), team.getTeamId());
        if(status == DbStatus.SUCCESS) {
            tournament.getParticipatingTeams().remove(team);
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

    public DbStatus cancelTournament(Tournament tournament) {
        if (tournament == null) return DbStatus.DATA_NOT_FOUND;

        DbStatus status = db.updateTournamentStatus(tournament.getTournamentId(), false);
        if (status == DbStatus.SUCCESS) {
            tournament.setActive(false);
            for(Team t : tournament.getParticipatingTeams()) {
                notifManager.sendToUser(t.getCaptain(), "Tournament Cancelled", "The tournament " + tournament.getTournamentName() + " has been cancelled.");
            }
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
}