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



