package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import managers.SessionManager;
import models.Student;

public class ProfileController {

   @FXML private Label nameLabel;
   @FXML private Label studentIdLabel;
   @FXML private Label matchesPlayedLabel;
   @FXML private Label winRateLabel;
   @FXML private Label eloPointLabel;

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
                       // Sadece Full Name gösteriliyor, sen değiştirdikçe bu da anında değişecek!
                       if (nameLabel != null) nameLabel.setText(currentUser.getFullName());
                       if (studentIdLabel != null) studentIdLabel.setText("ID: " + currentUser.getStudentId());
                       
                       if (eloPointLabel != null) eloPointLabel.setText(String.valueOf(currentUser.getEloPoint()));
                       if (matchesPlayedLabel != null) matchesPlayedLabel.setText(String.valueOf(currentUser.getMatchesPlayed()));
                       
                       if (winRateLabel != null) winRateLabel.setText(String.format("%.0f%%", currentUser.getWinRate() * 100));
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