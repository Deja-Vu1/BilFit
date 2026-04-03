package controllers; import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
public class SettingsController { 
    @FXML private PasswordField passwordField;
    @FXML public void initialize() {} 
    @FXML
        public void resetPassword(ActionEvent event) {
        System.out.println("Attempting to reset password...");
        String password=passwordField.getText();
        /* TODO: password'u database'de güncelle
                if (password.isEmpty()) {
            System.out.println("Error: Password cannot be empty.");
            return;
        }
            */
           System.out.println("Şifre değiştirildi");
    }
    @FXML public void makeAccountPublic(ActionEvent event) {
        System.out.println("Returning to main menu...");
    }


}