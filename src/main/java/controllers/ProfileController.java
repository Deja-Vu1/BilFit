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
       // Arayüz donmasın diye arka planda çalıştırıyoruz
       new Thread(() -> {
           try {
               // 1. SessionManager'dan giriş yapmış AKTİF KULLANICIYI çekiyoruz
               Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();

               Platform.runLater(() -> {
                   if (currentUser != null) {
                       // 2. SQL'e gerek yok! Tüm veriler zaten currentUser objesinin içinde.
                       if (nameLabel != null) nameLabel.setText(currentUser.getFullName());
                       if (studentIdLabel != null) studentIdLabel.setText("ID: " + currentUser.getStudentId());
                       
                       if (eloPointLabel != null) eloPointLabel.setText(String.valueOf(currentUser.getEloPoint()));
                       if (matchesPlayedLabel != null) matchesPlayedLabel.setText(String.valueOf(currentUser.getMatchesPlayed()));
                       
                       // WinRate oranını (örn: 0.83) yüzdelik dilime (%83) çeviriyoruz
                       if (winRateLabel != null) winRateLabel.setText(String.format("%.0f%%", currentUser.getWinRate() * 100));
                   } else {
                       // Eğer bir hata olur da kullanıcı bulunamazsa kalkanımız:
                       if (nameLabel != null) nameLabel.setText("Kullanıcı Bulunamadı");
                   }
               });
           } catch (Exception e) {
               e.printStackTrace();
           }
       }).start();
   }
}