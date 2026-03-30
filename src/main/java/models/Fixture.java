package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Fixture {

    private String fixtureId;
    private List<Match> scheduledMatches;
    private String currentStage;

    public Fixture(String fixtureId) {
        this.fixtureId = fixtureId;
        this.scheduledMatches = new ArrayList<>();
        this.currentStage = "Group Stage";
    }

    public String getFixtureId() {
        return fixtureId;
    }

    public void setFixtureId(String fixtureId) {
        this.fixtureId = fixtureId;
    }

    public List<Match> getScheduledMatches() {
        return scheduledMatches;
    }

    public void setScheduledMatches(List<Match> scheduledMatches) {
        this.scheduledMatches = scheduledMatches;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public void shuffleTeams(List<Team> teams) {
        Collections.shuffle(teams);
    }

    public void createBracket(List<Team> teams, LocalDate startDate, LocalDate endDate, SportType sportType) {
        this.scheduledMatches.clear();
        int totalTeams = teams.size();
        
        LocalTime dailyStartTime = LocalTime.of(18, 0);
        LocalTime dailyEndTime = LocalTime.of(22, 0);
        long minutesPerMatch = 90;

        int matchCounter = 1;
        int currentDayOffset = 0;
        
        LocalDateTime currentMatchTime = startDate.atTime(dailyStartTime);
        
        for (int i = 0; i < totalTeams - 1; i += 2) {
            Team team1 = teams.get(i);
            Team team2 = teams.get(i + 1);
            
            String matchId = this.fixtureId + "_M" + matchCounter;
            Match match = new Match(matchId, currentMatchTime, sportType, team1, team2);
            this.scheduledMatches.add(match);
            
            currentMatchTime = currentMatchTime.plusMinutes(minutesPerMatch);
            
            if (currentMatchTime.plusMinutes(minutesPerMatch).toLocalTime().isAfter(dailyEndTime) || 
                currentMatchTime.toLocalTime().isBefore(dailyStartTime)) {
                currentDayOffset++;
                currentMatchTime = startDate.plusDays(currentDayOffset).atTime(dailyStartTime);
            }
            
            matchCounter++;
        }

        this.currentStage = "Round 1";
    }

    public void updateBracket(Match match) {
        if (match.getWinner() != null) {
            this.currentStage = "Updated";
        }
    }

    public Team getWinner() {
        if (!scheduledMatches.isEmpty()) {
            return scheduledMatches.get(scheduledMatches.size() - 1).getWinner();
        }
        return null;
    }
}