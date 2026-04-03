package controllers; import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import org.controlsfx.control.ToggleSwitch;
public class SettingsController { 
    @FXML private PasswordField passwordField;
    @FXML private ToggleSwitch publicAccountSwitch; // <-- Inject switch
    @FXML private ToggleSwitch eloSwitch;
    @FXML public void initialize() {
        // Example: Listen for changes on the Public Account switch
        publicAccountSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Public Account is now: " + (newValue ? "ON" : "OFF"));
            // TODO: Update database or settings manager
        });

        // Example: Listen for changes on the ELO switch
        eloSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ELO is now: " + (newValue ? "ON" : "OFF"));
            // TODO: Update database or settings manager
        });
        
        // If you need to set their default states from the database when loading:
        // publicAccountSwitch.setSelected(true);
    }
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
}