package managers;

import database.Database;
import database.DbStatus;
import models.Match;
import models.SportType;
import models.Student;
import models.Team;
import java.util.UUID;

public class MatchmakingManager {

    private Database db;

    public MatchmakingManager(Database db) {
        this.db = db;
    }

    public DbStatus findSoloMatch(Student student, SportType sport) {
        Student opponent = db.findOpponentForMatch(student.getStudentId(), sport.name(), student.getEloPoint());
        
        if (opponent == null) {
            return DbStatus.DATA_NOT_FOUND;
        }
        
        String matchId = UUID.randomUUID().toString();
        return db.insertMatch(matchId, student.getStudentId(), opponent.getStudentId(), sport.name());
    }

    public DbStatus recordMatchResult(Match match, Team winnerTeam, int eloChange) {
        DbStatus status = db.updateMatchWinner(match.getMatchId(), winnerTeam.getTeamId());
        
        if (status != DbStatus.SUCCESS) {
            return status;
        }

        int team1AvgElo = calculateTeamAverageElo(match.getTeam1());
        int team2AvgElo = calculateTeamAverageElo(match.getTeam2());

        for (Student s : match.getTeam1().getMembers()) {
            boolean isWinner = match.getTeam1().equals(winnerTeam);
            DbStatus eloStatus = db.updateStudentElo(s.getStudentId(), isWinner, eloChange);
            if (eloStatus == DbStatus.SUCCESS) {
                s.updateElo(isWinner, team2AvgElo);
            }
        }

        for (Student s : match.getTeam2().getMembers()) {
            boolean isWinner = match.getTeam2().equals(winnerTeam);
            DbStatus eloStatus = db.updateStudentElo(s.getStudentId(), isWinner, eloChange);
            if (eloStatus == DbStatus.SUCCESS) {
                s.updateElo(isWinner, team1AvgElo);
            }
        }

        match.concludeMatch(winnerTeam, eloChange);
        return DbStatus.SUCCESS;
    }

    private int calculateTeamAverageElo(Team team) {
        if (team.getMembers().isEmpty()) return 1000;
        int totalElo = 0;
        for (Student s : team.getMembers()) {
            totalElo += s.getEloPoint();
        }
        return totalElo / team.getMembers().size();
    }
}