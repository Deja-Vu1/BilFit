package controllers;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private boolean USE_MOCK_DATA = true;

    public void setMockDataMode(boolean isMock) {
        this.USE_MOCK_DATA = isMock;
    }

    public void setTournament(Tournament tournament) {
        try {
            if (!USE_MOCK_DATA) {
                this.tournamentManager = new TournamentManager(Database.getInstance());
            } else {
                System.out.println("====== ATTENTION: DETAILS SCREEN USING FAKE DATA (MOCK) ======");
            }
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
            if (USE_MOCK_DATA) {
                setupMockData();
            } else {
                try {
                    List<Team> dbTeams = Database.getInstance().getTournamentTeams(tournament.getTournamentId());
                    if (dbTeams != null) tournament.setParticipatingTeams(dbTeams);
                    
                    Database.getInstance().fillTournamentFixtures(tournament);
                } catch (Exception e) {}
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
        byeTeamBox.setVisible(false);
        byeTeamBox.setManaged(false);

        List<Match> matches = tournament.getTournamentFixture().getScheduledMatches();

        for (Match match : matches) {
            Team t1 = match.getTeam1();
            Team t2 = match.getTeam2();

            if (t2 == null) {
                byeTeamLabel.setText("🏆 Automatically Advanced (BYE): " + t1.getTeamName());
                byeTeamBox.setVisible(true);
                byeTeamBox.setManaged(true);
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
    }

    private void setupMockData() {
        List<Team> mockTeams = new ArrayList<>();
        Team t1 = new Team("M1", "CS Strikers", "MOCK", 2, false, new Student("Ali", "ali@bilkent", "1"));
        Team t2 = new Team("M2", "EE Engineers", "MOCK", 2, false, new Student("Veli", "veli@bilkent", "2"));
        Team t3 = new Team("M3", "Math Geniuses", "MOCK", 2, false, new Student("Ayse", "ayse@bilkent", "3"));
        Team t4 = new Team("M4", "Physics Nerds", "MOCK", 2, false, new Student("Fatma", "fatma@bilkent", "4"));
        Team t5 = new Team("M5", "Lucky Ones", "MOCK", 2, false, new Student("Can", "can@bilkent", "5"));

        mockTeams.add(t1); mockTeams.add(t2); mockTeams.add(t3); mockTeams.add(t4); mockTeams.add(t5);
        tournament.setParticipatingTeams(mockTeams);

        models.Fixture fix = new models.Fixture(tournament.getTournamentId());
        
        Match m1 = new Match("1", java.time.LocalDateTime.now(), tournament.getSportType(), t1, t2);
        m1.setWinner(t1); 
        
        Match m2 = new Match("2", java.time.LocalDateTime.now(), tournament.getSportType(), t3, t4);
        
        Match m3 = new Match("3", java.time.LocalDateTime.now(), tournament.getSportType(), t5, null);
        
        fix.getScheduledMatches().add(m1);
        fix.getScheduledMatches().add(m2);
        fix.getScheduledMatches().add(m3);
        
        tournament.setTournamentFixture(fix);
    }

    @FXML
    public void handleApply(ActionEvent event) {
        if (!friendSelectionBox.isVisible()) {
            friendSelectionBox.setVisible(true);
            friendSelectionBox.setManaged(true);
            applyTournamentButton.setText("Confirm Join");
            
            new Thread(() -> {
                if (USE_MOCK_DATA) {
                    List<Student> mockFriends = new ArrayList<>();
                    mockFriends.add(new Student("Cem", "cem@bilkent", "3"));
                    mockFriends.add(new Student("Berk", "berk@bilkent", "4"));
                    Platform.runLater(() -> {
                        friendSelectionBox.getItems().clear();
                        friendSelectionBox.getItems().addAll(mockFriends);
                    });
                } else {
                    try {
                        Student current = (Student) SessionManager.getInstance().getCurrentUser();
                        Database.getInstance().fillFriendsByEmail(current);
                        Platform.runLater(() -> {
                            friendSelectionBox.getItems().clear();
                            friendSelectionBox.getItems().addAll(current.getFriends());
                        });
                    } catch(Exception e) {}
                }
            }).start();
            
        } else {
            applyTournamentButton.setText("Processing...");
            applyTournamentButton.setDisable(true);
            friendSelectionBox.setDisable(true);

            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                
                if (USE_MOCK_DATA) {
                    status = DbStatus.SUCCESS;
                } else {
                    try {
                        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                        Student selectedFriend = friendSelectionBox.getValue();
                        status = tournamentManager.processTournamentApplication(tournament, currentUser, selectedFriend);
                    } catch (Exception ex) {}
                }

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    if (finalStatus == DbStatus.SUCCESS) {
                        if (friendSelectionBox.getValue() == null) {
                            if (!USE_MOCK_DATA) SessionManager.getInstance().setTournamentApplied(true);
                            showAlert(Alert.AlertType.INFORMATION, "Successful", "MOCK: You have joined the tournament individually.");
                        } else {
                            showAlert(Alert.AlertType.INFORMATION, "Successful", "MOCK: Team request successfully sent to your friend.");
                        }
                        applyTournamentButton.setText("Joined / Requested");
                        loadContent();
                    } else {
                        applyTournamentButton.setText("Confirm Join");
                        applyTournamentButton.setDisable(false);
                        friendSelectionBox.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Failed", "Operation could not be performed.");
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
        try { 
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); 
        } catch (Exception e) {}
        alert.showAndWait();
    }
}