package controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import managers.SessionManager;
import models.User;

public class AdminSidebarController {
    
    private AdminMainController mainController;
    private static AdminSidebarController instance;
    @FXML private Button btnHome;
    @FXML private Button btnAccount;
    @FXML private Button btnReservation;
    @FXML private Button btnFacilities;
    @FXML private Button btnSettings;
    
    // YENİ EKLENDİ
    @FXML private Circle profileImageCircle;
    
    @FXML 
    public void initialize() {
        instance = this;
        loadSidebarProfilePicture();
    }
    public static void refreshProfilePicture() {
        if (instance != null) {
            instance.loadSidebarProfilePicture();
        }
    }

    // FOTOĞRAFI ARKA PLANDA ÇEKİP SİDEBARA KOYAN METOT
    private void loadSidebarProfilePicture() {
        new Thread(() -> {
            try {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser == null) return;

                Image downloadedImg = null;
                if (currentUser.getProfilePictureUrl() != null && !currentUser.getProfilePictureUrl().isEmpty()) {
                    String picUrl = currentUser.getProfilePictureUrl();
                    String noCacheUrl = picUrl + (picUrl.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
                    downloadedImg = new Image(noCacheUrl, false); 
                }

                final Image finalImg = downloadedImg;

                Platform.runLater(() -> {
                    if (profileImageCircle != null) {
                        if (finalImg != null && !finalImg.isError()) {
                            profileImageCircle.setFill(new ImagePattern(finalImg));
                        } else {
                            profileImageCircle.setFill(javafx.scene.paint.Color.web("#E2E8F0"));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateActiveButton(Button clickedButton) {
        Button[] allButtons = {btnHome, btnAccount, btnReservation, btnFacilities, btnSettings};
        
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        
        if (clickedButton != null) {
            clickedButton.getStyleClass().add("active");
        }
    }

    public void setMainController(AdminMainController controller) {
        this.mainController = controller;
        updateActiveButton(btnHome); 
    }

    @FXML private void loadHome() { updateActiveButton(btnHome); if (mainController != null) mainController.loadHome(); }
    @FXML private void loadAccount() { updateActiveButton(btnAccount); if (mainController != null) mainController.loadAccount(); }
    @FXML private void loadReservations() { updateActiveButton(btnReservation); if (mainController != null) mainController.loadReservations(); }
    @FXML private void loadFacilities() { updateActiveButton(btnFacilities); if (mainController != null) mainController.loadFacilities(); }
    @FXML private void loadSettings() { updateActiveButton(btnSettings); if (mainController != null) mainController.loadSettings(); }

    @FXML 
    private void logout() {
        System.out.println("Admin çıkış yapıyor...");
        SessionManager.getInstance().logout();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnHome.getScene().getWindow(); 
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("HATA: Çıkış yapılırken SelectionView sayfası yüklenemedi!");
            e.printStackTrace();
        }
    }
}