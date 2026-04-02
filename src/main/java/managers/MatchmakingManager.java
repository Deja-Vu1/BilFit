package managers;

import database.Database;
import database.DbStatus;
import models.Match;
import models.SportType;
import models.Student;
import models.Team;
import java.time.LocalDateTime;
import java.util.UUID;

public class MatchmakingManager {

    private Database db;

    public MatchmakingManager(Database db) {
        this.db = db;
    }

    public Match findSoloMatch(Student student, SportType sport) {
        if (!student.isCanAttend()) {
            return null; 
        }

        Student opponent = db.findOpponentForMatch(student.getBilkentEmail(), sport.name(), student.getEloPoint());
        
        if (opponent == null) {
            return null;
        }
        
        String matchId = UUID.randomUUID().toString();
        DbStatus status = db.insertMatch(matchId, student.getBilkentEmail(), opponent.getBilkentEmail(), sport.name());
        
        if (status == DbStatus.SUCCESS) {
            Team team1 = new Team(student.getStudentId() + "_T", student.getNickname(), "SOLO", 1, false, student);
            Team team2 = new Team(opponent.getStudentId() + "_T", opponent.getNickname(), "SOLO", 1, false, opponent);
            return new Match(matchId, LocalDateTime.now(), sport, team1, team2);
        }
        
        return null;
    }

    public DbStatus recordMatchResult(Match match, Team winnerTeam, int eloChange) {
        DbStatus status = db.updateMatchWinner(match.getMatchId(), winnerTeam.getTeamId());
        
        if (status != DbStatus.SUCCESS) {
            return status;
        }

        for (Student s : match.getTeam1().getMembers()) {
            boolean isWinner = match.getTeam1().equals(winnerTeam);
            int newElo = isWinner ? s.getEloPoint() + eloChange : s.getEloPoint() - eloChange;
            newElo = Math.max(0, newElo);
            
            DbStatus eloStatus = db.updateStudentElo(s.getBilkentEmail(), newElo);
            if (eloStatus == DbStatus.SUCCESS) {
                s.setEloPoint(newElo); 
            }
        }

        for (Student s : match.getTeam2().getMembers()) {
            boolean isWinner = match.getTeam2().equals(winnerTeam);
            int newElo = isWinner ? s.getEloPoint() + eloChange : s.getEloPoint() - eloChange;
            newElo = Math.max(0, newElo);

            DbStatus eloStatus = db.updateStudentElo(s.getBilkentEmail(), newElo);
            if (eloStatus == DbStatus.SUCCESS) {
                s.setEloPoint(newElo); 
            }
        }

        match.setWinner(winnerTeam);
        match.setPointChange(eloChange);
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