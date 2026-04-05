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

public class StudentSidebarController {

    private StudentMainController mainController;
    private static StudentSidebarController instance;
    @FXML private Button btnHome;
    @FXML private Button btnAccount;
    @FXML private Button btnTournaments;
    @FXML private Button btnELO;
    @FXML private Button btnReservation;
    @FXML private Button btnSettings;
    @FXML private Button btnFriends;
    
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
                    downloadedImg = new Image(noCacheUrl, false); // Arka planda donmadan indir
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
        Button[] allButtons = {btnHome, btnAccount, btnTournaments, btnELO, btnReservation, btnSettings, btnFriends};
        
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        
        if (clickedButton != null && !clickedButton.getStyleClass().contains("active")) {
            clickedButton.getStyleClass().add("active");
        }
    }

    @FXML
    public void setMainController(StudentMainController controller) {
        this.mainController = controller;
        updateActiveButton(btnHome);
    }

    @FXML private void loadHome() { updateActiveButton(btnHome); mainController.loadHome(); }
    @FXML private void loadAccount() { updateActiveButton(btnAccount); mainController.loadAccount(); }
    @FXML private void loadTournaments() { updateActiveButton(btnTournaments); mainController.loadTournaments(); }
    @FXML private void loadELO() { updateActiveButton(btnELO); mainController.loadELO(); }
    @FXML private void loadReservations() { updateActiveButton(btnReservation); mainController.loadReservations(); }
    @FXML private void loadSettings() { updateActiveButton(btnSettings); mainController.loadSettings(); }
    @FXML private void loadFriends() { updateActiveButton(btnFriends); mainController.loadFriends(); }

    @FXML 
    private void logout() {
        System.out.println("Oturum kapatılıyor ve Ana Ekrana dönülüyor...");
        try {
            SessionManager.getInstance().logout();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnHome.getScene().getWindow(); 
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Hata: SelectionView yüklenemedi.");
            e.printStackTrace();
        }
    }
}