package controllers;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import managers.SessionManager;
import managers.TournamentManager;
import models.Match;
import models.Student;
import models.Team;
import models.Tournament;

public class TournamentDetailsController {

    @FXML private Label tournamentNameLabel;
    @FXML private Label tournamentInfoLabel;
    @FXML private Button applyTournamentButton;
    @FXML private ComboBox<Student> friendSelectionBox;
    
    @FXML private VBox teamsListContainer;
    @FXML private VBox teamsVBox;
    
    @FXML private VBox fixtureContainer;
    @FXML private HBox byeTeamBox;
    @FXML private Label byeTeamLabel;
    @FXML private VBox matchesVBox;

    private Tournament tournament;
    private TournamentManager tournamentManager;

    public void setTournament(Tournament tournament) {
        try {
            this.tournamentManager = new TournamentManager(Database.getInstance());
            this.tournament = tournament;
            
            updateHeader();
            setupFriendSelection();
            loadContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateHeader() {
        tournamentNameLabel.setText(tournament.getTournamentName());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String info = tournament.getSportType().name() + " | " + 
                      tournament.getStartDate().format(dtf) + " - " + tournament.getEndDate().format(dtf) + 
                      " | Max " + tournament.getMaxPlayersPerTeam() + " Players/Team";
        tournamentInfoLabel.setText(info);
    }

    private void setupFriendSelection() {
        friendSelectionBox.setConverter(new StringConverter<Student>() {
            @Override
            public String toString(Student student) {
                return student == null ? "" : student.getFullName() + " (" + student.getBilkentEmail() + ")";
            }

            @Override
            public Student fromString(String string) {
                return null;
            }
        });
    }

    private void loadContent() {
        new Thread(() -> {
            try {
                List<Team> dbTeams = tournamentManager.getTournamentTeams(tournament.getTournamentId());
                if (dbTeams != null) tournament.setParticipatingTeams(dbTeams);
                
                tournamentManager.fillTournamentFixtures(tournament);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                boolean hasFixture = tournament.getTournamentFixture() != null && 
                                     !tournament.getTournamentFixture().getScheduledMatches().isEmpty();

                if (hasFixture) {
                    teamsListContainer.setVisible(false); teamsListContainer.setManaged(false);
                    fixtureContainer.setVisible(true); fixtureContainer.setManaged(true);
                    populateFixture();
                } else {
                    fixtureContainer.setVisible(false); fixtureContainer.setManaged(false);
                    teamsListContainer.setVisible(true); teamsListContainer.setManaged(true);
                    populateTeamsList();
                }
            });
        }).start();
    }

    private void populateTeamsList() {
        teamsVBox.getChildren().clear();
        List<Team> teams = tournament.getParticipatingTeams();
        
        if (teams == null || teams.isEmpty()) {
            teamsVBox.getChildren().add(new Label("No teams have joined yet."));
            return;
        }

        int count = 1;
        for (Team t : teams) {
            Label teamLabel = new Label(count + ". " + t.getTeamName());
            teamLabel.setFont(Font.font("System", FontWeight.BOLD, 14.0));
            teamLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));
            
            HBox row = new HBox(teamLabel);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
            
            teamsVBox.getChildren().add(row);
            count++;
        }
    }

    private void populateFixture() {
        matchesVBox.getChildren().clear();
        
        if (byeTeamBox != null) {
            byeTeamBox.setVisible(false);
            byeTeamBox.setManaged(false);
        }

        if (tournament.getTournamentFixture() == null || tournament.getTournamentFixture().getScheduledMatches() == null) return;
        List<Match> matches = tournament.getTournamentFixture().getScheduledMatches();

        // 1. ŞAMPİYONU (WINNER) KONTROL ET VE EN ÜSTE YAZDIR
        Team champion = Database.getInstance().getWinnerTeamTournament(tournament.getTournamentId());
        if (champion != null) {
            Label champLabel = new Label("🏆 CHAMPION: " + champion.getTeamName() + " 🏆");
            champLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FF9120; -fx-background-color: #FFF4E5; -fx-padding: 10; -fx-background-radius: 10; -fx-alignment: center;");
            champLabel.setMaxWidth(Double.MAX_VALUE);
            champLabel.setAlignment(Pos.CENTER);
            VBox.setMargin(champLabel, new Insets(0, 0, 15, 0));
            matchesVBox.getChildren().add(champLabel);
        }

        // 2. MAÇLARI TUR (STAGE) NUMARASINA GÖRE SIRALA
        matches.sort(Comparator.comparingInt(Match::getCurrentStage));

        int currentRoundNum = -1;

        for (Match match : matches) {
            
            // YENİ ROUND BAŞLIĞINI EKLE
            if (match.getCurrentStage() != currentRoundNum) {
                currentRoundNum = match.getCurrentStage();
                Label roundLabel = new Label("--- ROUND " + currentRoundNum + " ---");
                roundLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4318FF; -fx-padding: 10 0 5 0;");
                matchesVBox.getChildren().add(roundLabel);
            }

            Team t1 = match.getTeam1();
            Team t2 = match.getTeam2();

            // BAY GEÇEN TAKIMI DÜZENLE (Tek takımsa)
            if (t2 == null || t1.getTeamId().equals(t2.getTeamId())) {
                Label byeLabel = new Label("🏆 Auto Advance (BYE) Round " + currentRoundNum + ": " + t1.getTeamName());
                byeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E8E3E; -fx-background-color: #E6F4EA; -fx-padding: 10; -fx-background-radius: 8;");
                VBox.setMargin(byeLabel, new Insets(0, 0, 10, 0));
                matchesVBox.getChildren().add(byeLabel);
                continue;
            }

            HBox matchRow = new HBox();
            matchRow.setAlignment(Pos.CENTER_LEFT);
            matchRow.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF;");

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

            // KAZANANI RENKLENDİR
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
            } else if (match.is_concluded()) { 
                t1Box.setStyle("-fx-background-color: #FFF4E5; -fx-background-radius: 8 0 0 8;");
                t2Box.setStyle("-fx-background-color: #FFF4E5;");
                statusLabel.setText("Draw");
                statusLabel.setStyle("-fx-text-fill: #DD6B20;");
            } else {
                t1Box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8 0 0 8;");
                t2Box.setStyle("-fx-background-color: #F8FAFC;");
                statusLabel.setStyle("-fx-text-fill: #A0AEC0;");
            }

            matchRow.getChildren().addAll(t1Box, vsLabel, t2Box, statusBox);
            VBox.setMargin(matchRow, new Insets(0, 0, 10, 0));
            matchesVBox.getChildren().add(matchRow);
        }
    }

    @FXML
    public void handleApply(ActionEvent event) {
        if (!friendSelectionBox.isVisible()) {
            friendSelectionBox.setVisible(true);
            friendSelectionBox.setManaged(true);
            applyTournamentButton.setText("Confirm Join");
            
            new Thread(() -> {
                try {
                    Student current = (Student) SessionManager.getInstance().getCurrentUser();
                    Database.getInstance().fillFriendsByEmail(current);
                    Platform.runLater(() -> {
                        friendSelectionBox.getItems().clear();
                        friendSelectionBox.getItems().addAll(current.getFriends());
                    });
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }).start();
            
        } else {
            applyTournamentButton.setText("Processing...");
            applyTournamentButton.setDisable(true);
            friendSelectionBox.setDisable(true);

            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                    String generatedTeamName = currentUser.getFullName() + " Team";
                    status = tournamentManager.createTeamAndJoinTournament(currentUser, tournament, generatedTeamName);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    if (finalStatus == DbStatus.SUCCESS) {
                        SessionManager.getInstance().setTournamentApplied(true);
                        
                        applyTournamentButton.setText("Joined / Requested");
                        loadContent();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "You have joined the tournament.");
                    } else {
                        applyTournamentButton.setText("Confirm Join");
                        applyTournamentButton.setDisable(false);
                        friendSelectionBox.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Failed", "Operation could not be performed. You might already be in a team.");
                    }
                });
            }).start();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED); 
        alert.showAndWait();
    }
}