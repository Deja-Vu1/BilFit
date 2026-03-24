package java.models;

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
        this.currentStage = "Group Stage"; // Default starting stage
    }

    public void shuffleTeams(List<Team> teams) {
        Collections.shuffle(teams);
        System.out.println("Teams have been shuffled automatically for fair matchmaking.");

    }

    public void createBracket(List<Team> teams) {

        // The algorithm to pair teams will be implemented in controller
        System.out.println("Tournament bracket has been successfully generated.");
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