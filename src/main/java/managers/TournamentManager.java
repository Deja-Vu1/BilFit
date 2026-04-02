package managers;

import database.Database;
import database.DbStatus;
import models.Student;
import models.Team;
import models.Tournament;
import models.Match;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class TournamentManager {

    private Database db;

    public TournamentManager(Database db) {
        this.db = db;
    }

    public DbStatus createTournament(Tournament tournament) {
        if (tournament == null) return DbStatus.QUERY_ERROR;
        // Eksik alanlar (ge250, accessCode, campusLocation) eklendi
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
        if (tournament == null || student == null || code == null || !tournament.isActive() || !student.isCanAttend() || !tournament.getAccessCode().equals(code)) {
            return DbStatus.QUERY_ERROR;
        }

        for (Team t : tournament.getParticipatingTeams()) {
            if (t.getMembers().contains(student)) {
                return DbStatus.QUERY_ERROR;
            }
        }

        // DB/Memory tutarsızlığı giderildi: Takımı önce oluşturup DB'ye TeamId atıyoruz
        Team soloTeam = new Team(student.getStudentId() + "_T", student.getNickname(), "SOLO", 1, false, student);
        DbStatus status = db.insertTournamentParticipant(tournament.getTournamentId(), soloTeam.getTeamId());
        
        if (status == DbStatus.SUCCESS) {
            tournament.getParticipatingTeams().add(soloTeam);
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
        }
        return status;
    }

    public DbStatus generateAndSaveFixture(Tournament tournament) {
        if (tournament == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.insertFixture(tournament.getTournamentId());
        
        if (status == DbStatus.SUCCESS) {
            tournament.getTournamentFixture().setCurrentStage("Group Stage Prepared");
            if (tournament.getParticipatingTeams().size() >= 2) {
                Team t1 = tournament.getParticipatingTeams().get(0);
                Team t2 = tournament.getParticipatingTeams().get(1);
                Match openingMatch = new Match(UUID.randomUUID().toString(), LocalDateTime.now().plusDays(1), tournament.getSportType(), t1, t2);
                tournament.getTournamentFixture().getScheduledMatches().add(openingMatch);
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