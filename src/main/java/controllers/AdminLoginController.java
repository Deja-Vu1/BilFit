package controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

public class AdminLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    
    private boolean isProcessing = false;

    
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptAdminLogin(ActionEvent event) {
        if (isProcessing) return;

        
        String email = emailField.getText() != null ? emailField.getText().trim().toLowerCase() : "";
        String password = passwordField.getText();
      
        if (email.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in the email and password fields.");
            return;
        }

        isProcessing = true;

        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
      
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Logging in...");

        
        new Thread(() -> {
            try {
                
                DbStatus loginStatus = authManager.loginAdmin(email, password);

                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);

                    switch (loginStatus) {
                        case SUCCESS:
                            System.out.println("Admin login successful!");
                            deployAdminDashboard(event);
                            break;
                        case INVALID_CREDENTIALS:
                        case DATA_NOT_FOUND:
                            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid admin email or password.");
                            break;
                        case CONNECTION_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Connection Error", "Failed to connect to the database.");
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "System Error", "An error occurred during login.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Critical Error", "An unexpected system error occurred.");
                });
            }
        }).start();
    }

    
    private void deployAdminDashboard(ActionEvent event) {
        System.out.println("Redirecting to AdminDashboardView...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/AdminMainView.fxml")); 
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            
            stage.getScene().setRoot(root);
            
            
        } catch (IOException e) {
            System.err.println("Error occurred while loading AdminMainView!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToAdminRegister(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminRegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error occurred while loading AdminRegisterView!");
            e.printStackTrace();
        }
    }

    
    @FXML
    public void goToAdminResetPassword(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminResetPasswordView.fxml")); 
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error occurred while loading AdminResetPasswordView!");
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
            System.err.println("Error occurred while loading SelectionView!");
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