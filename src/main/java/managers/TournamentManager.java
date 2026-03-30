package managers;

import database.Database;
import database.DbStatus;
import models.Student;
import models.Team;
import models.Tournament;
import java.time.LocalDate;

public class TournamentManager {

    private Database db;

    public TournamentManager(Database db) {
        this.db = db;
    }

    public DbStatus createTournament(Tournament tournament) {
        return db.insertTournament(tournament.getTournamentId(), tournament.getTournamentName(), tournament.getSportType().name(), tournament.getStartDate(), tournament.getEndDate(), tournament.getMaxPlayersPerTeam());
    }

    public DbStatus registerTeamToTournament(Tournament tournament, Team team) {
        DbStatus status = db.insertTournamentParticipant(tournament.getTournamentId(), team.getTeamId());
        
        if (status == DbStatus.SUCCESS) {
            tournament.applyAsTeam(team);
        }
        return status;
    }

    public DbStatus applyWithCode(Tournament tournament, Student student, String code) {
        if (!tournament.getAccessCode().equals(code)) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.insertTournamentParticipant(tournament.getTournamentId(), student.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            tournament.applyWithCode(student, code);
        }
        return status;
    }

    public DbStatus editDetails(Tournament tournament, String newName, int newMaxPlayers) {
        DbStatus status = db.updateTournamentDetails(tournament.getTournamentId(), newName, newMaxPlayers);
        
        if (status == DbStatus.SUCCESS) {
            tournament.editDetails(newName, newMaxPlayers, tournament.getCampusLocation());
        }
        return status;
    }

    public DbStatus cancelTournament(Tournament tournament) {
        if (tournament == null) {
            return DbStatus.DATA_NOT_FOUND;
        }

        DbStatus status = db.updateTournamentStatus(tournament.getTournamentId(), false);
        
        if (status == DbStatus.SUCCESS) {
            tournament.cancelTournament();
        }
        return status;
    }

    public DbStatus generateAndSaveFixture(Tournament tournament) {
        DbStatus status = db.insertFixture(tournament.getTournamentId());
        
        if (status == DbStatus.SUCCESS) {
            tournament.generateFixture();
        }
        return status;
    }

    public DbStatus updateTournamentSchedule(Tournament tournament, LocalDate newStart, LocalDate newEnd) {
        DbStatus status = db.updateTournamentDates(tournament.getTournamentId(), newStart, newEnd);
        
        if (status == DbStatus.SUCCESS) {
            tournament.updateSchedule(newStart, newEnd);
        }
        return status;
    }
}