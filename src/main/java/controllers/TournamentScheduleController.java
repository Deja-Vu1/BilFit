package controllers;

import java.util.List;
import java.util.stream.Collectors;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import models.Match;
import models.Team;
import models.Tournament;

public class TournamentScheduleController {

    @FXML private Label scheduleTitleLabel;
    @FXML private Label tournamentStatusLabel;
    @FXML private VBox matchesVBox;
    @FXML private HBox byeTeamBox;
    @FXML private Label byeTeamLabel;

    private Team team;

    public void setTeam(Team team) {
        this.team = team;
        scheduleTitleLabel.setText("Schedule for " + team.getTeamName());
        loadFixture();
    }

    private void loadFixture() {
        matchesVBox.getChildren().clear();
        byeTeamBox.setVisible(false);
        byeTeamBox.setManaged(false);

        new Thread(() -> {
            Tournament tournament = Database.getInstance().getTournamentByTeamId(team.getTeamId());
            if (tournament == null) return;

            List<Match> allMatches = Database.getInstance().getAllTournamentMatches(tournament.getTournamentId());
            
            if (allMatches.isEmpty()) {
                List<Team> allTeams = Database.getInstance().getTournamentTeams(tournament.getTournamentId());
                
                Platform.runLater(() -> {
                    scheduleTitleLabel.setText("Participating Teams"); 
                    tournamentStatusLabel.setText("Status: Waiting for Admin to start the tournament & generate fixtures.");
                    tournamentStatusLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #DD6B20;"); 

                    matchesVBox.getChildren().clear();
                    
                    if (allTeams == null || allTeams.isEmpty()) {
                        Label emptyLabel = new Label("No teams have joined yet.");
                        emptyLabel.setStyle("-fx-text-fill: #A0AEC0; -fx-font-weight: bold;");
                        matchesVBox.getChildren().add(emptyLabel);
                        return;
                    }

                    int count = 1;
                    for (Team t : allTeams) {
                        HBox row = new HBox();
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setPadding(new Insets(15, 15, 15, 15));
                        
                        boolean isMyTeam = t.getTeamId().equals(team.getTeamId());
                        
                        if (isMyTeam) {
                            row.setStyle("-fx-border-color: #4299E1; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #EBF8FF;");
                        } else {
                            row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF;");
                        }
                        
                        VBox.setMargin(row, new Insets(0, 0, 10, 0)); 

                        String captainInfo = " (Captain: " + t.getCaptain().getFullName() + ")";
                        String myTeamTag = isMyTeam ? "   ★ YOUR TEAM" : "";
                        
                        Label teamLabel = new Label(count + ". " + t.getTeamName() + captainInfo + myTeamTag);
                        teamLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                        teamLabel.setTextFill(javafx.scene.paint.Color.web(isMyTeam ? "#2B6CB0" : "#2b3674"));
                        
                        row.getChildren().add(teamLabel);
                        matchesVBox.getChildren().add(row);
                        count++;
                    }
                });
                return; 
            }

            List<Match> teamMatches = allMatches.stream()
                .filter(m -> m.getTeam1().getTeamId().equals(team.getTeamId()) || 
                            (m.getTeam2() != null && m.getTeam2().getTeamId().equals(team.getTeamId())))
                .collect(Collectors.toList());

            Platform.runLater(() -> {
                scheduleTitleLabel.setText("Schedule for " + team.getTeamName()); 
                tournamentStatusLabel.setText("Status: Tournament Started - Fixtures Generated");
                tournamentStatusLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38A169;"); 

                matchesVBox.getChildren().clear();
                
                if (teamMatches.isEmpty()) {
                    Label emptyLabel = new Label("No matches found for your team.");
                    emptyLabel.setStyle("-fx-text-fill: #A0AEC0; -fx-font-weight: bold;");
                    matchesVBox.getChildren().add(emptyLabel);
                    return;
                }

                for (Match match : teamMatches) {
                    Team t1 = match.getTeam1();
                    Team t2 = match.getTeam2();

                    if (t2 == null) {
                        if (t1.getTeamId().equals(team.getTeamId())) {
                            byeTeamLabel.setText("🏆 Automatically Advanced (BYE): " + t1.getTeamName());
                            byeTeamBox.setVisible(true);
                            byeTeamBox.setManaged(true);
                        }
                        continue; 
                    }

                    HBox matchRow = new HBox();
                    matchRow.setAlignment(Pos.CENTER_LEFT);
                    matchRow.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF;");
                    VBox.setMargin(matchRow, new Insets(0, 0, 10, 0));

                    HBox t1Box = new HBox();
                    t1Box.setAlignment(Pos.CENTER);
                    t1Box.setPrefWidth(220);
                    t1Box.setPadding(new Insets(15, 10, 15, 10));
                    Label t1Label = new Label(t1.getTeamName());
                    t1Label.setFont(Font.font("System", FontWeight.BOLD, 13));
                    t1Label.setTextFill(javafx.scene.paint.Color.web("#2b3674"));
                    t1Box.getChildren().add(t1Label);

                    Label vsLabel = new Label(" VS ");
                    vsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                    vsLabel.setTextFill(javafx.scene.paint.Color.web("#A0AEC0"));

                    HBox t2Box = new HBox();
                    t2Box.setAlignment(Pos.CENTER);
                    t2Box.setPrefWidth(220);
                    t2Box.setPadding(new Insets(15, 10, 15, 10));
                    Label t2Label = new Label(t2.getTeamName());
                    t2Label.setFont(Font.font("System", FontWeight.BOLD, 13));
                    t2Label.setTextFill(javafx.scene.paint.Color.web("#2b3674"));
                    t2Box.getChildren().add(t2Label);

                    HBox statusBox = new HBox();
                    statusBox.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(statusBox, Priority.ALWAYS);
                    statusBox.setPadding(new Insets(0, 20, 0, 0));
                    Label statusLabel = new Label("Pending");
                    statusLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
                    statusBox.getChildren().add(statusLabel);

                    if (match.getWinner() != null) {
                        if (match.getWinner().getTeamId().equals(t1.getTeamId())) {
                            t1Box.setStyle("-fx-background-color: #D4EDDA; -fx-background-radius: 8 0 0 8;");
                            t2Box.setStyle("-fx-background-color: #F8D7DA;"); 
                            statusLabel.setText(t1.getTeamName() + " Won");
                            statusLabel.setStyle("-fx-text-fill: #28A745;");
                        } else {
                            t1Box.setStyle("-fx-background-color: #F8D7DA; -fx-background-radius: 8 0 0 8;"); 
                            t2Box.setStyle("-fx-background-color: #D4EDDA;"); 
                            statusLabel.setText(t2.getTeamName() + " Won");
                            statusLabel.setStyle("-fx-text-fill: #28A745;");
                        }
                    } else {
                        t1Box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8 0 0 8;");
                        t2Box.setStyle("-fx-background-color: #F8FAFC;");
                        statusLabel.setStyle("-fx-text-fill: #A0AEC0;");
                    }

                    matchRow.getChildren().addAll(t1Box, vsLabel, t2Box, statusBox);
                    matchesVBox.getChildren().add(matchRow);
                }
            });
        }).start();
    }
}