package controllers; import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
public class NewPasswordController {
    @FXML private PasswordField passwordField;
    @FXML public void initialize() {} 
    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to ResetPassword Screen");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/ResetPasswordView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("An error occurred while loading ResetPasswordView!");
            e.printStackTrace();
        }
    }
    @FXML
    public void attemptLogin(ActionEvent event) {
        System.out.println("Attempting to reset password...");
        String password=passwordField.getText();
           System.out.println("Password changed, redirecting to StudentLoginView...");

           directToHome(event);
    }
        public void directToHome(ActionEvent event) {
        System.out.println("Redirecting to StudentLoginView");
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("An error occurred while loading StudentLoginView!");
            e.printStackTrace();
        }
        
    }
    


}