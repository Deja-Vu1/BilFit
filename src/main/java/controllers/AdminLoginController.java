package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class AdminLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void attemptAdminLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
        
        System.out.println("Admin Girişi Deneniyor. Email: " + email);
        if (email.isEmpty() || password.isEmpty()) {
            System.out.println("Error: Fields cannot be empty.");
            return;
        }
    }

    @FXML
    public void goToAdminRegister(MouseEvent event) {
        System.out.println("Admin Kayıt Ekranına Yönlendiriliyor...");
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Giriş Seçim Ekranına Dönülüyor...");
    }
}