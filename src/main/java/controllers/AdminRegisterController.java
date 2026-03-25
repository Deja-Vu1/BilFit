package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class AdminRegisterController {

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void attemptAdminRegister(ActionEvent event) {
        String name = fullnameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        
        System.out.println("Attempting Admin Registration for: " + name);
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            System.out.println("Error: All fields must be filled.");
            return;
        }
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to Admin Login Screen...");
    }
}