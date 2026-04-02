package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class ELOController {

    // FXML Labels
    @FXML private Label activeReservationLabel;
    @FXML private Label duelloMainInfoLabel;
    @FXML private Label duelloSubInfoLabel;

    // Singleton Database connection
    /*private Database db = Database.getInstance();*/

    @FXML
    public void initialize() {
        // Fetch data from the database when the view is loaded
        loadEloAndDuelloData();
    }

    private void loadEloAndDuelloData() {
        // Using a background thread to prevent the UI from freezing
        new Thread(() -> {
            try {
                /*
                 * TODO: Replace hardcoded strings with actual Database queries.
                 * Example: 
                 * ResultSet rs = db.getConnection().prepareStatement("SELECT * FROM duellos WHERE status = 'open'").executeQuery();
                 */
                
                String reservationData = "Main Campus   |   Football Field 1   |   20.02.2026   |   18.45-19.45";
                String duelloMainData = "Main Campus   |   Basketball Field YSS   |   Max 10 player   |   20.02.2026";
                String duelloSubData = "B**** j**** S**** |   Empty Slot: 4   |   Pro";

                // Update the JavaFX UI elements on the main thread
                Platform.runLater(() -> {
                    if (activeReservationLabel != null) activeReservationLabel.setText(reservationData);
                    if (duelloMainInfoLabel != null) duelloMainInfoLabel.setText(duelloMainData);
                    if (duelloSubInfoLabel != null) duelloSubInfoLabel.setText(duelloSubData);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleCreateDuello(ActionEvent event) {
        /*
         * TODO: Logic to convert the current reservation into a public/private duello
         */
        System.out.println("Create Duello clicked.");
        showAlert(Alert.AlertType.INFORMATION, "Duello Created", "Your reservation has been successfully converted into a Duello!");
    }

    @FXML
    public void handleApplyWithCode(ActionEvent event) {
        // Create a prompt dialog to ask the user for a Duello Code
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Private Duello");
        dialog.setHeaderText("Enter Duello Code");
        dialog.setContentText("Code:");

        // Get the response
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            /*
             * TODO: Check if the code exists in the database and add the user to that duello.
             * Example: db.joinDuelloWithCode(userId, code);
             */
            System.out.println("User tried to join with code: " + code);
            showAlert(Alert.AlertType.INFORMATION, "Code Entered", "Checking code: " + code + "\n(Database integration pending...)");
        });
    }

    @FXML
    public void handleRequestDuello(ActionEvent event) {
        /*
         * TODO: Logic to send a join request to the creator of this specific duello.
         */
        System.out.println("Request Duello clicked.");
        showAlert(Alert.AlertType.INFORMATION, "Request Sent", "Your request to join this match has been sent to the host.");
    }

    // Helper method for showing pop-up alerts
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        alert.showAndWait();
    }
}