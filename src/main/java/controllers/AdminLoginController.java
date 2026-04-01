package controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class AdminLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void attemptAdminLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
        deployHomepage(event);
       /* System.out.println("Admin Girişi Deneniyor. Email: " + email);
        if (email.isEmpty() || password.isEmpty()) {
            System.out.println("Error: Fields cannot be empty.");
            return;
        }*/
    }

    @FXML
    public void goToAdminRegister(MouseEvent event) {
        System.out.println("Admin Kayıt Ekranına Yönlendiriliyor...");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminRegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("AdminRegisterView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Giriş Seçim Ekranına Dönülüyor...");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("SelectionView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }
    
        public void deployHomepage(ActionEvent event) {
        System.out.println("Redirecting to AdminMainView");
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/AdminMainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("AdminMainView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
        
    }
}