package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import managers.SessionManager;
import models.Student;
import models.SportType;

public class ProfileController {

   @FXML private Label nameLabel;
   @FXML private Label studentIdLabel;
   @FXML private Label matchesPlayedLabel;
   @FXML private Label winRateLabel;
   @FXML private Label eloPointLabel;
   
   // YENİ EKLENEN DİNAMİK İLGİ ALANLARI KUTUSU
   @FXML private HBox interestsBox;

   @FXML
   public void initialize() {
       loadProfileData();
   }

   private void loadProfileData() {
       new Thread(() -> {
           try {
               Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();

               Platform.runLater(() -> {
                   if (currentUser != null) {
                       
                       if (nameLabel != null) nameLabel.setText(currentUser.getFullName());
                       if (studentIdLabel != null) studentIdLabel.setText("ID: " + currentUser.getStudentId());
                       
                       if (eloPointLabel != null) eloPointLabel.setText(String.valueOf(currentUser.getEloPoint()));
                       if (matchesPlayedLabel != null) matchesPlayedLabel.setText(String.valueOf(currentUser.getMatchesPlayed()));
                       if (winRateLabel != null) winRateLabel.setText(String.format("%.0f%%", currentUser.getWinRate() * 100));

                       // İLGİ ALANLARINI DİNAMİK OLARAK OLUŞTUR VE EKRANA ÇİZ
                       if (interestsBox != null) {
                           interestsBox.getChildren().clear(); // Kutuyu temizle (Çift yazmayı engeller)
                           
                           if (currentUser.getInterests() != null && !currentUser.getInterests().isEmpty()) {
                               // Her bir spor türü için o eski "CS" tasarımında yeni bir Label üret
                               for (SportType sport : currentUser.getInterests()) {
                                   Label sportLabel = new Label(sport.name().replace("_", " "));
                                   sportLabel.getStyleClass().add("modern-input"); // Aynı şık gri stil
                                   interestsBox.getChildren().add(sportLabel);
                               }
                           } else {
                               // Eğer hiç ilgi alanı seçmemişse silik bir uyarı göster
                               Label emptyLabel = new Label("İlgi Alanı Yok");
                               emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-style: italic;");
                               interestsBox.getChildren().add(emptyLabel);
                           }
                       }

                   } else {
                       if (nameLabel != null) nameLabel.setText("Kullanıcı Bulunamadı");
                   }
               });
           } catch (Exception e) {
               e.printStackTrace();
           }
       }).start();
   }
}