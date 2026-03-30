package managers;

import database.Database;
import database.DbStatus;
import models.Match;
import models.Student;
import models.Team;

public class MatchmakingManager {

    private Database db;

    public MatchmakingManager(Database db) {
        this.db = db;
    }

    public DbStatus recordMatchResult(Match match, Team winnerTeam, int eloChange) {
        DbStatus status = db.updateMatchWinner(match.getMatchId(), winnerTeam.getTeamId());
        
        if (status != DbStatus.SUCCESS) {
            return status;
        }

        for (Student s : match.getTeam1().getMembers()) {
            boolean isWinner = match.getTeam1().equals(winnerTeam);
            DbStatus eloStatus = db.updateStudentElo(s.getStudentId(), isWinner, eloChange);
            if (eloStatus == DbStatus.SUCCESS) {
                s.updateElo(isWinner, 1000);
            }
        }

        for (Student s : match.getTeam2().getMembers()) {
            boolean isWinner = match.getTeam2().equals(winnerTeam);
            DbStatus eloStatus = db.updateStudentElo(s.getStudentId(), isWinner, eloChange);
            if (eloStatus == DbStatus.SUCCESS) {
                s.updateElo(isWinner, 1000);
            }
        }

        match.concludeMatch(winnerTeam, eloChange);
        return DbStatus.SUCCESS;
    }
}