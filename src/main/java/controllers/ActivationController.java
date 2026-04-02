package controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ActivationController {

    @FXML private TextField activationCodeField;

    @FXML
    public void submitCode(ActionEvent event) {
        String code = activationCodeField.getText();
        
        System.out.println("Verifying Activation Code: " + code);
        
        /* TODO: Kodu doğrula
                if (code.isEmpty()) {
            System.out.println("Error: Activation code cannot be empty.");
            return;
        }
            */
    }

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
}