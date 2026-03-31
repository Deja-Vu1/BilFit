package controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProfileController {
    @FXML public void initialize() {}
    
    @FXML public void loadAccount(ActionEvent event) throws Exception { loadScene(event, "/views/dashboard/ProfileView.fxml"); }
    @FXML public void loadTournaments(ActionEvent event) throws Exception { loadScene(event, "/views/dashboard/TournamentsView.fxml"); }
    @FXML public void loadELO(ActionEvent event) throws Exception { loadScene(event, "/views/dashboard/ELOView.fxml"); }
    @FXML public void loadReservation(ActionEvent event) throws Exception { loadScene(event, "/views/dashboard/ReservationView.fxml"); }
    @FXML public void loadSettings(ActionEvent event) throws Exception { loadScene(event, "/views/dashboard/SettingsView.fxml"); }
    @FXML public void logout(ActionEvent event) throws Exception { loadScene(event, "/views/auth/SelectionView.fxml"); }

    private void loadScene(ActionEvent event, String fxmlFile) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }
}