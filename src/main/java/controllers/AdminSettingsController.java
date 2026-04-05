package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import managers.AdminManager;
import managers.SessionManager;
import models.Admin;
import models.Student;

public class AdminSettingsController {

    @FXML private TextField nameField;
    @FXML private TextField passwordField;
    @FXML private Button changeNameBtn;
    @FXML private Button changePasswordBtn;

    @FXML private TextField studentEmailField;
    @FXML private TextField penaltyPointsField;
    @FXML private Button addPenaltyBtn;
    @FXML private Button reducePenaltyBtn;
    @FXML private Button unbanStudentBtn;
    @FXML private Button banStudentBtn;

    private AdminManager adminManager = new AdminManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
        if (currentAdmin != null) {
            if (nameField != null) {
                nameField.setPromptText("Current: " + currentAdmin.getFullName());
            }
        }
    }

    @FXML
    public void handleAddPenalty(ActionEvent event) {
        processPenaltyModification(true, addPenaltyBtn, "Add Penalty Points");
    }

    @FXML
    public void handleReducePenalty(ActionEvent event) {
        processPenaltyModification(false, reducePenaltyBtn, "Remove Penalty Points");
    }

    private void processPenaltyModification(boolean isAddition, Button clickedBtn, String originalBtnText) {
        if (isProcessing) return;

        String email = studentEmailField.getText();
        String pointsStr = penaltyPointsField.getText();

        if (email == null || email.trim().isEmpty()) {
            showCustomAlert("Warning", "Please enter the email of the student to be processed.");
            return;
        }

        int points = 0;
        try {
            points = Integer.parseInt(pointsStr.trim());
            if (points <= 0) {
                showCustomAlert("Error", "Penalty points must be a positive integer.");
                return;
            }
        } catch (NumberFormatException e) {
            showCustomAlert("Error", "Please enter a valid number in the 'Penalty Points' field.");
            return;
        }

        isProcessing = true;
        clickedBtn.setText("Processing...");

        final int finalPoints = points;
        new Thread(() -> {
            Database db = Database.getInstance();
            Student targetStudent = new Student("", email.trim(), "");
            DbStatus fillStatus = db.fillStudentDataByEmail(targetStudent, email.trim());
            
            if (fillStatus != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedBtn.setText(originalBtnText);
                    showCustomAlert("Not Found", "No student with this email found in the system.");
                });
                return;
            }
            
            int currentPoints = targetStudent.getPenaltyPoints();
            int projectedPoints = isAddition ? (currentPoints + finalPoints) : (currentPoints - finalPoints);

            // 100 PUAN SINIRI KONTROLÜ
            if (isAddition && projectedPoints >= 100) {
                Platform.runLater(() -> {
                    showCustomConfirmation(
                        "Critical Warning", 
                        "If this penalty is added, the student's total points will be " + projectedPoints + " and will exceed the 100 limit.\nThe student will be DIRECTLY BANNED FROM THE SYSTEM.\nAre you sure?",
                        () -> executePenaltyAndBan(targetStudent, finalPoints, true, clickedBtn, originalBtnText, true), // EVET'e basarsa
                        () -> { // İPTAL'e basarsa
                            isProcessing = false;
                            clickedBtn.setText(originalBtnText);
                        }
                    );
                });
            } else {
                executePenaltyAndBan(targetStudent, finalPoints, isAddition, clickedBtn, originalBtnText, false);
            }

        }).start();
    }

    private void executePenaltyAndBan(Student targetStudent, int finalPoints, boolean isAddition, Button clickedBtn, String originalBtnText, boolean autoBan) {
        new Thread(() -> {
            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            DbStatus status;
            
            if (isAddition) {
                status = adminManager.givePenaltyPoint(currentAdmin, targetStudent, finalPoints);
            } else {
                status = adminManager.reducePenaltyPoint(currentAdmin, targetStudent, finalPoints);
            }

            if (status == DbStatus.SUCCESS && autoBan) {
                adminManager.banStudent(currentAdmin, targetStudent);
            }

            Platform.runLater(() -> {
                isProcessing = false;
                clickedBtn.setText(originalBtnText);

                if (status == DbStatus.SUCCESS) {
                    studentEmailField.clear();
                    penaltyPointsField.clear();
                    
                    if (autoBan) {
                        showCustomAlert("Banned!", targetStudent.getFullName() + "'s penalty points exceeded 100, so they were REMOVED FROM THE SYSTEM.");
                    } else {
                        String action = isAddition ? " added." : " removed.";
                        showCustomAlert("Successful", targetStudent.getFullName() + "'s " + finalPoints + " penalty points" + action + "\n(Current Penalty Points: " + targetStudent.getPenaltyPoints() + ")");
                    }
                } else {
                    showCustomAlert("Error", "A server-side error occurred during the operation.");
                }
            });
        }).start();
    }

    @FXML
    public void handleUnbanStudent(ActionEvent event) {
        if (isProcessing) return;

        String email = studentEmailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showCustomAlert("Warning", "Please enter the email of the student whose ban is to be lifted.");
            return;
        }

        isProcessing = true;
        unbanStudentBtn.setText("Processing...");

        new Thread(() -> {
            Database db = Database.getInstance();
            Student targetStudent = new Student("", email.trim(), "");
            DbStatus fillStatus = db.fillStudentDataByEmail(targetStudent, email.trim());

            if (fillStatus != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    unbanStudentBtn.setText("Unban");
                    showCustomAlert("Not Found", "No student with this email found in the system.");
                });
                return;
            }

            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            DbStatus status = adminManager.unbanStudent(currentAdmin, targetStudent);

            Platform.runLater(() -> {
                isProcessing = false;
                unbanStudentBtn.setText("Ban Kaldır");

                if (status == DbStatus.SUCCESS) {
                    studentEmailField.clear();
                    penaltyPointsField.clear();
                    showCustomAlert("Ban Lifted!", targetStudent.getFullName() + "'s ban has been successfully lifted and they have been notified.");
                } else {
                    showCustomAlert("Error", "An error occurred while lifting the student's ban.");
                }
            });
        }).start();
    }

    @FXML
    public void handleBanStudent(ActionEvent event) {
        if (isProcessing) return;

        String email = studentEmailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showCustomAlert("Warning", "Please enter the email of the student to be banned.");
            return;
        }

        isProcessing = true;
        banStudentBtn.setText("Processing...");

        new Thread(() -> {
            Database db = Database.getInstance();
            Student targetStudent = new Student("", email.trim(), "");
            DbStatus fillStatus = db.fillStudentDataByEmail(targetStudent, email.trim());

            if (fillStatus != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    banStudentBtn.setText("Ban from System");
                    showCustomAlert("Not Found", "No student with this email found in the system.");
                });
                return;
            }

            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            DbStatus status = adminManager.banStudent(currentAdmin, targetStudent);

            Platform.runLater(() -> {
                isProcessing = false;
                banStudentBtn.setText("Sistemden Banla");

                if (status == DbStatus.SUCCESS) {
                    studentEmailField.clear();
                    penaltyPointsField.clear();
                    showCustomAlert("Banned!", targetStudent.getFullName() + " has been removed from the system and notified.");
                } else {
                    showCustomAlert("Error", "An error occurred while banning the student.");
                }
            });
        }).start();
    }

    @FXML
    public void handleChangeName(ActionEvent event) {
        if (isProcessing) return;

        Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
        String newName = nameField.getText();

        if (newName == null || newName.trim().isEmpty()) {
            showCustomAlert("Warning", "Name field cannot be left empty.");
            return;
        }

        isProcessing = true;
        changeNameBtn.setText("Updating...");

        new Thread(() -> {
            DbStatus status = adminManager.updateNickname(currentAdmin, newName.trim());

            Platform.runLater(() -> {
                isProcessing = false;
                changeNameBtn.setText("Change");

                if (status == DbStatus.SUCCESS) {
                    nameField.clear();
                    nameField.setPromptText("Current: " + currentAdmin.getFullName());
                    showCustomAlert("Successful", "Your admin name has been successfully updated!");
                } else {
                    showCustomAlert("Error", "A server-side issue occurred while updating the name.");
                }
            });
        }).start();
    }

    @FXML
    public void handleChangePassword(ActionEvent event) {
        if (isProcessing) return;

        Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
        String newPassword = passwordField.getText();

        if (newPassword == null || newPassword.trim().isEmpty()) {
            showCustomAlert("Warning", "Password field cannot be empty.");
            return;
        }

        if (newPassword.trim().length() < 6) {
            showCustomAlert("Weak Password", "Your new password must be at least 6 characters long.");
            return;
        }

        isProcessing = true;
        changePasswordBtn.setText("Updating...");

        new Thread(() -> {
            DbStatus status = adminManager.updatePassword(currentAdmin, newPassword.trim());

            Platform.runLater(() -> {
                isProcessing = false;
                changePasswordBtn.setText("Change");

                if (status == DbStatus.SUCCESS) {
                    passwordField.clear();
                    showCustomAlert("Successful", "Your password has been successfully changed.");
                } else if (status == DbStatus.SAME_PASSWORD) {
                    showCustomAlert("Error", "The new password must be different from the old one.");
                } else {
                    showCustomAlert("Error", "A server-side issue occurred while updating the password.");
                }
            });
        }).start();
    }

    private void showCustomConfirmation(String title, String message, Runnable onConfirm, Runnable onCancel) {
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
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #D93025;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            dialogStage.close();
            if (onCancel != null) onCancel.run();
        });

        Button confirmBtn = new Button("Yes, Ban");
        confirmBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            dialogStage.close();
            if (onConfirm != null) onConfirm.run();
        });

        btnBox.getChildren().addAll(cancelBtn, confirmBtn);
        layout.getChildren().addAll(titleLabel, msgLabel, btnBox);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
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