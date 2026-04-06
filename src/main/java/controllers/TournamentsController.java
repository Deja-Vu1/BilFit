package controllers;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import managers.SessionManager;
import managers.TournamentManager;
import models.Student;
import models.Team;
import models.Tournament;

public class TournamentsController {

    @FXML private Button applyCodeButton;
    @FXML private VBox myTournamentsCard; 
    @FXML private VBox upcomingTournamentsContainer;
    @FXML private VBox myTournamentsContainer;
    
    @FXML private Button btnIncoming;
    @FXML private Button btnOutgoing;
    
    @FXML private VBox incomingRequestsContainer;
    @FXML private VBox outgoingRequestsContainer;

    private TournamentManager tournamentManager;

    @FXML
    public void initialize() {
        try {
            tournamentManager = new TournamentManager(Database.getInstance());
            
            boolean isApplied = SessionManager.getInstance().isTournamentApplied();
            boolean isJoined = SessionManager.getInstance().isTournamentJoinedWithCode();

            if (isApplied || isJoined) {
                if (myTournamentsCard != null) {
                    myTournamentsCard.setVisible(true);
                    myTournamentsCard.setManaged(true);
                }
            }
            
            if (isJoined && applyCodeButton != null) {
                applyCodeButton.setText("Joined");
                applyCodeButton.setDisable(true);
            }

            showIncoming();
            loadUpcomingTournaments();
            loadTeamManagementData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showIncoming() {
        if(incomingRequestsContainer != null) {
            incomingRequestsContainer.setVisible(true); incomingRequestsContainer.setManaged(true);
            outgoingRequestsContainer.setVisible(false); outgoingRequestsContainer.setManaged(false);
            updateTabStyles(btnIncoming);
        }
    }

    @FXML
    public void showOutgoing() {
        if(outgoingRequestsContainer != null) {
            incomingRequestsContainer.setVisible(false); incomingRequestsContainer.setManaged(false);
            outgoingRequestsContainer.setVisible(true); outgoingRequestsContainer.setManaged(true);
            updateTabStyles(btnOutgoing);
        }
    }

    private void updateTabStyles(Button activeBtn) {
        if(btnIncoming != null) {
            btnIncoming.getStyleClass().removeAll("tab-btn-active", "tab-btn-inactive");
            btnIncoming.getStyleClass().add("tab-btn-inactive");
        }
        if(btnOutgoing != null) {
            btnOutgoing.getStyleClass().removeAll("tab-btn-active", "tab-btn-inactive");
            btnOutgoing.getStyleClass().add("tab-btn-inactive");
        }
        
        if(activeBtn != null) {
            activeBtn.getStyleClass().remove("tab-btn-inactive");
            activeBtn.getStyleClass().add("tab-btn-active");
        }
    }

    private void loadUpcomingTournaments() {
        if (upcomingTournamentsContainer != null) {
            upcomingTournamentsContainer.getChildren().clear();
        }

        new Thread(() -> {
            try {
                List<Tournament> tournaments = tournamentManager.getAllActiveTournaments();

                if (tournaments != null && upcomingTournamentsContainer != null) {
                    tournaments.sort(Comparator.comparing(Tournament::getStartDate));
                    
                    Platform.runLater(() -> {
                        for (Tournament t : tournaments) {
                            HBox row = createTournamentRow(t);
                            upcomingTournamentsContainer.getChildren().add(row);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadTeamManagementData() {
        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                
                List<Team> incoming = tournamentManager.getTeamIncomingRequests(null, currentUser);
                List<Student> outgoing = tournamentManager.getTeamOutgoingRequests(null, currentUser);
                List<Team> myTeams = tournamentManager.getMyTeams(currentUser);
                
                Platform.runLater(() -> {
                    populateIncomingTeamList(incomingRequestsContainer, incoming);
                    populateStudentRequestList(outgoingRequestsContainer, outgoing, "outgoing");
                    populateMyTournamentsList(myTournamentsContainer, myTeams, currentUser);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void populateIncomingTeamList(VBox container, List<Team> teams) {
        if (container == null) return;
        container.getChildren().clear();

        if (teams == null || teams.isEmpty()) {
            Label emptyLbl = new Label("No incoming requests.");
            emptyLbl.setStyle("-fx-text-fill: #a0aec0; -fx-padding: 10;");
            container.getChildren().add(emptyLbl);
            return;
        }

        for (Team t : teams) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");

            Label nameLabel = new Label("Team: " + t.getTeamName() + " (Captain: " + t.getCaptain().getFullName() + ")");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2b3674;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().add("btn-success");
            acceptBtn.setOnAction(e -> handleTeamRequestAction(t, "accept"));

            Button rejectBtn = new Button("Reject");
            rejectBtn.getStyleClass().add("btn-danger");
            rejectBtn.setOnAction(e -> handleTeamRequestAction(t, "reject"));
            
            HBox btnBox = new HBox(10, acceptBtn, rejectBtn);
            row.getChildren().addAll(nameLabel, spacer, btnBox);

            container.getChildren().add(row);
        }
    }
    
    private void populateStudentRequestList(VBox container, List<Student> students, String type) {
        if (container == null) return;
        container.getChildren().clear();

        if (students == null || students.isEmpty()) {
            Label emptyLbl = new Label("No outgoing requests.");
            emptyLbl.setStyle("-fx-text-fill: #a0aec0; -fx-padding: 10;");
            container.getChildren().add(emptyLbl);
            return;
        }

        for (Student s : students) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");

            Label nameLabel = new Label(s.getFullName() + " (" + s.getBilkentEmail() + ")");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2b3674;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (type.equals("outgoing")) {
                Label status = new Label("Pending");
                status.getStyleClass().add("tag-orange");
                row.getChildren().addAll(nameLabel, spacer, status);
            }

            container.getChildren().add(row);
        }
    }

    private void populateMyTournamentsList(VBox container, List<Team> teams, Student currentUser) {
        if (container == null) return;
        container.getChildren().clear();

        if (teams != null && !teams.isEmpty() && myTournamentsCard != null) {
            myTournamentsCard.setVisible(true);
            myTournamentsCard.setManaged(true);
        }

        if (teams == null || teams.isEmpty()) {
            container.getChildren().add(new Label("No active tournaments."));
            return;
        }

        for (Team t : teams) {
            boolean isCaptain = t.getCaptain() != null && t.getCaptain().getBilkentEmail().equals(currentUser.getBilkentEmail());

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");

            VBox infoBox = new VBox(5);
            
            String tourneyNameStr = (t.getCurrentTournament() != null && t.getCurrentTournament().getTournamentName() != null) 
                                     ? t.getCurrentTournament().getTournamentName() + " | " 
                                     : "";
                                     
            Label nameLabel = new Label(tourneyNameStr + "Team: " + t.getTeamName() + (isCaptain ? " (Captain)" : ""));
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2b3674;");
            
            Label subLabel = new Label("Status: Active | Max Players: " + t.getMaxCapacity() + " | Team Code: " + t.getAccessCode());
            subLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 12px;");
            infoBox.getChildren().addAll(nameLabel, subLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button scheduleBtn = new Button("Schedule");
            scheduleBtn.setPrefHeight(35.0);
            scheduleBtn.setPrefWidth(90.0);
            scheduleBtn.getStyleClass().add("btn-secondary");
            scheduleBtn.setOnAction(e -> openScheduleView(t));

            Button viewEditBtn = new Button(isCaptain ? "Edit Team" : "View Team");
            viewEditBtn.setPrefHeight(35.0);
            viewEditBtn.setPrefWidth(90.0);
            viewEditBtn.getStyleClass().add(isCaptain ? "btn-warning" : "btn-success");
            HBox.setMargin(viewEditBtn, new Insets(0, 0, 0, 10));
            viewEditBtn.setOnAction(e -> openTeamEditView(t));

            Button actionBtn = new Button(isCaptain ? "Cancel" : "Leave");
            actionBtn.setPrefHeight(35.0);
            actionBtn.setPrefWidth(90.0);
            actionBtn.getStyleClass().add("btn-danger");
            HBox.setMargin(actionBtn, new Insets(0, 0, 0, 10));
            actionBtn.setOnAction(e -> handleCancelOrLeaveTournament(t, isCaptain, currentUser));

            row.getChildren().addAll(infoBox, spacer, scheduleBtn, viewEditBtn, actionBtn);

            container.getChildren().add(row);
        }
    }

    private void handleCancelOrLeaveTournament(Team t, boolean isCaptain, Student currentUser) {
        new Thread(() -> {
            try {
                DbStatus status;
                if (isCaptain) {
                    status = tournamentManager.withdrawTeam(t.getTeamId());
                } else {
                    status = tournamentManager.leaveTeam(t.getTeamId(), currentUser);
                }
                
                Platform.runLater(() -> {
                    if(status == DbStatus.SUCCESS) {
                        loadTeamManagementData();
                        String msg = isCaptain ? "Team withdrawn successfully." : "You left the team successfully.";
                        showAlert(Alert.AlertType.INFORMATION, "Success", msg);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Operation failed.");
                    }
                });
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

   private void openScheduleView(Team t) {
        try {
            java.net.URL fxmlLocation = getClass().getResource("/views/dashboard/TournamentScheduleView.fxml");
            if (fxmlLocation == null) {
                showAlert(Alert.AlertType.ERROR, "Yol Hatası", "TournamentScheduleView.fxml dosyası bulunamadı!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            
            TournamentScheduleController controller = loader.getController();
            controller.setTeam(t);

            Stage stage = new Stage();
            stage.setTitle("Tournament Schedule");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch(Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Kritik Hata", "Ekran yüklenirken hata oluştu: " + ex.toString());
        }
    }

    private void openTeamEditView(Team t) {
        try {
            java.net.URL fxmlLocation = getClass().getResource("/views/dashboard/TeamEditView.fxml");
            if (fxmlLocation == null) {
                showAlert(Alert.AlertType.ERROR, "Yol Hatası", "TeamEditView.fxml dosyası bulunamadı!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            
            TeamEditController controller = loader.getController();
            controller.setTeam(t);

            Stage stage = new Stage();
            stage.setTitle("Team Info");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 900, 650));
            stage.show();
            
        } catch(Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Kritik Hata", "Ekran yüklenirken hata oluştu: " + ex.toString());
        }
    }

    private void handleTeamRequestAction(Team t, String action) {
        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                DbStatus status;
                
                if (action.equals("accept")) {
                    status = tournamentManager.acceptTeamInvite(t.getTeamId(), currentUser);
                } else {
                    status = tournamentManager.rejectTeamInvite(t.getTeamId(), currentUser);
                }

                final DbStatus finalStatus = status;
                Platform.runLater(() -> {
                    if (finalStatus == DbStatus.SUCCESS) {
                        loadTeamManagementData(); 
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Team request " + action + "ed successfully.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Action could not be completed.");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private HBox createTournamentRow(Tournament tournament) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dateStr = tournament.getStartDate().format(dtf) + " - " + tournament.getEndDate().format(dtf);
        String infoText = tournament.getTournamentName() + "   |   " + dateStr + "   |   Max " + tournament.getMaxPlayersPerTeam() + " players   |   Tournament Code: " + tournament.getAccessCode();

        Label infoLabel = new Label(infoText);
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2b3674;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button applyButton = new Button("Apply");
        applyButton.setPrefHeight(35.0);
        applyButton.setPrefWidth(100.0);
        applyButton.getStyleClass().add("btn-primary");

        applyButton.setOnAction(e -> {
            Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
            
            TextInputDialog nameDialog = new TextInputDialog(currentUser.getFullName() + " Team");
            nameDialog.setTitle("Team Name Request");
            nameDialog.setHeaderText("Create a new team for " + tournament.getTournamentName());
            nameDialog.setContentText("Enter your team name:");
            
            Optional<String> nameResult = nameDialog.showAndWait();
            
            if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
                String chosenTeamName = nameResult.get().trim();
                
                applyButton.setText("Joining...");
                applyButton.setDisable(true);

                new Thread(() -> {
                    DbStatus status = DbStatus.QUERY_ERROR;
                    try {
                        status = tournamentManager.createTeamAndJoinTournament(currentUser, tournament, chosenTeamName);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }

                    final DbStatus finalStatus = status;
                    Platform.runLater(() -> {
                        if(finalStatus == DbStatus.SUCCESS) {
                            SessionManager.getInstance().setTournamentApplied(true);
                            if (myTournamentsCard != null) {
                                myTournamentsCard.setVisible(true);
                                myTournamentsCard.setManaged(true);
                            }
                            loadUpcomingTournaments();
                            loadTeamManagementData();
                            showAlert(Alert.AlertType.INFORMATION, "Successful", "Team '" + chosenTeamName + "' created and joined!");
                        } else {
                            applyButton.setText("Apply");
                            applyButton.setDisable(false);
                            showAlert(Alert.AlertType.ERROR, "Error", "Tournament participation failed. You might already be in a team.");
                        }
                    });
                }).start();
            } else if (nameResult.isPresent()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Name", "Team name cannot be empty!");
            }
        });

        row.getChildren().addAll(infoLabel, spacer, applyButton);
        return row;
    }

    @FXML
    public void handleApplyWithCode(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Team via Code");
        dialog.setHeaderText("Enter the 6-digit Team Access Code");
        dialog.setContentText("Team Code:");

        if (applyCodeButton != null && applyCodeButton.getScene() != null) {
            dialog.initOwner(applyCodeButton.getScene().getWindow());
        }

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String tCode = input.trim();
            if (tCode.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please enter the Team Code.");
                return;
            }

            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                    status = tournamentManager.joinTeamWithCode(currentUser, tCode);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    if (finalStatus == DbStatus.SUCCESS) {
                        SessionManager.getInstance().setTournamentJoinedWithCode(true);
                        if (myTournamentsCard != null) {
                            myTournamentsCard.setVisible(true);
                            myTournamentsCard.setManaged(true);
                        }
                        if (applyCodeButton != null) {
                            applyCodeButton.setText("Joined");
                            applyCodeButton.setDisable(true);
                        }
                        loadTeamManagementData();
                        showAlert(Alert.AlertType.INFORMATION, "Successful", "You have joined the team.");
                    } else if (finalStatus == DbStatus.DATA_NOT_FOUND) {
                        showAlert(Alert.AlertType.ERROR, "Failed", "Team not found with this code.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Failed", "Invalid Team Code or you are already in a team.");
                    }
                });
            }).start();
        });
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