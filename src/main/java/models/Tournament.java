package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Tournament {

    private String tournamentId;
    private String tournamentName;
    private SportType sportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxPlayersPerTeam;
    private boolean hasGe250;
    private String accessCode;
    private boolean isActive;
    private String campusLocation;
    private List<Team> participatingTeams;
    private Fixture tournamentFixture;

    public Tournament(String tournamentId, String tournamentName, SportType sportType, LocalDate startDate, LocalDate endDate, int maxPlayersPerTeam, boolean hasGe250, String accessCode, String campusLocation) {
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        this.sportType = sportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxPlayersPerTeam = maxPlayersPerTeam;
        this.hasGe250 = hasGe250;
        this.accessCode = accessCode;
        this.campusLocation = campusLocation;
        this.isActive = true;
        this.participatingTeams = new ArrayList<>();
        
        
        //Create a fixture automatically when a tournament is created 
        this.tournamentFixture = new Fixture(tournamentId + "_FIXTURE");

    }

    public boolean applyAsTeam(Team team) {
        if (!participatingTeams.contains(team) && this.isActive) {

            participatingTeams.add(team);
            System.out.println("Team " + team.getTeamName() + " applied to the tournament successfully.");

            return true;
        }
        return false;
    }

    public boolean applyWithCode(Student student, String code) {
        if (this.accessCode != null && this.accessCode.equals(code) && this.isActive) {
            System.out.println(student.getNickname() + " joined the tournament using an access code.");
            return true;
        }
        System.out.println("Invalid tournament access code.");
        return false;
    }

    public void generateFixture() {
        this.tournamentFixture.shuffleTeams(this.participatingTeams);
        this.tournamentFixture.createBracket(this.participatingTeams);
    }

    public void updateSchedule(LocalDate newStartDate, LocalDate newEndDate) {
        this.startDate = newStartDate;
        this.endDate = newEndDate;
        System.out.println("Tournament schedule has been updated.");
    }

    public void cancelTournament() {
        this.isActive = false;
        System.out.println("Tournament " + this.tournamentName + " has been cancelled.");
    }

    public void editDetails(String newName, int newMaxPlayers, String newLocation) {
        this.tournamentName = newName;
        this.maxPlayersPerTeam = newMaxPlayers;
        this.campusLocation = newLocation;
        System.out.println("Tournament details have been updated.");
    }
}