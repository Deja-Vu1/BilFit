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
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/ResetPasswordView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("ResetPasswordView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }
    @FXML
    public void attemptLogin(ActionEvent event) {
        System.out.println("Attempting to reset password...");
        String password=passwordField.getText();
        /* TODO: password'u database'de güncelle
                if (mail.isEmpty()) {
            System.out.println("Error: Activation code cannot be empty.");
            return;
        }
            */
           System.out.println("Mail doğrulandı, SelectionView'e yönlendiriliyor...");
           checkActivationCode(event);
    }
        public void checkActivationCode(ActionEvent event) {
        System.out.println("Redirecting to SelectionView");
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("SelectionView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
        
    }


}