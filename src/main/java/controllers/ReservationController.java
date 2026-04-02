package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ReservationController {

    // FXML Connections
    @FXML private Label activeReservationLabel;
    @FXML private Button cancelButton;

    // Singleton Database instance
    private Database db = Database.getInstance();

    @FXML
    public void initialize() {
        // Fetch active reservations as soon as the view is loaded
        loadReservationData();
    }

    private void loadReservationData() {
        // Run database operations on a separate thread to prevent UI freezing
        new Thread(() -> {
            try {
                /*
                 * TODO: Replace this hardcoded string with actual Database queries when 
                 * the 'reservations' table and related functions are fully ready.
                 * Example SQL: "SELECT facility_name, date, time_slot FROM reservations WHERE user_id = ?"
                 */
                String myReservation = "Main Campus   |   Football Field 1   |   20.02.2026   |   18.45-19.45";

                // Update the UI on the main JavaFX thread
                Platform.runLater(() -> {
                    if (activeReservationLabel != null) {
                        activeReservationLabel.setText(myReservation);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleCancelReservation(ActionEvent event) {
        /*
         * TODO: Add database logic to delete the reservation.
         * Example: db.cancelReservation(reservationId);
         */
        System.out.println("Cancel button clicked. Deleting reservation...");
        
        // Simulating the UI update after successful cancellation
        if (activeReservationLabel != null) {
            activeReservationLabel.setText("You don't have any active reservations.");
        }
        
        if (cancelButton != null) {
            cancelButton.setDisable(true); // Disable the button so user can't click it again
        }
        
        // Show success pop-up
        showAlert(Alert.AlertType.INFORMATION, "Success", "Your reservation has been successfully canceled.");
    }

    // A helper method for displaying pop-ups
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