package controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class AdminRegisterController {

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField adminAccessCodeField;

    @FXML
    public void attemptAdminRegister(ActionEvent event) {

         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminActivationView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);;

        } catch (IOException e) {
            e.printStackTrace();
        }

        /*String name = fullnameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        
        System.out.println("Attempting Admin Registration for: " + name);
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            System.out.println("Error: All fields must be filled.");
            return;
        }*/
    }

    @FXML
    public void goBack(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);;

        } catch (IOException e) {
            System.err.println("SelectionView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
        System.out.println("Redirecting to Selection Screen...");
    }
}