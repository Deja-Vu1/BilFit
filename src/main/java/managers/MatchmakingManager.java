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
    private NotificationManager notifManager;

    public MatchmakingManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
    }

    public Match findSoloMatch(Student student, SportType sport) {
        if (student == null || sport == null || !student.isCanAttend() || !student.isEloMatchingEnabled() || student.isBanned()) {
            return null; 
        }

        Student opponent = db.findOpponentForMatch(student.getBilkentEmail(), sport.name(), student.getEloPoint());
        
        if (opponent == null) return null;
        
        String matchId = UUID.randomUUID().toString();
        DbStatus status = db.insertMatch(matchId, student.getBilkentEmail(), opponent.getBilkentEmail(), sport.name());
        
        if (status == DbStatus.SUCCESS) {
            Team team1 = new Team(student.getStudentId() + "_T", student.getNickname(), "SOLO", 1, false, student);
            Team team2 = new Team(opponent.getStudentId() + "_T", opponent.getNickname(), "SOLO", 1, false, opponent);
            Match match = new Match(matchId, LocalDateTime.now(), sport, team1, team2);

            notifManager.sendToUser(student, "Match Found", "A solo match has been found for you!");
            notifManager.sendToUser(opponent, "Match Found", "A solo match has been found for you!");

            return match;
        }
        return null;
    }

    public Match findTeamMatch(Team team, SportType sport) {
        if (team == null || sport == null || team.getMembers().isEmpty()) return null;

        for (Student s : team.getMembers()) {
            if (s == null || !s.isCanAttend() || s.isBanned()) return null;
        }

        int avgElo = calculateTeamAverageElo(team);
        Team opponentTeam = db.findOpponentTeamForMatch(team.getCaptain().getBilkentEmail(), sport.name(), avgElo);

        if (opponentTeam == null) return null;

        String matchId = UUID.randomUUID().toString();
        DbStatus status = db.insertMatch(matchId, team.getCaptain().getBilkentEmail(), opponentTeam.getCaptain().getBilkentEmail(), sport.name());

        if (status == DbStatus.SUCCESS) {
            Match match = new Match(matchId, LocalDateTime.now(), sport, team, opponentTeam);

            notifManager.sendToUser(team.getCaptain(), "Team Match Found", "An opponent team has been found!");
            notifManager.sendToUser(opponentTeam.getCaptain(), "Team Match Found", "An opponent team has been found!");

            return match;
        }
        return null;
    }

    public DbStatus recordMatchResult(Match match, Team winnerTeam) {
        if (match == null) return DbStatus.QUERY_ERROR;

        if (winnerTeam == null) {
            DbStatus drawStatus = db.updateMatchDraw(match.getMatchId());
            if (drawStatus == DbStatus.SUCCESS) {
                match.setPointChange(0);
                updateTeamStats(match.getTeam1(), false, match);
                updateTeamStats(match.getTeam2(), false, match);
            }
            return drawStatus;
        }

        DbStatus status = db.updateMatchWinner(match.getMatchId(), winnerTeam.getTeamId());
        
        if (status == DbStatus.SUCCESS) {
            int team1Change = calculateTrueEloDelta(match.getTeam1(), match.getTeam2(), winnerTeam.getTeamId().equals(match.getTeam1().getTeamId()));
            int team2Change = calculateTrueEloDelta(match.getTeam2(), match.getTeam1(), winnerTeam.getTeamId().equals(match.getTeam2().getTeamId()));

            applyEloToTeam(match.getTeam1(), team1Change);
            applyEloToTeam(match.getTeam2(), team2Change);

            match.setWinner(winnerTeam);
            match.setPointChange(Math.abs(team1Change));

            boolean isTeam1Winner = match.getTeam1().getTeamId().equals(winnerTeam.getTeamId());
            updateTeamStats(match.getTeam1(), isTeam1Winner, match);
            updateTeamStats(match.getTeam2(), !isTeam1Winner, match);
        }
        return status;
    }

    private void updateTeamStats(Team team, boolean isWinner, Match match) {
        for (Student s : team.getMembers()) {
            if (s == null) continue;
            
            s.setMatchesPlayed(s.getMatchesPlayed() + 1);
            if (isWinner) {
                s.setMatchesWon(s.getMatchesWon() + 1);
            }
            s.setWinRate((double) s.getMatchesWon() / s.getMatchesPlayed());
            s.getMatchHistory().add(match);

            db.updateStudentStats(s.getBilkentEmail(), s.getMatchesPlayed(), s.getMatchesWon(), s.getWinRate());
        }
    }

    private int calculateTrueEloDelta(Team targetTeam, Team opponentTeam, boolean isWinner) {
        int rTarget = calculateTeamAverageElo(targetTeam);
        int rOpponent = calculateTeamAverageElo(opponentTeam);
        
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (rOpponent - rTarget) / 400.0));
        double actualScore = isWinner ? 1.0 : 0.0;
        
        int kFactor = 32;
        if (rTarget >= 2000) kFactor = 16;
        else if (rTarget >= 2400) kFactor = 10;
        
        return (int) Math.round(kFactor * (actualScore - expectedScore));
    }

    private void applyEloToTeam(Team team, int deltaElo) {
        for (Student s : team.getMembers()) {
            if (s == null) continue;
            int newElo = Math.max(0, s.getEloPoint() + deltaElo);
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