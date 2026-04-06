package controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;


public class FacilitySelectController {


   @FXML
   public void selectSport(ActionEvent event) {
       
       try {
           
           StackPane contentArea = (StackPane) ((Parent)event.getSource()).getScene().lookup("#contentArea");
           Parent nextView = FXMLLoader.load(getClass().getResource("/views/dashboard/TimeSelectView.fxml"));
           contentArea.getChildren().setAll(nextView);
       } catch (Exception e) {
           e.printStackTrace();
       }
   }
}



