package java.models;

import java.time.LocalDateTime;

public class Match {


    private String matchId;
    private LocalDateTime date;
    private SportType sportType;
    private int pointChange;
    private boolean isWin;
    private String opponentName;
    private Team team1;
    private Team team2;
    private Team winner;

    public Match(String matchId, LocalDateTime date, SportType sportType, Team team1, Team team2) {
        
        this.matchId = matchId;
        this.date = date;
        this.sportType = sportType;
        this.team1 = team1;
        this.team2 = team2;
        this.winner = null; 


    }

    public void concludeMatch(Team winnerTeam, int pointChange) {
        this.winner = winnerTeam;
        this.pointChange = pointChange;
        
        System.out.println("Match concluded! Winner: " + winnerTeam.getTeamName() + " | Point Change: " + pointChange);
    }

    public Team getTeam1() { return team1; }
    public Team getTeam2() { return team2; }
    public Team getWinner() { return winner; }
}