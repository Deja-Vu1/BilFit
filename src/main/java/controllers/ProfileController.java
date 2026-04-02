package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.lang.reflect.Method; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileController {

    @FXML private Label nameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label matchesPlayedLabel;
    @FXML private Label winRateLabel;
    @FXML private Label eloPointLabel;

    private Database db = Database.getInstance();

    @FXML
    public void initialize() {
        loadProfileData();
    }

    private void loadProfileData() {
        String loggedInUserEmail = "test@ug.bilkent.edu.tr"; 

        new Thread(() -> {
            String query = "SELECT u.full_name, u.student_id, s.elo_point, s.matches_played, s.win_rate " +
                           "FROM users u " +
                           "JOIN students s ON u.id = s.user_id " +
                           "WHERE u.bilkent_email = ?";

            try {
                Method getConnectionMethod = db.getClass().getDeclaredMethod("getConnection");
                getConnectionMethod.setAccessible(true); 
                Connection conn = (Connection) getConnectionMethod.invoke(db); 
                

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, loggedInUserEmail);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String name = rs.getString("full_name");
                        String studentId = rs.getString("student_id");
                        int elo = rs.getInt("elo_point");
                        int matches = rs.getInt("matches_played");
                        double winRate = rs.getDouble("win_rate");

                        Platform.runLater(() -> {
                            if (nameLabel != null) nameLabel.setText(name);
                            if (studentIdLabel != null) studentIdLabel.setText("ID: " + studentId);
                            if (eloPointLabel != null) eloPointLabel.setText(String.valueOf(elo));
                            if (matchesPlayedLabel != null) matchesPlayedLabel.setText(String.valueOf(matches));
                            
                            if (winRateLabel != null) winRateLabel.setText(String.format("%.0f%%", winRate * 100)); 
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}