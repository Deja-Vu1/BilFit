package controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import database.Database;
import database.DbStatus;
import managers.AuthManager;

public class AdminRegisterController {

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    
    
    @FXML private PasswordField adminAccessCodeField;

    private boolean isProcessing = false;
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptAdminRegister(ActionEvent event) {
        if (isProcessing) return;

        
        String name = fullnameField.getText() != null ? fullnameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim().toLowerCase() : "";
        String password = passwordField.getText();
        String adminSecretCode = adminAccessCodeField != null && adminAccessCodeField.getText() != null ? adminAccessCodeField.getText().trim() : "";
      
        if (name.isEmpty() || email.isEmpty() || password == null || password.isEmpty() || adminSecretCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in all fields (including Admin Authorization Code).");
            return;
        }

        if (!email.endsWith("@ug.bilkent.edu.tr") && !email.endsWith("@yahoo.com") && !email.endsWith("@bilkent.edu.tr")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email", "Only Bilkent email addresses can be used for registration.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Weak Password", "Your password must be at least 6 characters long.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
      
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Registering...");

        new Thread(() -> {
            try {
                
                DbStatus registerStatus = authManager.registerAdmin(email, password, adminSecretCode, name);

                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);

                    switch (registerStatus) {
                        case SUCCESS:
                            
                            AdminActivationController.emailToActivate = email;
                            goToAdminActivation(event);
                            break;
                        case EMAIL_ALREADY_EXISTS:
                            showAlert(Alert.AlertType.ERROR, "Registration Failed", "An admin account with this email address already exists.");
                            break;
                        case CONNECTION_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Connection Error", "Failed to connect to the database.");
                            break;
                        case QUERY_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Invalid Operation", "The data does not comply with database rules or the Admin Authorization Code is incorrect.");
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "System Error", "An unexpected error occurred during admin registration.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Critical Error", "An unexpected error occurred in the system.");
                });
            }
        }).start();
    }

    
    private void goToAdminActivation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminActivationView.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
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
        
        try { alert.initStyle(javafx.stage.StageStyle.UNDECORATED); } catch (Exception ignored) {}
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {}
        
        if (emailField != null && emailField.getScene() != null) {
            Stage stage = (Stage) emailField.getScene().getWindow();
            alert.initOwner(stage);
        }
      
        alert.showAndWait();
    }
}