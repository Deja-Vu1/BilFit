package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import managers.SessionManager;
import models.Notification;
import models.Student;
import models.User;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HomeController {

   // FXML Labels for Tournaments (Burayı da DB'ye bağlayacağımız zaman dinamik yapacağız)
   @FXML private Label tournament1Label;
   @FXML private Label tournament2Label;

   // ARTIK 3 TANE SABİT LABEL DEĞİL, SINIRSIZ SCROLL KUTUMUZ VAR!
   @FXML private VBox notificationsContainer;

   private Database db = Database.getInstance();

   @FXML
   public void initialize() {
       loadHomeData();
   }

   private void loadHomeData() {
       new Thread(() -> {
           try {
               User sessionUser = SessionManager.getInstance().getCurrentUser();
               if (!(sessionUser instanceof Student)) return;
               
               Student currentUser = (Student) sessionUser;

               // VERİTABANINDAN TÜM BİLDİRİMLERİ ÇEKİYORUZ
               List<Notification> myNotifications = db.getNotificationsByStudent(currentUser);

               String t1 = "Football Tournament  | 25.02.2026 - 20.03.2026  |  Max 10 player  | Ge250-251";
               String t2 = "Tennis Tournament  | 25.02.2026 - 20.03.2026  |  Max 4 player  | Ge250-251";
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

               Platform.runLater(() -> {
                   if (tournament1Label != null) tournament1Label.setText(t1);
                   if (tournament2Label != null) tournament2Label.setText(t2);
                  
                   if (notificationsContainer != null) {
                       notificationsContainer.getChildren().clear();

                       if (myNotifications.isEmpty()) {
                           Label emptyLabel = new Label("Şu an için yeni bir bildiriminiz bulunmuyor.");
                           emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                           notificationsContainer.getChildren().add(emptyLabel);
                       } else {
                           // 10 TANE BİLE OLSA LİSTEYE EKLER, KULLANICI SCROLL İLE KAYDIRIR!
                           for (Notification notif : myNotifications) {
                               notificationsContainer.getChildren().add(createNotificationRow(notif, formatter));
                           }
                       }
                   }
               });

           } catch (Exception e) {
               e.printStackTrace();
           }
       }).start();
   }

   // HER BİLDİRİM İÇİN YILDIZLI ŞIK BİR SATIR (HBox) ÜRETEN METOT
   private HBox createNotificationRow(Notification notif, DateTimeFormatter formatter) {
       // Orijinal tasarımdaki gibi elemanları yan yana dizecek HBox
       HBox row = new HBox(15);
       row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
       row.setStyle("-fx-background-color: #F4F7FE; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #E2E8F0;");
       row.setPadding(new Insets(10, 15, 10, 15));
       
       // DÜZELTME: Turuncu Yuvarlak yerine Turuncu Yıldız (Unicode Karakteri)
       Label starIndicator = new Label("★");
       // Stil: Renk #FF9120 (Orijinal turuncu), Boyut 18px, Kalın
       starIndicator.setStyle("-fx-text-fill: #FF9120; -fx-font-size: 18px; -fx-font-weight: bold;");
       
       // Yazıları alt alta dizecek VBox
       VBox textContainer = new VBox(5);
       
       String dateStr = (notif.getDate() != null) ? notif.getDate().format(formatter) : "";
       Label titleLabel = new Label(notif.getTitle() + "  |  " + dateStr);
       titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B3674; -fx-font-size: 13px;");
       
       Label msgLabel = new Label(notif.getMessage());
       msgLabel.setWrapText(true); 
       msgLabel.setStyle("-fx-text-fill: #A3AED0; -fx-font-size: 12px;");
       
       textContainer.getChildren().addAll(titleLabel, msgLabel);
       
       // Yıldız ve yazıları yan yana koy
       row.getChildren().addAll(starIndicator, textContainer);
       return row;
   }
}