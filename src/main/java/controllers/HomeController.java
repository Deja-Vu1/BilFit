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

   @FXML private Label tournament1Label;
   @FXML private Label tournament2Label;

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
                           Label emptyLabel = new Label("You have no new notifications at the moment.");
                           emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                           notificationsContainer.getChildren().add(emptyLabel);
                       } else {
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

   private HBox createNotificationRow(Notification notif, DateTimeFormatter formatter) {
       HBox row = new HBox(15);
       row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
       row.setStyle("-fx-background-color: #F4F7FE; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #E2E8F0;");
       row.setPadding(new Insets(10, 15, 10, 15));
       
       Label starIndicator = new Label("★");
       starIndicator.setStyle("-fx-text-fill: #FF9120; -fx-font-size: 18px; -fx-font-weight: bold;");
       
       VBox textContainer = new VBox(5);
       
       String dateStr = (notif.getDate() != null) ? notif.getDate().format(formatter) : "";
       Label titleLabel = new Label(notif.getTitle() + "  |  " + dateStr);
       titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B3674; -fx-font-size: 13px;");
       
       Label msgLabel = new Label(notif.getMessage());
       msgLabel.setWrapText(true); 
       msgLabel.setStyle("-fx-text-fill: #A3AED0; -fx-font-size: 12px;");
       
       textContainer.getChildren().addAll(titleLabel, msgLabel);
       
       row.getChildren().addAll(starIndicator, textContainer);
       return row;
   }
}