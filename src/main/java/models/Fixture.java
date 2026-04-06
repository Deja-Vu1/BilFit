package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Fixture {
    private String fixtureId;
    private List<Match> scheduledMatches;
    private String currentStage;

    public Fixture(String fixtureId) {
        this.fixtureId = fixtureId;
        this.scheduledMatches = new ArrayList<>();
        this.currentStage = "Group Stage";
    }

    public Fixture() {
        this.fixtureId = UUID.randomUUID().toString();
        this.scheduledMatches = new ArrayList<>();
        this.currentStage = "Group Stage";
    }

    public String getFixtureId() { return fixtureId; }
    public void setFixtureId(String fixtureId) { this.fixtureId = fixtureId; }
    public List<Match> getScheduledMatches() { return scheduledMatches; }
    public void setScheduledMatches(List<Match> scheduledMatches) { this.scheduledMatches = scheduledMatches; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
}