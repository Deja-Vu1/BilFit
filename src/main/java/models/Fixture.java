package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
public class Fixture {

    private String fixtureId;
    private List<Match> scheduledMatches;
    private String currentStage;

    public Fixture(String fixtureId) {
        this.fixtureId = fixtureId;
        this.scheduledMatches = new ArrayList<>();
        this.currentStage = "Group Stage"; // Default starting stage
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
        System.out.println("Teams have been shuffled automatically for fair matchmaking.");

    }

    public void createBracket(List<Team> teams, LocalDate tournamentStartDate, LocalDate tournamentEndDate, LocalTime dailyStartTime, LocalTime dailyEndTime, SportType sportType) {
        this.scheduledMatches.clear();
        int totalTeams = teams.size();
        int totalMatches = totalTeams / 2; 
        
        long totalDays = ChronoUnit.DAYS.between(tournamentStartDate, tournamentEndDate) + 1;
        if (totalDays <= 0) totalDays = 1; 
        
        int matchesPerDay = (int) Math.ceil((double) totalMatches / totalDays);
        
        // İki maç arası ortalama ne kadar süre olmalı? (Adminin verdiği saat aralığını, o günkü maç sayısına bölüyoruz)
        long totalActiveMinutesPerDay = ChronoUnit.MINUTES.between(dailyStartTime, dailyEndTime);
        long minutesBetweenMatches = matchesPerDay > 1 ? totalActiveMinutesPerDay / matchesPerDay : 90; 
        
        // Eğer hesaplanan süre 90 dakikadan (1.5 saat) kısa çıkarsa, gerçekçilik için minimum 90 dk yap
        if (minutesBetweenMatches < 90) minutesBetweenMatches = 90;

        int matchCounter = 1;
        int matchesScheduledToday = 0;
        int currentDayOffset = 0;
        
        // İLK MAÇ: Kodun içine gömülü bir saat DEĞİL, adminin parametre olarak gönderdiği saat!
        LocalDateTime currentMatchTime = tournamentStartDate.atTime(dailyStartTime); 
        
        for (int i = 0; i < totalTeams - 1; i += 2) {
            Team team1 = teams.get(i);
            Team team2 = teams.get(i + 1);
            
            String matchId = this.fixtureId + "_M" + matchCounter;
            Match match = new Match(matchId, currentMatchTime, sportType, team1, team2);
            this.scheduledMatches.add(match);
            
            matchesScheduledToday++;
            
            // Eğer o günkü maç kotası dolduysa VEYA bir sonraki maç adminin belirlediği bitiş saatini aşıyorsa: ERTESİ GÜNE GEÇ
            if (matchesScheduledToday >= matchesPerDay || currentMatchTime.plusMinutes(minutesBetweenMatches).toLocalTime().isAfter(dailyEndTime)) {
                currentDayOffset++;
                // Ertesi günün ilk maçı yine adminin belirlediği saatte başlar
                currentMatchTime = tournamentStartDate.plusDays(currentDayOffset).atTime(dailyStartTime); 
                matchesScheduledToday = 0;
            } else {
                // Aynı gün içindeyse, hesaplanan dinamik dakika kadar ileri at (Örn: 105 dakika sonra diğer maç)
                currentMatchTime = currentMatchTime.plusMinutes(minutesBetweenMatches); 
            }
            
            matchCounter++;
        }

        if (totalTeams % 2 != 0) {
            Team byeTeam = teams.get(totalTeams - 1);
            System.out.println("Info: " + byeTeam.getTeamName() + " gets a bye for this round.");
        }

        this.currentStage = "Round 1";
        System.out.println("Bracket generated: " + scheduledMatches.size() + " matches.");
    }

    public void updateBracket(Match match) {
        if (match.getWinner() != null) {
            System.out.println("Bracket updated. Team goes one step further: " + match.getWinner().getTeamName());
        }
    }

    
    public Team getWinner() {
        if (!scheduledMatches.isEmpty()) {
            return scheduledMatches.get(scheduledMatches.size() - 1).getWinner();
        }
        return null;
    }
}