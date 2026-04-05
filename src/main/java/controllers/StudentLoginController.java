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

public class StudentLoginController {

   @FXML private TextField emailField;
   @FXML private PasswordField passwordField;

   private boolean isProcessing = false;
   private Database db = Database.getInstance();
   private AuthManager authManager = new AuthManager(db);

   @FXML
   public void attemptLogin(ActionEvent event) {
       if (isProcessing) return;

       String emailInput = emailField.getText();
       String passwordInput = passwordField.getText();

       if (emailInput == null || emailInput.isEmpty() || passwordInput == null || passwordInput.isEmpty()) {
           showAlert(Alert.AlertType.WARNING, "Warning", "Email or password fields cannot be left empty.");
           return;
       }

       isProcessing = true;
       Button clickedButton = (Button) event.getSource();
       String originalButtonText = clickedButton.getText();
      
       clickedButton.getParent().requestFocus();
       clickedButton.setDisable(true);
       clickedButton.setText("Logging in...");

       new Thread(() -> {
           try {
               DbStatus loginStatus = authManager.loginStudent(emailInput, passwordInput);

               Platform.runLater(() -> {
                   isProcessing = false;
                   clickedButton.setDisable(false);
                   clickedButton.setText(originalButtonText);

                   switch (loginStatus) {
                       case SUCCESS:
                           deployHomepage(event);
                           break;
                       case ACCOUNT_NOT_ACTIVATED:
                           showAlert(Alert.AlertType.INFORMATION, "Activation Required", "Your account has not been activated yet. Please check your email.");
                           break;
                       case INVALID_CREDENTIALS:
                       case DATA_NOT_FOUND:
                           showAlert(Alert.AlertType.ERROR, "Login Failed", "You entered incorrect email or password.");
                           break;
                       case CONNECTION_ERROR:
                           showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the database.");
                           break;
                       default:
                           showAlert(Alert.AlertType.ERROR, "System Error", "An unknown error occurred.");
                           break;
                   }
               });
           } catch (Exception ex) {
               ex.printStackTrace();
               Platform.runLater(() -> {
                   isProcessing = false;
                   clickedButton.setDisable(false);
                   clickedButton.setText(originalButtonText);
                   showAlert(Alert.AlertType.ERROR, "Critical Code Error", "Login process crashed in the background:\n" + ex.getMessage());
               });
           }
       }).start();
   }

   public void deployHomepage(ActionEvent event) {
       try {
           java.net.URL url = getClass().getResource("/views/dashboard/StudentMainView.fxml");
           
           if (url == null) {
               showAlert(Alert.AlertType.ERROR, "File Not Found", "StudentMainView.fxml file is not in the folder or the name is wrong!");
               return;
           }

           FXMLLoader loader = new FXMLLoader(url);
           Parent root = loader.load();
           Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
           stage.getScene().setRoot(root);
           
       } catch (Exception e) {
           e.printStackTrace();
           showAlert(Alert.AlertType.ERROR, "Interface Crashed", "An error occurred while loading the homepage:\n" + e.getMessage());
       }
   }

   @FXML
   public void goToRegister(MouseEvent event) {
       try {
           FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentRegisterView.fxml"));
           Parent root = loader.load();
           Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
           stage.getScene().setRoot(root);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   @FXML
   public void goToForgotPassword(MouseEvent event) {
       try {
           FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/ResetPasswordView.fxml"));
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
       alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
       
       try {
           alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
       } catch (Exception e) {}

       if (emailField.getScene() != null) {
           Stage stage = (Stage) emailField.getScene().getWindow();
           alert.initOwner(stage);
       }
       alert.showAndWait();
   }
}