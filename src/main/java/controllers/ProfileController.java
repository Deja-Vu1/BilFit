package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;

import managers.SessionManager;
import managers.StudentManager;
import database.Database;
import database.DbStatus;
import models.Student;
import models.SportType;

public class ProfileController {

   @FXML private Label nameLabel;
   @FXML private Label studentIdLabel;
   @FXML private Label matchesPlayedLabel;
   @FXML private Label winRateLabel;
   @FXML private Label eloPointLabel;
   @FXML private HBox interestsBox;
   
   @FXML private StackPane profileImageContainer;
   @FXML private Circle profileImageCircle;
   @FXML private VBox editIconOverlay;

   private StudentManager studentManager = new StudentManager(Database.getInstance());

   @FXML
   public void initialize() {
       if (profileImageContainer != null && editIconOverlay != null) {
           profileImageContainer.setOnMouseEntered(e -> editIconOverlay.setVisible(true));
           profileImageContainer.setOnMouseExited(e -> editIconOverlay.setVisible(false));
       }
       loadProfileData();
   }

 private void loadProfileData() {
       new Thread(() -> {
           try {
               Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();

               // 1. ADIM: FOTOĞRAFI ARKA PLANDA TAMAMEN İNDİR! (Platform.runLater DIŞINDA)
               Image downloadedImg = null;
               if (currentUser != null && currentUser.getProfilePictureUrl() != null && !currentUser.getProfilePictureUrl().isEmpty()) {
                   String picUrl = currentUser.getProfilePictureUrl();
                   String noCacheUrl = picUrl + (picUrl.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
                   // false parametresi: Resim tamamen inene kadar bu arka plan Thread'ini beklet (UI donmaz)
                   downloadedImg = new Image(noCacheUrl, false);
               }
               
               // Lambda içine gönderebilmek için final yapıyoruz
               final Image finalImg = downloadedImg;

               // 2. ADIM: İNDİRME BİTTİKTEN SONRA ARAYÜZÜ GÜNCELLE
               Platform.runLater(() -> {
                   if (currentUser != null) {
                       if (nameLabel != null) nameLabel.setText(currentUser.getFullName());
                       if (studentIdLabel != null) studentIdLabel.setText("ID: " + currentUser.getStudentId());
                       if (eloPointLabel != null) eloPointLabel.setText(String.valueOf(currentUser.getEloPoint()));
                       if (matchesPlayedLabel != null) matchesPlayedLabel.setText(String.valueOf(currentUser.getMatchesPlayed()));
                       if (winRateLabel != null) winRateLabel.setText(String.format("%.0f%%", currentUser.getWinRate() * 100));

                       // TAMAMEN İNDİRİLMİŞ RESMİ YUVARLAĞA KOY (HATA VERMEZ)
                       if (finalImg != null && !finalImg.isError()) {
                           profileImageCircle.setFill(new ImagePattern(finalImg));
                       } else {
                           profileImageCircle.setFill(javafx.scene.paint.Color.web("#E2E8F0"));
                       }

                       if (interestsBox != null) {
                           interestsBox.getChildren().clear();
                           if (currentUser.getInterests() != null && !currentUser.getInterests().isEmpty()) {
                               for (SportType sport : currentUser.getInterests()) {
                                   Label sportLabel = new Label(sport.name().replace("_", " "));
                                   sportLabel.getStyleClass().add("modern-input"); 
                                   interestsBox.getChildren().add(sportLabel);
                               }
                           } else {
                               Label emptyLabel = new Label("İlgi Alanı Yok");
                               emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-style: italic;");
                               interestsBox.getChildren().add(emptyLabel);
                           }
                       }
                   }
               });
           } catch (Exception e) {
               e.printStackTrace();
           }
       }).start();
   }
   @FXML
   public void handleProfilePictureUpload() {
       FileChooser fileChooser = new FileChooser();
       fileChooser.setTitle("Profil Fotoğrafı Seç");
       fileChooser.getExtensionFilters().addAll(
           new FileChooser.ExtensionFilter("Resim Dosyaları", "*.png", "*.jpg", "*.jpeg")
       );
       
       File selectedFile = fileChooser.showOpenDialog(profileImageContainer.getScene().getWindow());
       if (selectedFile != null) {
           Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
           
           editIconOverlay.setVisible(true);
           
           new Thread(() -> {
               DbStatus status = studentManager.updateProfilePicture(currentUser, selectedFile);
               
               Platform.runLater(() -> {
                   editIconOverlay.setVisible(false);
                   if (status == DbStatus.SUCCESS) {
                       loadProfileData(); 
                       showCustomAlert("Başarılı", "Profil fotoğrafınız harika görünüyor! Başarıyla güncellendi.");
                   } else if (status == DbStatus.FILE_TOO_LARGE) {
                       showCustomAlert("Dosya Çok Büyük", "Seçtiğiniz fotoğrafın boyutu çok yüksek. Lütfen 5 MB altı bir dosya seçin.");
                   } else {
                       showCustomAlert("Hata", "Fotoğraf yüklenirken sunucu kaynaklı bir hata oluştu.");
                   }
               });
           }).start();
       }
   }

   // ŞIK VE MODERN POP-UP TASARIMI
   private void showCustomAlert(String title, String message) {
       Stage dialogStage = new Stage();
       dialogStage.initModality(Modality.APPLICATION_MODAL);
       dialogStage.initStyle(StageStyle.TRANSPARENT);

       VBox layout = new VBox(20);
       layout.setAlignment(Pos.CENTER);
       layout.setPadding(new Insets(30));
       layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
       
       DropShadow shadow = new DropShadow();
       shadow.setRadius(20);
       shadow.setColor(Color.rgb(0, 0, 0, 0.15));
       layout.setEffect(shadow);

       Label titleLabel = new Label(title);
       titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

       Label msgLabel = new Label(message);
       msgLabel.setWrapText(true);
       msgLabel.setAlignment(Pos.CENTER);
       msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");

       Button okBtn = new Button("Tamam");
       okBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
       okBtn.setOnAction(e -> dialogStage.close());

       layout.getChildren().addAll(titleLabel, msgLabel, okBtn);

       Scene scene = new Scene(layout);
       scene.setFill(Color.TRANSPARENT); 
       dialogStage.setScene(scene);
       dialogStage.centerOnScreen();
       dialogStage.showAndWait();
   }
}