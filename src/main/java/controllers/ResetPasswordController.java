package controllers;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.Node;


import database.Database;
import database.DbStatus;


import java.io.IOException;
import java.net.URL;


public class ResetPasswordController {


   @FXML private TextField emailField;


   private boolean isProcessing = false;
   private Database db = Database.getInstance();


   @FXML
   public void sendResetCode(ActionEvent event) {
       if (isProcessing) return;


       String emailInput = emailField.getText();


       if (emailInput == null || emailInput.trim().isEmpty()) {
           showAlert(Alert.AlertType.WARNING, "Warning", "Please enter your Bilkent email address.");
           return;
       }


       isProcessing = true;
       Button clickedButton = (Button) event.getSource();
       String originalButtonText = clickedButton.getText();
      
       clickedButton.getParent().requestFocus();
       clickedButton.setDisable(true);
       clickedButton.setText("Sending Code...");


       new Thread(() -> {
           DbStatus status = db.createActivationCode(emailInput);


           Platform.runLater(() -> {
               isProcessing = false;
               clickedButton.setDisable(false);
               clickedButton.setText(originalButtonText);


               switch (status) {
                   case SUCCESS:
                       goToActivationScreen(event, emailInput);
                       break;
                   case DATA_NOT_FOUND:
                       showAlert(Alert.AlertType.ERROR, "Error", "No account found registered to this email address.");
                       break;
                   case CONNECTION_ERROR:
                       showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to the database.");
                       break;
                   default:
                       showAlert(Alert.AlertType.ERROR, "System Error", "An unknown error occurred.");
                       break;
               }
           });
       }).start();
   }


   private void goToActivationScreen(ActionEvent event, String email) {
       try {
           ActivationController.emailToActivate = email;
           ActivationController.currentContext = ActivationController.ActivationContext.PASSWORD_RESET;


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
      
       URL cssUrl = getClass().getResource("/views/dashboard/bilfit-exact.css");
       if (cssUrl != null) {
           alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
       }


       if (emailField.getScene() != null) {
           Stage stage = (Stage) emailField.getScene().getWindow();
           alert.initOwner(stage);
       }
      
       alert.showAndWait();
   }
}



