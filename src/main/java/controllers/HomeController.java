package controllers;


import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class HomeController {


   // FXML Labels for Tournaments
   @FXML private Label tournament1Label;
   @FXML private Label tournament2Label;


   // FXML Labels for Notifications
   @FXML private Label notification1Label;
   @FXML private Label notification2Label;
   @FXML private Label notification3Label;


   // Singleton Database instance
   private Database db = Database.getInstance();


   @FXML
   public void initialize() {
       // Fetch and display data as soon as the Home view is loaded
       loadHomeData();
   }


   private void loadHomeData() {
       // Run database operations on a separate thread to prevent UI freezing
       new Thread(() -> {
           try {
               /*
                * TODO: Replace these hardcoded strings with actual Database queries when
                * the 'tournaments' and 'notifications' tables are fully created by ilkan
                * Example SQL: "SELECT name, start_date, max_players FROM tournaments ORDER BY created_at DESC LIMIT 2"
                */


               String t1 = "Football Tournament  | 25.02.2026 - 20.03.2026  |  Max 10 player  | Ge250-251";
               String t2 = "Tennis Tournament  | 25.02.2026 - 20.03.2026  |  Max 4 player  | Ge250-251";
              
               String n1 = "Football Tournament   |   Date Change   |   new date 25.02.2026   |   10.02.2026";
               String n2 = "Tennis Tournament   |   Location Change   |   new location East Campus 1   |   7.02.2026";
               String n3 = "Basketball Tournament   |   Location Change   |   new location East Campus SS   |   6.02.2026";


               // Update the UI back on the main JavaFX thread
               Platform.runLater(() -> {
                   if (tournament1Label != null) tournament1Label.setText(t1);
                   if (tournament2Label != null) tournament2Label.setText(t2);
                  
                   if (notification1Label != null) notification1Label.setText(n1);
                   if (notification2Label != null) notification2Label.setText(n2);
                   if (notification3Label != null) notification3Label.setText(n3);
               });


           } catch (Exception e) {
               e.printStackTrace();
           }
       }).start();
   }
}



