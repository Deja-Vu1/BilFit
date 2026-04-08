package controllers;

import java.util.ArrayList;
import java.util.List;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import managers.AdminManager;
import managers.SessionManager;
import models.Admin;
import models.Reservation;
import models.Student;

public class AdminReservationController {

    @FXML private VBox reservationsContainer;
    @FXML private TextField searchField;

    private Database db = Database.getInstance();
    private AdminManager adminManager = new AdminManager(db);
    private List<Reservation> allReservations = new ArrayList<>();
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadReservations();
    }

    private void loadReservations() {
        if (reservationsContainer == null) return;
        reservationsContainer.getChildren().clear();
        Label loadingLabel = new Label("Loading reservations...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
        reservationsContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            allReservations = db.getAllReservations();
            Platform.runLater(() -> renderReservations(allReservations));
        }).start();
    }

    private void renderReservations(List<Reservation> listToRender) {
        reservationsContainer.getChildren().clear();

        if (listToRender == null || listToRender.isEmpty()) {
            Label emptyLabel = new Label("No reservations found.");
            emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
            reservationsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Reservation res : listToRender) {
            reservationsContainer.getChildren().add(createReservationCard(res));
        }
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            renderReservations(allReservations);
            return;
        }

        List<Reservation> filteredList = new ArrayList<>();
        for (Reservation res : allReservations) {
            String orgEmail = res.getOrganizer() != null ? res.getOrganizer().getBilkentEmail().toLowerCase() : "";
            if (orgEmail.contains(query)) {
                filteredList.add(res);
            }
        }
        renderReservations(filteredList);
    }

    private HBox createReservationCard(Reservation res) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        card.setPadding(new Insets(15, 20, 15, 20));

        VBox infoBox = new VBox(5);
        Label facilityLabel = new Label(res.getFacility().getName());
        facilityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2B3674;");
        
        Label dateLabel = new Label(res.getDate().toString() + " | " + res.getTimeSlot());
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #4318FF;");

        String orgEmail = res.getOrganizer() != null ? res.getOrganizer().getBilkentEmail() : "Unknown";
        Label organizerLabel = new Label("Student: " + orgEmail);
        organizerLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #A3AED0;");

        infoBox.getChildren().addAll(facilityLabel, dateLabel, organizerLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (res.isHasAttended()) {
            Label attendedLabel = new Label("Attended / Concluded");
            attendedLabel.setStyle("-fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-background-color: #E6F4EA; -fx-padding: 8 15 8 15; -fx-background-radius: 8;");
            card.getChildren().addAll(infoBox, spacer, attendedLabel);
        } else if (res.isCancelled()) {
            Label cancelledLabel = new Label("Cancelled");
            cancelledLabel.setStyle("-fx-text-fill: #D93025; -fx-font-weight: bold; -fx-background-color: #FCE8E8; -fx-padding: 8 15 8 15; -fx-background-radius: 8;");
            card.getChildren().addAll(infoBox, spacer, cancelledLabel);
        } else {
            
            
            Button creatorWinBtn = new Button("Creator Won");
            creatorWinBtn.setStyle("-fx-background-color: #EBF8FF; -fx-text-fill: #2B6CB0; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
            creatorWinBtn.setOnAction(e -> handleCreatorWin(res));

            Button requesterWinBtn = new Button("Requester Won");
            requesterWinBtn.setStyle("-fx-background-color: #FAF5FF; -fx-text-fill: #6B46C1; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
            requesterWinBtn.setOnAction(e -> handleRequesterWin(res));

            
            Button attendBtn = new Button("Mark Attended");
            attendBtn.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
            attendBtn.setOnAction(e -> handleMarkAttended(res));

            Button noShowBtn = new Button("No Show (Penalty)");
            noShowBtn.setStyle("-fx-background-color: #FCE8E8; -fx-text-fill: #D93025; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
            noShowBtn.setOnAction(e -> handleNoShowPenalty(res));

            card.getChildren().addAll(infoBox, spacer, creatorWinBtn, requesterWinBtn, attendBtn, noShowBtn);
        }

        return card;
    }

    private void handleCreatorWin(Reservation res) {
        if (isProcessing) return;
        isProcessing = true;

        new Thread(() -> {
            DbStatus status = db.setCreatorAsWinner(res.getReservationId());
            Platform.runLater(() -> {
                isProcessing = false;
                if (status == DbStatus.SUCCESS) {
                    showCustomAlert("Success", "The Creator has been marked as the winner of this Duello.");
                    db.updateReservationAttendance(res.getReservationId(), true); 
                    loadReservations();
                } else if (status == DbStatus.DATA_NOT_FOUND) {
                    showCustomAlert("Information", "This reservation is not a Duello.");
                } else {
                    showCustomAlert("Error", "Could not update the winner status.");
                }
            });
        }).start();
    }

    private void handleRequesterWin(Reservation res) {
        if (isProcessing) return;
        isProcessing = true;

        new Thread(() -> {
            DbStatus status = db.setRequesterAsWinner(res.getReservationId());
            Platform.runLater(() -> {
                isProcessing = false;
                if (status == DbStatus.SUCCESS) {
                    showCustomAlert("Success", "The Requester has been marked as the winner of this Duello.");
                    db.updateReservationAttendance(res.getReservationId(), true); 
                    loadReservations();
                } else if (status == DbStatus.DATA_NOT_FOUND) {
                    showCustomAlert("Information", "This reservation is not a Duello.");
                } else {
                    showCustomAlert("Error", "Could not update the winner status.");
                }
            });
        }).start();
    }

    private void handleMarkAttended(Reservation res) {
        if (isProcessing) return;
        isProcessing = true;
        
        new Thread(() -> {
            DbStatus status = db.updateReservationAttendance(res.getReservationId(), true);
            Platform.runLater(() -> {
                isProcessing = false;
                if (status == DbStatus.SUCCESS) {
                    loadReservations();
                } else {
                    showCustomAlert("Error", "Could not update attendance status.");
                }
            });
        }).start();
    }

    private void handleNoShowPenalty(Reservation res) {
        if (isProcessing) return;
        isProcessing = true;

        new Thread(() -> {
            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            Student targetStudent = res.getOrganizer();
            
            if (targetStudent != null) {
                adminManager.givePenaltyPoint(currentAdmin, targetStudent, 20);
            }
            
            db.updateReservationAttendance(res.getReservationId(), false);
            db.deleteReservation(res.getReservationId()); 

            Platform.runLater(() -> {
                isProcessing = false;
                loadReservations();
                showCustomAlert("Penalty Issued", "Student received 20 penalty points for no-show.");
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
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
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
        dialog.showAndWait();
    }
}