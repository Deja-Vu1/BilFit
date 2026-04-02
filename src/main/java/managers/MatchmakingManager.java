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
        if (student == null || sport == null || !student.isCanAttend()) {
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

    public Match findTeamMatch(Team team, SportType sport) {
        if (team == null || sport == null || team.getMembers().isEmpty()) return null;

        for (Student s : team.getMembers()) {
            if (s == null || !s.isCanAttend()) return null;
        }

        int avgElo = calculateTeamAverageElo(team);
        // Sadece kaptanı değil, rakip takımın tamamını DB'den çekiyoruz
        Team opponentTeam = db.findOpponentTeamForMatch(team.getCaptain().getBilkentEmail(), sport.name(), avgElo);

        if (opponentTeam == null) return null;

        String matchId = UUID.randomUUID().toString();
        DbStatus status = db.insertMatch(matchId, team.getCaptain().getBilkentEmail(), opponentTeam.getCaptain().getBilkentEmail(), sport.name());

        if (status == DbStatus.SUCCESS) {
            return new Match(matchId, LocalDateTime.now(), sport, team, opponentTeam);
        }
        return null;
    }

    public DbStatus recordMatchResult(Match match, Team winnerTeam, int eloChange) {
        if (match == null || winnerTeam == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateMatchWinner(match.getMatchId(), winnerTeam.getTeamId());
        
        if (status != DbStatus.SUCCESS) {
            return status;
        }

        updateTeamElo(match.getTeam1(), winnerTeam, eloChange);
        updateTeamElo(match.getTeam2(), winnerTeam, eloChange);

        match.setWinner(winnerTeam);
        match.setPointChange(eloChange);
        return DbStatus.SUCCESS;
    }

    private void updateTeamElo(Team team, Team winnerTeam, int eloChange) {
        boolean isWinner = team.getTeamId().equals(winnerTeam.getTeamId());
        for (Student s : team.getMembers()) {
            if (s == null) continue;
            int newElo = isWinner ? s.getEloPoint() + eloChange : s.getEloPoint() - eloChange;
            newElo = Math.max(0, newElo);
            
            DbStatus eloStatus = db.updateStudentElo(s.getBilkentEmail(), newElo);
            if (eloStatus == DbStatus.SUCCESS) {
                s.setEloPoint(newElo); 
            }
        }
    }

    private int calculateTeamAverageElo(Team team) {
        if (team.getMembers().isEmpty()) return 1000;
        int totalElo = 0;
        for (Student s : team.getMembers()) {
            if (s != null) totalElo += s.getEloPoint();
        }
        return totalElo / team.getMembers().size();
    }
}