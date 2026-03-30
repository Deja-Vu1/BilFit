package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class MainDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnAccount, btnHome, btnTournaments, btnELO, btnReservation, btnAdmin, btnSettings;

    @FXML
    public void initialize() {
        try {
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
        System.out.println("Logging out...");
    }

    private void loadView(String fxml, Button activeBtn) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/dashboard/" + fxml));
        contentArea.getChildren().setAll(root);
        
        btnAccount.setStyle("-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnHome.setStyle("-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnTournaments.setStyle("-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnELO.setStyle("-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnReservation.setStyle("-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnAdmin.setStyle("-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnSettings.setStyle("-fx-background-color: transparent; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");

        activeBtn.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10;");
    }
}