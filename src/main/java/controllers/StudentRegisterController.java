package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import models.Student;
import javafx.scene.Node;

public class StudentRegisterController {

    // Giriş ekranında kullandığın geçici veritabanı simülasyonu
    public static List<Student> temporaryDatabase = new ArrayList<>();

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField studentIdField;
    @FXML private PasswordField passwordField;

    @FXML
    public void attemptRegister(ActionEvent event) {
        String name = fullnameField.getText();
        String email = emailField.getText();
        String studentId = studentIdField.getText();
        String password = passwordField.getText();
        
        System.out.println("Öğrenci Kaydı Alınıyor: " + name + " | ID: " + studentId);
        
        /*if (name.isEmpty() || email.isEmpty() || studentId.isEmpty() || password.isEmpty()) {
            System.out.println("Error: All fields are required.");
            return;
        }

        DB : PostgreSQL INSERT INTO Students sorgusu buraya gelecek.*/
        System.out.println("Kayıt başarılı, Ana Sayfaya yönlendiriliyor...");
        deployHomepage(event);

    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Önceki Ekrana Dönülüyor...");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentLoginView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }
        public void deployHomepage(ActionEvent event) {
        System.out.println("Redirecting to StudentMainView");
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/StudentMainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentMainView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
        
    }
}