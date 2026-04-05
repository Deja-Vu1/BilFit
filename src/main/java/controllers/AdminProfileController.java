package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.util.List;

import managers.AdminManager;
import managers.SessionManager;
import models.Admin;
import models.User;

public class AdminProfileController {

    @FXML private Label nameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalAdminsLabel; 
    @FXML private Label totalFacilitiesLabel;
    @FXML private Label adminActionsLabel;

    @FXML private StackPane profileImageContainer;
    @FXML private Circle profileImageCircle;
    @FXML private VBox editIconOverlay;

    private Database db = Database.getInstance();
    private AdminManager adminManager = new AdminManager(db); 

    @FXML
    public void initialize() {
        if (profileImageContainer != null && editIconOverlay != null) {
            profileImageContainer.setOnMouseEntered(e -> editIconOverlay.setVisible(true));
            profileImageContainer.setOnMouseExited(e -> editIconOverlay.setVisible(false));
        }
        loadAdminProfileData();
    }

    private void loadAdminProfileData() {
        new Thread(() -> {
            try {
                User sessionUser = SessionManager.getInstance().getCurrentUser();
                if (!(sessionUser instanceof Admin)) return;
                
                Admin currentAdmin = (Admin) sessionUser;

                int facilitiesCount = db.getFacilities() != null ? db.getFacilities().size() : 0;
                java.util.List<Integer> userCounts = adminManager.getUsersCount();
                int studentsCount = (userCounts != null && userCounts.size() >= 2) ? userCounts.get(0) : 0;
                int adminsCount = (userCounts != null && userCounts.size() >= 2) ? userCounts.get(1) : 0;
                int actionsCount = currentAdmin.getActionsPerformed();

                
                Image downloadedImg = null;
                if (currentAdmin.getProfilePictureUrl() != null && !currentAdmin.getProfilePictureUrl().isEmpty()) {
                    String picUrl = currentAdmin.getProfilePictureUrl();
                    String noCacheUrl = picUrl + (picUrl.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
                    downloadedImg = new Image(noCacheUrl, false);
                }
                
                final Image finalImg = downloadedImg;

                
                Platform.runLater(() -> {
                    if (nameLabel != null) nameLabel.setText(currentAdmin.getFullName());
                    if (totalFacilitiesLabel != null) totalFacilitiesLabel.setText(String.valueOf(facilitiesCount));
                    if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(studentsCount));
                    if (totalAdminsLabel != null) totalAdminsLabel.setText(String.valueOf(adminsCount));
                    if (adminActionsLabel != null) adminActionsLabel.setText(String.valueOf(actionsCount));

                    
                    if (finalImg != null && !finalImg.isError()) {
                        profileImageCircle.setFill(new ImagePattern(finalImg));
                    } else {
                        profileImageCircle.setFill(javafx.scene.paint.Color.web("#E2E8F0"));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    @FXML
    public void handleProfilePictureUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(profileImageContainer.getScene().getWindow());
        if (selectedFile != null) {
            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            
            editIconOverlay.setVisible(true);
            
            new Thread(() -> {
                DbStatus status = adminManager.updateProfilePicture(currentAdmin, selectedFile);
                
                Platform.runLater(() -> {
                    editIconOverlay.setVisible(false);
                    if (status == DbStatus.SUCCESS) {
                        loadAdminProfileData(); 
                        
                        
                        AdminSidebarController.refreshProfilePicture();
                        
                        showCustomAlert("Success", "Your profile picture has been updated successfully!");
                    } else if (status == DbStatus.FILE_TOO_LARGE) {
                        showCustomAlert("File Too Large", "The selected image is too large. Please choose a file under 5 MB.");
                    } else {
                        showCustomAlert("Error", "An error occurred while uploading the image.");
                    }
                });
            }).start();
        }
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