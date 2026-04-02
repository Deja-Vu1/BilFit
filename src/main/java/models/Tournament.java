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
        this.tournamentFixture = new Fixture(tournamentId + "_FIXTURE");
    }

    public String getTournamentId() { return tournamentId; }
    public void setTournamentId(String tournamentId) { this.tournamentId = tournamentId; }
    public String getTournamentName() { return tournamentName; }
    public void setTournamentName(String tournamentName) { this.tournamentName = tournamentName; }
    public SportType getSportType() { return sportType; }
    public void setSportType(SportType sportType) { this.sportType = sportType; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) { this.maxPlayersPerTeam = maxPlayersPerTeam; }
    public boolean isHasGe250() { return hasGe250; }
    public void setHasGe250(boolean hasGe250) { this.hasGe250 = hasGe250; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public String getCampusLocation() { return campusLocation; }
    public void setCampusLocation(String campusLocation) { this.campusLocation = campusLocation; }
    public List<Team> getParticipatingTeams() { return participatingTeams; }
    public void setParticipatingTeams(List<Team> participatingTeams) { this.participatingTeams = participatingTeams; }
    public Fixture getTournamentFixture() { return tournamentFixture; }
    public void setTournamentFixture(Fixture tournamentFixture) { this.tournamentFixture = tournamentFixture; }
}