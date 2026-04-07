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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import managers.TournamentManager;
import models.Tournament;

public class AdminHomeController {

    @FXML private VBox tournamentsBox;
    @FXML private TextField titleField;
    @FXML private TextArea messageArea;
    @FXML private TextArea emailsArea;
    @FXML private CheckBox broadcastCheckBox;
    @FXML private Button sendBtn;

    private Database db = Database.getInstance();
    private TournamentManager tManager = new TournamentManager(db);
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadTournaments();

        broadcastCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            emailsArea.setDisable(newVal);
            if (newVal) {
                emailsArea.setPromptText("Will be sent to all students in the system (Broadcast active)");
                emailsArea.clear();
            } else {
                emailsArea.setPromptText("E.g., ali@ug.bilkent.edu.tr, veli@ug.bilkent.edu.tr (Separate emails with commas)");
            }
        });
    }

    private void loadTournaments() {
        if (tournamentsBox == null) return;
        
        tournamentsBox.getChildren().clear();
        Label loadingLabel = new Label("Loading tournaments...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
        tournamentsBox.getChildren().add(loadingLabel);

        new Thread(() -> {
            try {
                List<Tournament> tournaments = tManager.getAllActiveTournaments();

                Platform.runLater(() -> {
                    tournamentsBox.getChildren().clear();

                    if (tournaments == null || tournaments.isEmpty()) {
                        Label emptyLabel = new Label("No upcoming tournaments.");
                        emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                        tournamentsBox.getChildren().add(emptyLabel);
                    } else {
                        tournaments.sort(Comparator.comparing(Tournament::getStartDate));
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                        boolean isFirst = true;

                        for (Tournament t : tournaments) {
                            String details = t.getTournamentName() + "  |  " + 
                                             t.getStartDate().format(dtf) + " - " + t.getEndDate().format(dtf) + "  |  " + 
                                             "Max " + t.getMaxPlayersPerTeam() + " players  |  Ge250: " + (t.isHasGe250() ? "Yes" : "No");

                            HBox row = new HBox(15);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-background-color: #FFFFFF;");
                            row.setPadding(new Insets(12, 15, 12, 15));

                            if (isFirst) {
                                Label newTag = new Label("⭐ NEXT EVENT");
                                newTag.setStyle("-fx-background-color: #FFF4E5; -fx-text-fill: #FF9120; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 8; -fx-font-size: 11px;");
                                row.getChildren().add(newTag);
                                row.setStyle("-fx-border-color: #FF9120; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-background-color: #FFFFFF;");
                                isFirst = false;
                            }

                            Label tLabel = new Label(details);
                            tLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B3674; -fx-font-size: 13px;");
                            
                            row.getChildren().add(tLabel);
                            tournamentsBox.getChildren().add(row);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleSendNotification(ActionEvent event) {
        if (isProcessing) return;

        String title = titleField.getText();
        String message = messageArea.getText();
        boolean isBroadcast = broadcastCheckBox.isSelected();
        String emailsText = emailsArea.getText();

        if (title == null || title.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            showCustomAlert("Warning", "Title and message fields cannot be left empty.");
            return;
        }

        if (!isBroadcast && (emailsText == null || emailsText.trim().isEmpty())) {
            showCustomAlert("Warning", "Please enter at least one email address or check the 'Send to All Students (Broadcast)' option.");
            return;
        }

        isProcessing = true;
        sendBtn.setText("Sending...");
        sendBtn.setDisable(true);

        new Thread(() -> {
            int successCount = 0;
            int failCount = 0;
            StringBuilder failEmails = new StringBuilder();

            if (isBroadcast) {
                DbStatus status = db.insertNotification("BROADCAST", title.trim(), message.trim());
                if (status == DbStatus.SUCCESS) successCount++;
                else failCount++;
            } else {
                String[] emails = emailsText.split(",");
                for (String email : emails) {
                    String cleanEmail = email.trim();
                    if (!cleanEmail.isEmpty()) {
                        DbStatus status = db.insertNotification(cleanEmail, title.trim(), message.trim());
                        if (status == DbStatus.SUCCESS) {
                            successCount++;
                        } else {
                            failCount++;
                            failEmails.append(cleanEmail).append("\n");
                        }
                    }
                }
            }

            final int finalSuccess = successCount;
            final int finalFail = failCount;
            final String finalFailStr = failEmails.toString();

            Platform.runLater(() -> {
                isProcessing = false;
                sendBtn.setText("Send Notification");
                sendBtn.setDisable(false);

                if (isBroadcast) {
                    if (finalSuccess > 0) {
                        showCustomAlert("Success", "Broadcast (General Announcement) successfully sent to all users.");
                        titleField.clear();
                        messageArea.clear();
                    } else {
                        showCustomAlert("Error", "An error occurred while sending the broadcast.");
                    }
                } else {
                    if (finalFail == 0 && finalSuccess > 0) {
                        showCustomAlert("Success", "Notification successfully sent to " + finalSuccess + " users!");
                        titleField.clear();
                        messageArea.clear();
                        emailsArea.clear();
                    } else if (finalSuccess > 0 && finalFail > 0) {
                        showCustomAlert("Partial Success", finalSuccess + " users successfully received the notification, but the following users could not be reached (User not found):\n" + finalFailStr);
                    } else {
                        showCustomAlert("Error", "No notifications were sent. Please ensure the email addresses you entered are registered in the system.");
                    }
                }
            });
        }).start();
    }

    private void showCustomAlert(String title, String message) {
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

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        okBtn.setOnAction(e -> dialogStage.close());

        layout.getChildren().addAll(titleLabel, msgLabel, okBtn);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }
}