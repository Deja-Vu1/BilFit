package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class FacilitySelectController {

    @FXML
    public void selectSport(ActionEvent event) {
        // Hangi sporun seçildiğini belirle (örn: Futbol, Basketbol)
        // Manager sınıfına (ReservationManager) bu bilgiyi gönderin
        try {
            // Dashboard'un içindeki contentArea'yı bulup TimeSelectView yükle
            StackPane contentArea = (StackPane) ((Parent)event.getSource()).getScene().lookup("#contentArea");
            Parent nextView = FXMLLoader.load(getClass().getResource("/views/dashboard/TimeSelectView.fxml"));
            contentArea.getChildren().setAll(nextView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}