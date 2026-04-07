package models;

import java.time.LocalDateTime;

public class Match {
    private String matchId;
    private LocalDateTime date;
    private SportType sportType;
    private int pointChange;
    private Team team1;
    private Team team2;
    private Team winner;
    private boolean is_concluded;
    private int currentStage;
    
    public Match(String matchId, LocalDateTime date, SportType sportType, Team team1, Team team2) {
        this.matchId = matchId;
        this.date = date;
        this.sportType = sportType;
        this.team1 = team1;
        this.team2 = team2;
        this.winner = null;
        this.currentStage = 0;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public SportType getSportType() { return sportType; }
    public void setSportType(SportType sportType) { this.sportType = sportType; }
    public int getPointChange() { return pointChange; }
    public void setPointChange(int pointChange) { this.pointChange = pointChange; }
    public Team getTeam1() { return team1; }
    public void setTeam1(Team team1) { this.team1 = team1; }
    public Team getTeam2() { return team2; }
    public void setTeam2(Team team2) { this.team2 = team2; }
    public Team getWinner() { return winner; }
    public void setWinner(Team winner) { this.winner = winner; }
    public boolean is_concluded() { return is_concluded; }
    public void set_concluded(boolean is_concluded) { this.is_concluded = is_concluded; }
    public int getCurrentStage() { return currentStage; }
    public void setCurrentStage(int currentStage) { this.currentStage = currentStage; }
}