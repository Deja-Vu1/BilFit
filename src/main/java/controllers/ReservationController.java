package controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class ReservationController {
        @FXML public void initialize() {} 
        @FXML public void loadHome(ActionEvent event) throws Exception { loadScene(event, "/views/dashboard/HomeView.fxml"); }

        private void loadScene(ActionEvent event, String fxmlFile) throws Exception {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
    }
}