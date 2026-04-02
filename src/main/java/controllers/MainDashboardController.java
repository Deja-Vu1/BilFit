package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnAccount, btnHome, btnTournaments, btnELO, btnReservation, btnAdmin, btnSettings;

    @FXML
    public void initialize() {
        try {
            // Uygulama ilk açıldığında varsayılan olarak Ana Sayfa'yı (Home) yükle
            loadView("HomeView.fxml", btnHome);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void loadAccount(ActionEvent event) throws Exception { loadView("ProfileView.fxml", btnAccount); }

    @FXML
    public void loadHome(ActionEvent event) throws Exception { loadView("HomeView.fxml", btnHome); }

    @FXML
    public void loadTournaments(ActionEvent event) throws Exception { loadView("TournamentsView.fxml", btnTournaments); }

    @FXML
    public void loadELO(ActionEvent event) throws Exception { loadView("ELOView.fxml", btnELO); }

    @FXML
    public void loadReservation(ActionEvent event) throws Exception { loadView("ReservationView.fxml", btnReservation); }

    @FXML
    public void loadAdmin(ActionEvent event) throws Exception { loadView("AdminDashboardView.fxml", btnAdmin); }

    @FXML
    public void loadSettings(ActionEvent event) throws Exception { loadView("SettingsView.fxml", btnSettings); }

    @FXML
    public void logout(ActionEvent event) {
        System.out.println("Sistemden çıkış yapılıyor...");
        try {
            // 1. SelectionView (Ana giriş seçimi) ekranına dönmek için sayfayı yükle
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            
            // 2. Full-screen ayarını bozmadan ana giriş ekranına geçiş yap
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Çıkış Hatası", "Çıkış yapılırken bir sorun oluştu.");
        }
    }

    
    private void loadView(String fxml, Button activeBtn) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/dashboard/" + fxml));
        contentArea.getChildren().setAll(root);
        
        
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;";
        
        if (btnAccount != null) btnAccount.setStyle(defaultStyle);
        if (btnHome != null) btnHome.setStyle(defaultStyle);
        if (btnTournaments != null) btnTournaments.setStyle(defaultStyle);
        if (btnELO != null) btnELO.setStyle(defaultStyle);
        if (btnReservation != null) btnReservation.setStyle(defaultStyle);
        if (btnAdmin != null) btnAdmin.setStyle(defaultStyle);
        if (btnSettings != null) btnSettings.setStyle(defaultStyle);

       
        if (activeBtn != null) {
            activeBtn.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10;");
        }
    }

    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        if (contentArea != null && contentArea.getScene() != null) {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            alert.initOwner(stage);
        }
        
        alert.showAndWait();
    }
}