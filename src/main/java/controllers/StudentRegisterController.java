package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.Node;

import database.Database;
import database.DbStatus;
import managers.AuthManager;

import java.io.IOException;

public class StudentRegisterController {

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField studentIdField;
    @FXML private PasswordField passwordField;

    private boolean isProcessing = false;
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptRegister(ActionEvent event) {
        if (isProcessing) return;

        String name = fullnameField.getText() != null ? fullnameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim().toLowerCase() : "";
        String studentId = studentIdField.getText() != null ? studentIdField.getText().trim() : "";
        String password = passwordField.getText(); 
      
        if (name.isEmpty() || email.isEmpty() || studentId.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in all fields.");
            return;
        }
        
        if (!email.endsWith("@ug.bilkent.edu.tr") && !email.endsWith("@bilkent.edu.tr") && !email.endsWith("@alumni.bilkent.edu.tr")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email", "You can only register with Bilkent email addresses in the system.");
            return;
        }

        
        if (!studentId.matches("\\d+") || studentId.length() < 7 || studentId.length() > 9) {
            showAlert(Alert.AlertType.WARNING, "Invalid ID", "Your student number must consist only of digits and be of valid length (Ex: 22200000).");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Weak Password", "Your password must be at least 6 characters long.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalButtonText = clickedButton.getText();
      
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Registering...");

        new Thread(() -> {
            try {
                DbStatus registerStatus = authManager.registerStudent(email, password, studentId, name);

                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalButtonText);

                    switch (registerStatus) {
                        case SUCCESS:
                            ActivationController.emailToActivate = email;
                            goToActivation(event);
                            break;
                        case EMAIL_ALREADY_EXISTS:
                            showAlert(Alert.AlertType.ERROR, "Registration Failed", "This email address already exists in the system.");
                            break;
                        case ID_ALREADY_EXISTS:
                            showAlert(Alert.AlertType.ERROR, "Registration Failed", "This student number already exists in the system.");
                            break;
                        case CONNECTION_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the database.");
                            break;
                        case QUERY_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Database Rejection", "The database rejected this record! Either there is a special character in the information, or there is a previously incomplete registration with this email.");
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "System Error", "An unexpected error occurred during registration.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalButtonText);
                });
            }
        }).start();
    }

    private void goToActivation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/ActivationView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goBack(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch(Exception e) {}

        if (emailField != null && emailField.getScene() != null) {
            Stage stage = (Stage) emailField.getScene().getWindow();
            alert.initOwner(stage);
        }
      
        alert.showAndWait();
    }
}