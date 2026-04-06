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

public class RegisterActivationController {

    @FXML private TextField activationCodeField;

    @FXML
    public void submitCode(ActionEvent event) {
        String code = activationCodeField.getText();
        
        System.out.println("Verifying Activation Code: " + code);
           
           System.out.println("Code verified, redirecting to NewPasswordView...");
           directHome(event);
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to SelectionView Screen");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("An error occurred while loading SelectionView!");
            e.printStackTrace();
        }
    }
            public void directHome(ActionEvent event) {
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