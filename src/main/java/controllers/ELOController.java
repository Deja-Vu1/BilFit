package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import managers.DuelloManager;
import managers.ReservationManager;
import managers.SessionManager;
import models.Duello;
import models.Reservation;
import models.SportType;
import models.Student;

public class ELOController {

    @FXML private VBox reservationsContainer;
    @FXML private VBox myDuellosContainer;
    @FXML private VBox duellosContainer;
    @FXML private VBox incomingRequestsContainer;
    @FXML private ComboBox<String> sportTypeComboBox;

    private DuelloManager duelloManager = new DuelloManager(Database.getInstance());
    private ReservationManager resManager = new ReservationManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        if (sportTypeComboBox != null) {
            sportTypeComboBox.getItems().clear();
            for (SportType sport : SportType.values()) {
                sportTypeComboBox.getItems().add(sport.toString());
            }
            sportTypeComboBox.getSelectionModel().selectFirst();
        }
        loadEloAndDuelloData();
    }

    private void loadEloAndDuelloData() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String selectedSport = sportTypeComboBox != null && sportTypeComboBox.getValue() != null ? sportTypeComboBox.getValue() : "BASKETBALL";

        new Thread(() -> {
            try {
                ArrayList<Reservation> dbReservations = resManager.getUserReservations(currentUser);
                ArrayList<Duello> myDuellos = duelloManager.getUserDuellos(currentUser);
                ArrayList<Duello> suitableDuellos = duelloManager.findOpponentForMatch(currentUser, selectedSport);

                
                Map<String, Student> duelloWinners = new HashMap<>();
                if (myDuellos != null) {
                    for (Duello d : myDuellos) {
                        if (d.isMatched()) {
                            Student winner = Database.getInstance().getWinnerOfDuello(d.getReservationId());
                            if (winner != null) {
                                duelloWinners.put(d.getReservationId(), winner);
                            }
                        }
                    }
                }
                

                ArrayList<Reservation> validReservations = new ArrayList<>();
                if (dbReservations != null) {
                    for (Reservation res : dbReservations) {
                        if (!res.isCancelled()) {
                            boolean isAlreadyDuello = false;
                            for (Duello d : myDuellos) {
                                if (d.getReservationId().equals(res.getReservationId())) {
                                    isAlreadyDuello = true;
                                    break;
                                }
                            }
                            if (!isAlreadyDuello) {
                                validReservations.add(res);
                            }
                        }
                    }
                }

                ArrayList<HBox> requestRows = new ArrayList<>();
                if (myDuellos != null) {
                    for (Duello d : myDuellos) {
                        if (!d.isMatched() && !d.isCancelled()) {
                            ArrayList<Student> requesters = duelloManager.getPendingRequestsForDuello(d.getReservationId());
                            if (requesters != null) {
                                for (Student requester : requesters) {
                                    requestRows.add(createIncomingRequestRow(d, requester));
                                }
                            }
                        }
                    }
                }

                Platform.runLater(() -> {
                    if (reservationsContainer != null) {
                        reservationsContainer.getChildren().clear();
                        if (!validReservations.isEmpty()) {
                            for (Reservation res : validReservations) {
                                reservationsContainer.getChildren().add(createReservationRow(res));
                            }
                        } else {
                            Label emptyLabel = new Label("You have no active normal reservations.");
                            emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            reservationsContainer.getChildren().add(emptyLabel);
                        }
                    }

                    if (myDuellosContainer != null) {
                        myDuellosContainer.getChildren().clear();
                        if (myDuellos != null && !myDuellos.isEmpty()) {
                            boolean hasActiveDuello = false;
                            for (Duello d : myDuellos) {
                                if (!d.isCancelled()) {
                                    
                                    Student winner = duelloWinners.get(d.getReservationId());
                                    myDuellosContainer.getChildren().add(createMyDuelloRow(d, winner));
                                    hasActiveDuello = true;
                                }
                            }
                            if (!hasActiveDuello) {
                                Label emptyMyDuellosLabel = new Label("You have not created or joined any active duello yet.");
                                emptyMyDuellosLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                                myDuellosContainer.getChildren().add(emptyMyDuellosLabel);
                            }
                        } else {
                            Label emptyMyDuellosLabel = new Label("You have not created or joined any duello yet.");
                            emptyMyDuellosLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            myDuellosContainer.getChildren().add(emptyMyDuellosLabel);
                        }
                    }

                    if (incomingRequestsContainer != null) {
                        incomingRequestsContainer.getChildren().clear();
                        if (!requestRows.isEmpty()) {
                            incomingRequestsContainer.getChildren().addAll(requestRows);
                        } else {
                            Label emptyReqLabel = new Label("You have no pending incoming requests at the moment.");
                            emptyReqLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            incomingRequestsContainer.getChildren().add(emptyReqLabel);
                        }
                    }

                    if (duellosContainer != null) {
                        duellosContainer.getChildren().clear();
                        if (suitableDuellos != null && !suitableDuellos.isEmpty()) {
                            for (Duello d : suitableDuellos) {
                                duellosContainer.getChildren().add(createAvailableDuelloRow(d));
                            }
                        } else {
                            Label emptyDuelloLabel = new Label("There is currently no open duello suitable for your level.");
                            emptyDuelloLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            duellosContainer.getChildren().add(emptyDuelloLabel);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleFindDuello(ActionEvent event) {
        String selectedSport = sportTypeComboBox.getValue();
        if (selectedSport != null) {
            loadEloAndDuelloData();
        }
    }

    private HBox createReservationRow(Reservation res) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 10));

        String facilityName = res.getFacility() != null ? res.getFacility().getName() : "Saha";
        String resText = res.getFacility().getCampusLocation() + "   |   " + facilityName + "   |   " + res.getDate() + "   |   " + res.getTimeSlot();

        Label infoLabel = new Label(resText);
        infoLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button createBtn = new Button("Create A Duello");
        createBtn.setPrefHeight(35.0);
        createBtn.setPrefWidth(130.0);
        createBtn.getStyleClass().add("btn-success");
        createBtn.setOnAction(e -> handleCreateSpecificDuello(res, createBtn));

        row.getChildren().addAll(infoLabel, spacer, createBtn);
        return row;
    }

    private HBox createMyDuelloRow(Duello duello, Student winner) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15;");
        row.setPadding(new Insets(10, 20, 10, 20));

        VBox infoBox = new VBox(5);
        
        String facilityName = duello.getFacility() != null ? duello.getFacility().getName() : "Saha";
        String loc = duello.getFacility() != null ? duello.getFacility().getCampusLocation() : "Kampüs";
        String mainText = loc + "   |   " + facilityName + "   |   " + duello.getDate() + " " + duello.getTimeSlot();
        
        
        String statusStr = "Waiting for Opponent";
        if (winner != null) {
            statusStr = "Concluded";
        } else if (duello.isMatched()) {
            statusStr = "Match Ready";
        }
        
        String subText = "Access Code: " + duello.getAccessCode() + "   |   Status: " + statusStr;

        Label mainLabel = new Label(mainText);
        mainLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(mainLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        
        if (winner != null) {
            Label winnerLabel = new Label("🏆 Winner: " + winner.getFullName());
            winnerLabel.setStyle("-fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #E6F4EA; -fx-padding: 8 15 8 15; -fx-background-radius: 8;");
            row.getChildren().addAll(infoBox, spacer, winnerLabel);
        } else {
            
            Button actionBtn = new Button();
            actionBtn.setPrefHeight(35.0);
            actionBtn.setPrefWidth(110.0);

            if (duello.isMatched()) {
                actionBtn.setText("Cancel Match");
                actionBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
                actionBtn.setOnAction(e -> handleCancelSpecificDuello(duello, actionBtn));
            } else {
                actionBtn.setText("Cancel Duello");
                actionBtn.setStyle("-fx-background-color: #E2ECF6; -fx-text-fill: #4318FF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
                actionBtn.setOnAction(e -> handleCancelSpecificDuello(duello, actionBtn));
            }

            row.getChildren().addAll(infoBox, spacer, actionBtn);
        }

        return row;
    }

    private HBox createIncomingRequestRow(Duello duello, Student requester) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FEF4F4; -fx-border-color: #FFB547; -fx-border-radius: 15; -fx-background-radius: 15;");
        row.setPadding(new Insets(10, 20, 10, 20));

        VBox infoBox = new VBox(5);
        String mainText = "Requester: " + requester.getFullName() + " (ELO: " + requester.getEloPoint() + ")";
        String facilityName = duello.getFacility() != null ? duello.getFacility().getName() : "Saha";
        String subText = "Match: " + facilityName + " | " + duello.getDate() + " " + duello.getTimeSlot();

        Label mainLabel = new Label(mainText);
        mainLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(mainLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button acceptBtn = new Button("Accept");
        acceptBtn.setPrefHeight(30.0);
        acceptBtn.setPrefWidth(80.0);
        acceptBtn.setStyle("-fx-background-color: #05CD99; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        acceptBtn.setOnAction(e -> handleAcceptRequest(duello, requester, acceptBtn));

        Button rejectBtn = new Button("Reject");
        rejectBtn.setPrefHeight(30.0);
        rejectBtn.setPrefWidth(80.0);
        rejectBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> handleRejectRequest(duello, requester, rejectBtn));

        HBox btnBox = new HBox(10, acceptBtn, rejectBtn);
        row.getChildren().addAll(infoBox, spacer, btnBox);
        return row;
    }

    private HBox createAvailableDuelloRow(Duello duello) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FCE8E8; -fx-border-color: #D93025; -fx-border-radius: 15; -fx-background-radius: 15;");
        row.setPadding(new Insets(10, 20, 10, 20));

        VBox infoBox = new VBox(5);
        
        String facilityName = duello.getFacility() != null ? duello.getFacility().getName() : "Saha";
        String loc = duello.getFacility() != null ? duello.getFacility().getCampusLocation() : "Kampüs";
        String mainText = loc + "   |   " + facilityName + "   |   Max " + (duello.getFacility() != null ? duello.getFacility().getCapacity() : "0") + " player   |   " + duello.getDate() + " / " +duello.getTimeSlot();
        String subText = "Empty Slots: " + duello.getEmptySlots() + "   |   Skill: " + duello.getRequiredSkillLevel();

        Label mainLabel = new Label(mainText);
        mainLabel.setStyle("-fx-text-fill: #D93025; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-text-fill: #D93025; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(mainLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label iconLabel = new Label("🔥");
        iconLabel.setStyle("-fx-font-size: 24;");
        HBox.setMargin(iconLabel, new Insets(0, 15, 0, 0));

        Button requestBtn = new Button("Request");
        requestBtn.setPrefHeight(35.0);
        requestBtn.setPrefWidth(100.0);
        requestBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        requestBtn.setOnAction(e -> handleRequestSpecificDuello(duello, requestBtn));

        row.getChildren().addAll(infoBox, spacer, iconLabel, requestBtn);
        return row;
    }

    private void handleAcceptRequest(Duello duello, Student requester, Button btn) {
        if (isProcessing) return;
        isProcessing = true;
        String originalText = btn.getText();
        btn.setDisable(true);
        btn.setText("...");

        new Thread(() -> {
            DbStatus status = duelloManager.acceptDuelloRequest(duello, requester);
            Platform.runLater(() -> {
                isProcessing = false;
                if (status == DbStatus.SUCCESS) {
                    showAlert(Alert.AlertType.INFORMATION, "Successful", "You accepted the request. Match completed!");
                    loadEloAndDuelloData();
                } else {
                    btn.setDisable(false);
                    btn.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Error", "Request could not be accepted.");
                }
            });
        }).start();
    }

    private void handleRejectRequest(Duello duello, Student requester, Button btn) {
        if (isProcessing) return;
        isProcessing = true;
        String originalText = btn.getText();
        btn.setDisable(true);
        btn.setText("...");

        new Thread(() -> {
            DbStatus status = duelloManager.declineDuelloRequest(duello, requester);
            Platform.runLater(() -> {
                isProcessing = false;
                if (status == DbStatus.SUCCESS) {
                    loadEloAndDuelloData();
                } else {
                    btn.setDisable(false);
                    btn.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Error", "Request could not be rejected.");
                }
            });
        }).start();
    }

    private void handleCreateSpecificDuello(Reservation targetRes, Button clickedButton) {
        if (isProcessing) return;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Creating...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                Duello newDuello = new Duello(targetRes.getReservationId(), targetRes.getFacility(), targetRes.getDate(), targetRes.getTimeSlot(), "CODE123", "Mid-Level", 1);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    status = duelloManager.createDuello(newDuello, currentUser);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (finalStatus == DbStatus.SUCCESS) {
                        showAlert(Alert.AlertType.INFORMATION, "Successful", "Your reservation has been successfully converted to a Duello!");
                        loadEloAndDuelloData();
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Error", "Duello could not be created. Please try again.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    private void handleCancelSpecificDuello(Duello duello, Button clickedButton) {
        if (isProcessing) return;
        
        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Processing...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                DbStatus status = DbStatus.QUERY_ERROR;
                
                boolean isCreator = false;
                if (duello.getAttendees() != null && !duello.getAttendees().isEmpty()) {
                    Student creator = duello.getAttendees().get(0);
                    if (creator.getBilkentEmail().equals(currentUser.getBilkentEmail())) {
                        isCreator = true;
                    }
                }

                try {
                    if (isCreator) {
                        status = duelloManager.cancelDuello(duello, currentUser);
                    } else {
                        status = duelloManager.leaveDuello(duello, currentUser);
                    }
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;
                final boolean wasCreator = isCreator;

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (finalStatus == DbStatus.SUCCESS) {
                        String msg = wasCreator ? "Duello has been successfully cancelled and converted back to a normal reservation." 
                                                : "You have successfully left the duello. Match is cancelled.";
                        showAlert(Alert.AlertType.INFORMATION, "Success", msg);
                        loadEloAndDuelloData();
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Error", "Action could not be completed.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    private void handleRequestSpecificDuello(Duello targetDuello, Button clickedButton) {
        if (isProcessing) return;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Sending...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    status = duelloManager.requestToJoinDuello(targetDuello, currentUser);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (finalStatus == DbStatus.SUCCESS) { 
                        clickedButton.setText("Requested");
                        showAlert(Alert.AlertType.INFORMATION, "Request Sent", "Your request to join this match has been successfully sent to the creator.");
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Error", "Request could not be sent.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    @FXML
    public void handleApplyWithCode(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Private Duello");
        dialog.setHeaderText("Enter Duello Code");
        dialog.setContentText("Code:");
        try { dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception e) {}

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Code field cannot be left empty.");
                return;
            }
            new Thread(() -> {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                DbStatus status = DbStatus.QUERY_ERROR;
                
                try { 
                    status = duelloManager.joinDuelloWithCode(code.trim(), currentUser); 
                } catch (Exception ex) { ex.printStackTrace(); }
                
                final DbStatus finalStatus = status;
                Platform.runLater(() -> {
                    if (finalStatus == DbStatus.SUCCESS) { 
                         showAlert(Alert.AlertType.INFORMATION, "Operation Successful", "You have successfully joined the duello with the code!");
                         loadEloAndDuelloData();
                    } else if (finalStatus == DbStatus.DATA_NOT_FOUND) {
                         showAlert(Alert.AlertType.ERROR, "Error", "No open duello found for the code you entered.");
                    } else {
                         showAlert(Alert.AlertType.ERROR, "Error", "Invalid code, ELO mismatch, or you are trying to join your own match.");
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
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception e) {}
        if (reservationsContainer != null && reservationsContainer.getScene() != null) {
            alert.initOwner(reservationsContainer.getScene().getWindow());
        }
        alert.showAndWait();
    }
}