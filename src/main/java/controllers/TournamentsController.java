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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        String inactiveStyle = "-fx-background-color: #E2E8F0; -fx-text-fill: #2b3674; -fx-background-radius: 8; -fx-font-weight: normal;";
        String activeStyle = "-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;";
        
        if(btnIncoming != null) btnIncoming.setStyle(inactiveStyle);
        if(btnOutgoing != null) btnOutgoing.setStyle(inactiveStyle);
        
        if(activeBtn != null) activeBtn.setStyle(activeStyle);
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
                    populateMyTournamentsList(myTournamentsContainer, myTeams, currentUser); // currentUser eklendi
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
            container.getChildren().add(new Label("No incoming requests."));
            return;
        }

        for (Team t : teams) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: #FFFFFF;");
            row.setPadding(new Insets(10));

            Label nameLabel = new Label("Team: " + t.getTeamName() + " (Captain: " + t.getCaptain().getFullName() + ")");
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12.0));
            nameLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button acceptBtn = new Button("Accept");
            acceptBtn.setStyle("-fx-background-color: #05CD99; -fx-text-fill: white; -fx-background-radius: 5;");
            acceptBtn.setOnAction(e -> handleTeamRequestAction(t, "accept"));

            Button rejectBtn = new Button("Reject");
            rejectBtn.setStyle("-fx-background-color: #EE5D50; -fx-text-fill: white; -fx-background-radius: 5;");
            rejectBtn.setOnAction(e -> handleTeamRequestAction(t, "reject"));
            
            HBox btnBox = new HBox(5, acceptBtn, rejectBtn);
            row.getChildren().addAll(nameLabel, spacer, btnBox);

            container.getChildren().add(row);
        }
    }
    
    private void populateStudentRequestList(VBox container, List<Student> students, String type) {
        if (container == null) return;
        container.getChildren().clear();

        if (students == null || students.isEmpty()) {
            container.getChildren().add(new Label("No records found."));
            return;
        }

        for (Student s : students) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: #FFFFFF;");
            row.setPadding(new Insets(10));

            Label nameLabel = new Label(s.getFullName() + " (" + s.getBilkentEmail() + ")");
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12.0));
            nameLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (type.equals("outgoing")) {
                Label status = new Label("Pending");
                status.setTextFill(javafx.scene.paint.Color.web("#FFB547"));
                row.getChildren().addAll(nameLabel, spacer, status);
            }

            container.getChildren().add(row);
        }
    }

    private void populateMyTournamentsList(VBox container, List<Team> teams, Student currentUser) {
        if (container == null) return;
        container.getChildren().clear();

        // Eğer takım varsa myTournamentsCard'ı görünür yap
        if (teams != null && !teams.isEmpty() && myTournamentsCard != null) {
            myTournamentsCard.setVisible(true);
            myTournamentsCard.setManaged(true);
        }

        if (teams == null || teams.isEmpty()) {
            container.getChildren().add(new Label("No active tournaments."));
            return;
        }

        for (Team t : teams) {
            // Kullanıcı bu takımın kaptanı mı kontrolü
            boolean isCaptain = t.getCaptain() != null && t.getCaptain().getBilkentEmail().equals(currentUser.getBilkentEmail());

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
            row.setPadding(new Insets(10, 20, 10, 20));

            VBox infoBox = new VBox(5);
            Label nameLabel = new Label("Team: " + t.getTeamName() + (isCaptain ? " (Captain)" : ""));
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13.0));
            nameLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));
            
            Label subLabel = new Label("Status: Active | Max Players: " + t.getMaxCapacity());
            subLabel.setStyle("-fx-text-fill: #a0aec0;");
            infoBox.getChildren().addAll(nameLabel, subLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button scheduleBtn = new Button("Schedule");
            scheduleBtn.setPrefHeight(35.0);
            scheduleBtn.setPrefWidth(90.0);
            scheduleBtn.getStyleClass().add("btn-secondary");
            scheduleBtn.setOnAction(e -> openScheduleView(t));

            Button actionBtn = new Button(isCaptain ? "Cancel" : "Leave");
            actionBtn.setPrefHeight(35.0);
            actionBtn.setPrefWidth(90.0);
            actionBtn.getStyleClass().add("btn-danger");
            HBox.setMargin(actionBtn, new Insets(0, 0, 0, 10));
            actionBtn.setOnAction(e -> handleCancelOrLeaveTournament(t, isCaptain, currentUser));

            // Sadece kaptansa Edit butonunu satıra ekle
            if (isCaptain) {
                Button editTeamBtn = new Button("Edit Team");
                editTeamBtn.setPrefHeight(35.0);
                editTeamBtn.setPrefWidth(90.0);
                editTeamBtn.setStyle("-fx-background-color: #FFB547; -fx-text-fill: white; -fx-background-radius: 8;");
                HBox.setMargin(editTeamBtn, new Insets(0, 0, 0, 10));
                editTeamBtn.setOnAction(e -> openTeamEditView(t));
                
                row.getChildren().addAll(infoBox, spacer, scheduleBtn, editTeamBtn, actionBtn);
            } else {
                row.getChildren().addAll(infoBox, spacer, scheduleBtn, actionBtn);
            }

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/TournamentScheduleView.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Tournament Schedule");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openTeamEditView(Team t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/TeamEditView.fxml"));
            Parent root = loader.load();
            
            TeamEditController controller = loader.getController();
            controller.setTeam(t);

            Stage stage = new Stage();
            stage.setTitle("Edit Team");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 800, 600));
            stage.show();
        } catch(Exception ex) {
            ex.printStackTrace();
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
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 20));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dateStr = tournament.getStartDate().format(dtf) + " - " + tournament.getEndDate().format(dtf);
        String infoText = tournament.getTournamentName() + "   |   " + dateStr + "   |   Max " + tournament.getMaxPlayersPerTeam() + " player";

        Label infoLabel = new Label(infoText);
        infoLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));
        infoLabel.setFont(Font.font("System", FontWeight.BOLD, 13.0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button applyButton = new Button("Apply");
        applyButton.setPrefHeight(35.0);
        applyButton.setPrefWidth(100.0);
        applyButton.getStyleClass().add("btn-secondary");

        applyButton.setOnAction(e -> {
            applyButton.setText("Joining...");
            applyButton.setDisable(true);

            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                    String generatedTeamName = currentUser.getFullName() + " Team";
                    status = tournamentManager.createTeamAndJoinTournament(currentUser, tournament, generatedTeamName);
                } catch(Exception ex) {}

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
                        showAlert(Alert.AlertType.INFORMATION, "Successful", "You have joined the tournament successfully.");
                    } else {
                        applyButton.setText("Apply");
                        applyButton.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Error", "Tournament participation failed.");
                    }
                });
            }).start();
        });

        row.getChildren().addAll(infoLabel, spacer, applyButton);
        return row;
    }

    @FXML
    public void handleApplyWithCode(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Team via Code");
        dialog.setHeaderText("Enter the Team ID and Access Code");
        dialog.setContentText("Format (TeamID,Code):");

        if (applyCodeButton != null && applyCodeButton.getScene() != null) {
            dialog.initOwner(applyCodeButton.getScene().getWindow());
        }

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            if (input.trim().isEmpty() || !input.contains(",")) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please enter in TeamID,Code format.");
                return;
            }
            
            String[] parts = input.split(",");
            String tId = parts[0].trim();
            String tCode = parts[1].trim();

            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                    status = tournamentManager.joinTeamWithCode(tId, currentUser, tCode);
                } catch (Exception ex) {}

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
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Failed", "Invalid Team ID or Code.");
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