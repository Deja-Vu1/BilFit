package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;

public class TournamentsController {

    @FXML
    public void initialize() {
        System.out.println("Tournaments ekranı başarıyla yüklendi.");
    }

    // --- Navigasyon Metodları ---

    @FXML
    public void loadAccount(ActionEvent event) {
        switchScene(event, "/views/dashboard/ProfileView.fxml");
    }

    @FXML
    public void loadHome(ActionEvent event) {
        switchScene(event, "/views/dashboard/HomeView.fxml");
    }

    @FXML
    public void loadELO(ActionEvent event) {
        switchScene(event, "/views/dashboard/ELOView.fxml");
    }

    @FXML
    public void loadReservation(ActionEvent event) {
        switchScene(event, "/views/dashboard/FacilitySelectView.fxml");
    }

    @FXML
    public void loadSettings(ActionEvent event) {
        switchScene(event, "/views/dashboard/SettingsView.fxml");
    }

    @FXML
    public void logout(ActionEvent event) {
        switchScene(event, "/views/auth/SelectionView.fxml");
    }

    // --- Sayfa İçi Buton İşlevleri (FXML'de tanımlı değilse eklenmeli) ---

    @FXML
    public void handleApplyTournament(ActionEvent event) {
        System.out.println("Turnuva başvuru işlemi başlatıldı.");
    }

    @FXML
    public void handleCancelTournament(ActionEvent event) {
        System.out.println("Turnuva iptal edildi.");
    }

    /**
     * Merkezi Sahne Değiştirme Metodu
     * Her geçişte ekranı tam boy yapar.
     */
    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setFullScreen(true); // Tam ekran modunu korur
        } catch (IOException e) {
            System.err.println("Sahne yüklenemedi: " + fxmlPath);
            e.printStackTrace();
        }
    }
}