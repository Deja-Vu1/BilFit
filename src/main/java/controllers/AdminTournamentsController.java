package controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import managers.TournamentManager;
import models.Match;
import models.SportType;
import models.Team;
import models.Tournament;

public class AdminTournamentsController {

    @FXML private VBox tournamentsContainer;
    
    private Map<CheckBox, Tournament> selectionMap = new HashMap<>();
    private Database db = Database.getInstance();
    private TournamentManager tManager = new TournamentManager(db);
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadTournaments();
    }

    private void loadTournaments() {
        if (tournamentsContainer == null) return;
        
        tournamentsContainer.getChildren().clear();
        selectionMap.clear();

        Label loadingLabel = new Label("Loading tournaments...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
        tournamentsContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            List<Tournament> tournaments = tManager.getAllActiveTournaments(); 
            
            if (tournaments != null && !tournaments.isEmpty()) {
                tournaments.sort(Comparator.comparing(Tournament::getStartDate));
            }
            
            Platform.runLater(() -> {
                tournamentsContainer.getChildren().clear();
                
                if (tournaments == null || tournaments.isEmpty()) {
                    Label emptyLabel = new Label("No active tournament found in the system.");
                    emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                    tournamentsContainer.getChildren().add(emptyLabel);
                } else {
                    for (Tournament t : tournaments) {
                        tournamentsContainer.getChildren().add(createTournamentCard(t));
                    }
                }
            });
        }).start();
    }

    private HBox createTournamentCard(Tournament t) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        card.setPadding(new Insets(15, 20, 15, 20));

        CheckBox selectBox = new CheckBox();
        selectBox.setStyle("-fx-cursor: hand;");
        selectionMap.put(selectBox, t);

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(t.getTournamentName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2B3674;");
        
        Label detailsLabel = new Label("Sport: " + t.getSportType().name() + " | Max Capacity/Team: " + t.getMaxPlayersPerTeam() + " | Campus: " + t.getCampusLocation());
        detailsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #A3AED0;");
        
        Label dateLabel = new Label(t.getStartDate() + " -> " + t.getEndDate());
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4318FF; -fx-font-weight: bold;");
        
        infoBox.getChildren().addAll(nameLabel, detailsLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 80;");
        editBtn.setOnAction(e -> handleEditTournament(t));

        Button scheduleBtn = new Button("Schedule / Result");
        scheduleBtn.setStyle("-fx-background-color: #FFF4E5; -fx-text-fill: #FF9120; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 130;");
        scheduleBtn.setOnAction(e -> handleSchedule(t));

        Button fixtureBtn = new Button("Create Fixture");
        fixtureBtn.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 120;");
        fixtureBtn.setOnAction(e -> handleCreateFixture(t));

        card.getChildren().addAll(selectBox, infoBox, spacer, editBtn, scheduleBtn, fixtureBtn);
        return card;
    }

    @FXML
    public void handleCreateTournament(ActionEvent event) {
        Stage dialog = createDialogStage("Create New Tournament");
        VBox layout = (VBox) dialog.getScene().getRoot();

        TextField nameField = new TextField(); 
        nameField.setPromptText("Tournament Title (e.g., Spring Cup)");
        nameField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        ComboBox<String> sportCombo = new ComboBox<>();
        for (SportType s : SportType.values()) {
            sportCombo.getItems().add(s.name());
        }
        sportCombo.setPromptText("Select Sport Type");
        sportCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        ComboBox<String> campusCombo = new ComboBox<>();
        campusCombo.getItems().addAll("Main Campus", "East Campus");
        campusCombo.setPromptText("Select Campus");
        campusCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        DatePicker startDate = new DatePicker(); 
        startDate.setPromptText("Start Date");
        startDate.setStyle("-fx-font-size: 14px; -fx-pref-width: 300;");

        DatePicker endDate = new DatePicker(); 
        endDate.setPromptText("End Date");
        endDate.setStyle("-fx-font-size: 14px; -fx-pref-width: 300;");

        TextField maxPlayers = new TextField(); 
        maxPlayers.setPromptText("Max Players per Team (e.g., 5)");
        maxPlayers.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        CheckBox ge250Check = new CheckBox("Grant GE250 Points?");
        ge250Check.setStyle("-fx-font-size: 14px; -fx-text-fill: #2B3674;");

        HBox btnBox = new HBox(15); 
        btnBox.setAlignment(Pos.CENTER);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Create");
        saveBtn.setStyle("-fx-background-color: #1E8E3E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        
        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || sportCombo.getValue() == null || campusCombo.getValue() == null || 
                startDate.getValue() == null || endDate.getValue() == null || maxPlayers.getText().isEmpty()) {
                showCustomAlert("Missing Information", "Please fill out all fields.");
                return;
            }

            try {
                int players = Integer.parseInt(maxPlayers.getText().trim());
                
                Tournament t = new Tournament(
                    "", 
                    nameField.getText().trim(), 
                    SportType.valueOf(sportCombo.getValue()), 
                    startDate.getValue(), 
                    endDate.getValue(), 
                    players, 
                    ge250Check.isSelected(), 
                    "", 
                    campusCombo.getValue()
                );
                
                saveBtn.setText("Processing...");
                saveBtn.setDisable(true);

                new Thread(() -> {
                    DbStatus status = tManager.createTournament(t);

                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) {
                            dialog.close(); 
                            Platform.runLater(() -> {
                                loadTournaments(); 
                                showCustomAlert("Success", "Tournament created successfully.");
                            });
                        } else {
                            saveBtn.setText("Create");
                            saveBtn.setDisable(false);
                            showCustomAlert("Error", "An error occurred while creating the tournament.");
                        }
                    });
                }).start();
                
            } catch (NumberFormatException ex) {
                showCustomAlert("Invalid Format", "Player count must consist of numbers only.");
            }
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        layout.getChildren().addAll(nameField, sportCombo, campusCombo, startDate, endDate, maxPlayers, ge250Check, btnBox);
        dialog.showAndWait();
    }

    private void handleEditTournament(Tournament t) {
        Stage dialog = createDialogStage("Edit Tournament: " + t.getTournamentName());
        VBox layout = (VBox) dialog.getScene().getRoot();

        TextField nameField = new TextField(t.getTournamentName());
        nameField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");
        
        TextField maxPlayers = new TextField(String.valueOf(t.getMaxPlayersPerTeam()));
        maxPlayers.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-background-color: #1E8E3E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        
        saveBtn.setOnAction(e -> {
            try {
                int newPlayers = Integer.parseInt(maxPlayers.getText().trim());
                String newName = nameField.getText().trim();
                
                saveBtn.setText("Saving...");
                saveBtn.setDisable(true);

                new Thread(() -> {
                    DbStatus status = tManager.editDetails(t, newName, newPlayers);
                    
                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) { 
                            dialog.close(); 
                            Platform.runLater(() -> {
                                loadTournaments();
                                showCustomAlert("Success", "Tournament successfully updated.");
                            });
                        } else {
                            saveBtn.setText("Save");
                            saveBtn.setDisable(false);
                            showCustomAlert("Error", "An error occurred during the update.");
                        }
                    });
                }).start();
            } catch (NumberFormatException ex) {
                showCustomAlert("Invalid Format", "Player count must be a numeric value.");
            }
        });
        
        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        layout.getChildren().addAll(new Label("New Tournament Title:"), nameField, new Label("Max Participants per Team:"), maxPlayers, btnBox);
        dialog.showAndWait();
    }

    private void handleCreateFixture(Tournament t) {
        if (isProcessing) return;
        isProcessing = true;
        
        new Thread(() -> {
            Team existingWinner = db.getWinnerTeamTournament(t.getTournamentId());
            if (existingWinner != null) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    showCustomAlert("Information", "This tournament has already concluded. Champion: " + existingWinner.getTeamName());
                });
                return;
            }

            List<Team> allTeams = tManager.getTournamentTeams(t.getTournamentId());
            List<Match> allMatches = db.getAllTournamentMatches(t.getTournamentId());
            int currentStage = db.getCurrentStageOfTournament(t.getTournamentId());
            
            if (allTeams == null || allTeams.size() < 2) {
                Platform.runLater(() -> { 
                    isProcessing = false; 
                    showCustomAlert("Error", "At least 2 approved teams are required to create a fixture."); 
                });
                return;
            }

            List<Team> activeTeams = new ArrayList<>();

            if (allMatches == null || allMatches.isEmpty() || currentStage == 0) {
                activeTeams.addAll(allTeams);
                currentStage = 1;
            } else {
                boolean hasPending = false;
                List<String> loserIds = new ArrayList<>();

                for (Match m : allMatches) {
                    boolean isByeMatch = (m.getTeam2() == null || (m.getTeam1() != null && m.getTeam1().getTeamId().equals(m.getTeam2().getTeamId())));

                    if (m.getWinner() == null && !isByeMatch) {
                        hasPending = true; 
                    } else if (m.getWinner() != null && !isByeMatch) {
                        if (m.getTeam1() != null && !m.getTeam1().getTeamId().equals(m.getWinner().getTeamId())) {
                            loserIds.add(m.getTeam1().getTeamId());
                        }
                        if (m.getTeam2() != null && !m.getTeam2().getTeamId().equals(m.getWinner().getTeamId())) {
                            loserIds.add(m.getTeam2().getTeamId());
                        }
                    }
                }

                if (hasPending) {
                    Platform.runLater(() -> {
                        isProcessing = false;
                        showCustomAlert("Warning", "Cannot generate the next round until all current matches are concluded and winners are set.");
                    });
                    return;
                }

                for (Team team : allTeams) {
                    if (!loserIds.contains(team.getTeamId())) {
                        activeTeams.add(team);
                    }
                }

                if (activeTeams.size() == 1) {
                    Team champion = activeTeams.get(0);
                    db.setWinnerTournament(t.getTournamentId(), champion);
                    
                    Platform.runLater(() -> {
                        isProcessing = false;
                        showCustomAlert("Tournament Concluded", "The tournament has ended! The Champion is: " + champion.getTeamName());
                    });
                    return;
                }

                currentStage++;
            }

            Collections.shuffle(activeTeams);
            OffsetDateTime matchTime = OffsetDateTime.of(t.getStartDate(), LocalTime.of(1, 0), ZoneOffset.UTC);

            int matchCount = 0;
            final int stageToInsert = currentStage;
            
            for (int i = 0; i < activeTeams.size(); i += 2) {
                Team t1 = activeTeams.get(i);
                
                if (i + 1 < activeTeams.size()) {
                    Team t2 = activeTeams.get(i + 1);
                    db.insertMatch(t, t1, t2, matchTime, 10, stageToInsert); 
                    matchCount++;
                } else {
                    db.insertMatch(t, t1, t1, matchTime, 0, stageToInsert); 
                }
            }

            final int finalMatches = matchCount;
            Platform.runLater(() -> {
                isProcessing = false;
                showCustomAlert("Success", "Round " + stageToInsert + " generated! " + finalMatches + " new matches created.");
            });
        }).start();
    }

    private void handleSchedule(Tournament t) {
        Stage dialog = createDialogStage("Schedule & Results: " + t.getTournamentName());
        VBox layout = (VBox) dialog.getScene().getRoot();
        
        ScrollPane scrollPane = new ScrollPane(); 
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(450); 

        VBox matchesBox = new VBox(15);
        matchesBox.setPadding(new Insets(10));

        new Thread(() -> {
            List<Match> matches = db.getAllTournamentMatches(t.getTournamentId());
            Team champion = db.getWinnerTeamTournament(t.getTournamentId());
            
            Platform.runLater(() -> {
                
                if (champion != null) {
                    Label champLabel = new Label("🏆 CHAMPION: " + champion.getTeamName() + " 🏆");
                    champLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FF9120; -fx-background-color: #FFF4E5; -fx-padding: 10; -fx-background-radius: 10; -fx-alignment: center;");
                    champLabel.setMaxWidth(Double.MAX_VALUE);
                    champLabel.setAlignment(Pos.CENTER);
                    matchesBox.getChildren().add(champLabel);
                }

                if (matches == null || matches.isEmpty()) {
                    new Thread(() -> {
                        List<Team> registeredTeams = tManager.getTournamentTeams(t.getTournamentId());
                        Platform.runLater(() -> {
                            if (registeredTeams == null || registeredTeams.isEmpty()) {
                                Label emptyLabel = new Label("No teams registered yet.");
                                emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                                matchesBox.getChildren().add(emptyLabel);
                            } else {
                                Label titleLabel = new Label("Registered Teams (Awaiting Fixture):");
                                titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2B3674;");
                                matchesBox.getChildren().add(titleLabel);

                                for (int i = 0; i < registeredTeams.size(); i++) {
                                    Team team = registeredTeams.get(i);
                                    HBox teamRow = new HBox(15);
                                    teamRow.setAlignment(Pos.CENTER_LEFT);
                                    teamRow.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-padding: 10; -fx-background-color: #F8FAFC;");
                                    
                                    Label tLabel = new Label((i + 1) + ". " + team.getTeamName() + " (Pending Fixture)");
                                    tLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #A3AED0;");
                                    
                                    teamRow.getChildren().add(tLabel);
                                    matchesBox.getChildren().add(teamRow);
                                }
                            }
                        });
                    }).start();
                } else {
                    matches.sort(Comparator.comparingInt(Match::getCurrentStage));
                    
                    int currentRoundNum = -1;

                    for (Match m : matches) {
                        
                        if (m.getCurrentStage() != currentRoundNum) {
                            currentRoundNum = m.getCurrentStage();
                            Label roundLabel = new Label("--- ROUND " + currentRoundNum + " ---");
                            roundLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4318FF; -fx-padding: 10 0 5 0;");
                            matchesBox.getChildren().add(roundLabel);
                        }

                        HBox matchRow = new HBox(15); 
                        matchRow.setAlignment(Pos.CENTER_LEFT);
                        matchRow.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-padding: 10; -fx-background-color: #F8FAFC;");

                        String team1Name = (m.getTeam1() != null) ? m.getTeam1().getTeamName() : "Unknown";
                        boolean isBye = (m.getTeam2() == null || (m.getTeam1() != null && m.getTeam1().getTeamId().equals(m.getTeam2().getTeamId())));
                        String team2Name = isBye ? "BYE" : m.getTeam2().getTeamName();
                        
                        Label t1Label = new Label(team1Name);
                        Label vsLabel = new Label(" VS ");
                        Label t2Label = new Label(team2Name);

                        String defaultStyle = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2B3674;";
                        String winStyle = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E8E3E;"; 
                        String loseStyle = "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #D93025; -fx-strikethrough: true;"; 

                        t1Label.setStyle(defaultStyle);
                        vsLabel.setStyle("-fx-text-fill: #A3AED0; -fx-font-weight: bold; -fx-font-size: 14px;");
                        t2Label.setStyle(defaultStyle);

                        HBox namesBox = new HBox(5, t1Label, vsLabel, t2Label);
                        namesBox.setAlignment(Pos.CENTER_LEFT);
                        namesBox.setMinWidth(Region.USE_PREF_SIZE);
                        
                        Region rSpacer = new Region();
                        HBox.setHgrow(rSpacer, Priority.ALWAYS);
                        
                        if (isBye) {
                            Label bayLabel = new Label("Auto Advance (BYE)");
                            bayLabel.setStyle("-fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-background-color: #E6F4EA; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
                            bayLabel.setMinWidth(Region.USE_PREF_SIZE); 
                            
                            t1Label.setStyle(winStyle); 
                            
                            matchRow.getChildren().addAll(namesBox, rSpacer, bayLabel);
                        } else {
                            ComboBox<String> winnerCombo = new ComboBox<>();
                            winnerCombo.getItems().addAll(team1Name, team2Name);
                            winnerCombo.setPromptText("Select Winner");
                            winnerCombo.setPrefWidth(160);
                            winnerCombo.setMinWidth(Region.USE_PREF_SIZE);
                            winnerCombo.setPrefHeight(35);
                            
                            Button updateBtn = new Button("Confirm");
                            updateBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                            updateBtn.setPrefWidth(100);
                            updateBtn.setMinWidth(Region.USE_PREF_SIZE);
                            updateBtn.setPrefHeight(35);
                            
                            if (m.getWinner() != null) {
                                String winnerName = m.getWinner().getTeamName();
                                winnerCombo.setValue(winnerName);
                                updateBtn.setText("Confirmed");
                                updateBtn.setDisable(true);

                                if (winnerName.equals(team1Name)) {
                                    t1Label.setStyle(winStyle);
                                    t2Label.setStyle(loseStyle);
                                } else if (winnerName.equals(team2Name)) {
                                    t2Label.setStyle(winStyle);
                                    t1Label.setStyle(loseStyle);
                                }
                            } else if (m.is_concluded()) { 
                                updateBtn.setText("Concluded");
                                updateBtn.setDisable(true);
                            }
                            
                            updateBtn.setOnAction(e -> {
                                if (winnerCombo.getValue() == null) return;
                                
                                Team winner = null;
                                if (winnerCombo.getValue().equals(team1Name)) {
                                    winner = m.getTeam1();
                                } else if (winnerCombo.getValue().equals(team2Name)) {
                                    winner = m.getTeam2();
                                }
                                
                                final Team finalWinner = winner;
                                updateBtn.setText("...");
                                updateBtn.setDisable(true);
                                
                                new Thread(() -> {
                                    db.updateMatchWinner(m.getMatchId(), finalWinner);
                                    db.updateMatchStatus(m.getMatchId(), true);
                                    
                                    Platform.runLater(() -> {
                                        dialog.close();
                                        handleSchedule(t);
                                    });
                                }).start();
                            });
                            
                            matchRow.getChildren().addAll(namesBox, rSpacer, winnerCombo, updateBtn);
                        }
                        
                        matchesBox.getChildren().add(matchRow);
                    }
                }
            });
        }).start();

        scrollPane.setContent(matchesBox);
        
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 35; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        
        layout.getChildren().addAll(scrollPane, closeBtn);
        dialog.getScene().getWindow().setWidth(750); 
        dialog.show(); 
    }

    @FXML
    public void handleBulkDelete(ActionEvent event) {
        List<Tournament> toDelete = new ArrayList<>();
        for (Map.Entry<CheckBox, Tournament> entry : selectionMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                toDelete.add(entry.getValue());
            }
        }

        if (toDelete.isEmpty()) {
            showCustomAlert("Warning", "Please select the tournaments you want to delete.");
            return;
        }

        isProcessing = true;
        new Thread(() -> {
            int count = 0;
            for (Tournament t : toDelete) {
                DbStatus status = db.deleteTournament(t.getTournamentId());
                if (status == DbStatus.SUCCESS) {
                    count++;
                }
            }
            
            final int finalCount = count;
            Platform.runLater(() -> {
                isProcessing = false;
                loadTournaments();
                showCustomAlert("Information", finalCount + " tournaments permanently deleted.");
            });
        }).start();
    }

    private Stage createDialogStage(String titleText) {
        Stage dialogStage = new Stage(); 
        dialogStage.initModality(Modality.APPLICATION_MODAL); 
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        
        VBox layout = new VBox(20); 
        layout.setAlignment(Pos.CENTER); 
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
        
        DropShadow shadow = new DropShadow(); 
        shadow.setRadius(20); 
        shadow.setColor(Color.rgb(0, 0, 0, 0.15)); 
        layout.setEffect(shadow);
        
        Label titleLabel = new Label(titleText); 
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");
        layout.getChildren().add(titleLabel);
        
        Scene scene = new Scene(layout); 
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene); 
        dialogStage.centerOnScreen();
        
        return dialogStage;
    }

    private void showCustomAlert(String title, String message) {
        Stage dialog = createDialogStage(title);
        VBox layout = (VBox) dialog.getScene().getRoot();
        
        Label msgLabel = new Label(message); 
        msgLabel.setWrapText(true); 
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");
        
        Button okBtn = new Button("OK"); 
        okBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        okBtn.setOnAction(e -> dialog.close());
        
        layout.getChildren().addAll(msgLabel, okBtn); 
        
        dialog.show(); 
    }
}