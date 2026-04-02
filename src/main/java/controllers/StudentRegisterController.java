package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import models.Student;
import javafx.scene.Node;

import database.Database;
import database.DbStatus;
import managers.AuthManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StudentRegisterController {

    // Giriş ekranında kullandığın geçici veritabanı simülasyonu
    public static List<Student> temporaryDatabase = new ArrayList<>();

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField studentIdField;
    @FXML private PasswordField passwordField;

    private boolean isProcessing = false;
    /*private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);*/

    @FXML
    public void attemptRegister(ActionEvent event) {
        if (isProcessing) return; 

        String name = fullnameField.getText();
        String email = emailField.getText();
        String studentId = studentIdField.getText();
        String password = passwordField.getText();
        
        /*if (name == null || name.isEmpty() || email == null || email.isEmpty() || studentId == null || studentId.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen tüm alanları doldurunuz.");
            return;
        }
        if (!email.endsWith("@ug.bilkent.edu.tr") && !email.endsWith("@alumni.bilkent.edu.tr") && !email.endsWith("@bilkent.edu.tr")) {
            showAlert(Alert.AlertType.WARNING, "Geçersiz E-posta", "Sisteme sadece Bilkent e-posta adresleri ile kayıt olunabilir.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalButtonText = clickedButton.getText(); 
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Kayıt Yapılıyor...");

        new Thread(() -> {
            DbStatus registerStatus = authManager.registerStudent(email, password, studentId, name);

            Platform.runLater(() -> {
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalButtonText);

                switch (registerStatus) {
                    case SUCCESS:
                        ActivationController.emailToActivate = email; 
                        goToActivation(event);
                        break;
                    case EMAIL_ALREADY_EXISTS:
                        showAlert(Alert.AlertType.ERROR, "Kayıt Başarısız", "Bu e-posta adresi sistemde zaten mevcut.");
                        break;
                    case ID_ALREADY_EXISTS:
                        showAlert(Alert.AlertType.ERROR, "Kayıt Başarısız", "Bu öğrenci numarası sistemde zaten mevcut.");
                        break;
                    case CONNECTION_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Kayıt sırasında beklenmeyen bir hata oluştu.");
                        break;
                }
            });
        }).start();*/
        goToActivation(event);
    }

    private void goToActivation(ActionEvent event) {
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goBack(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
        @FXML
        public void checkActivation(ActionEvent event) {
        System.out.println("Redirecting to RegisterActivationView");
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/RegisterActivationView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
         } catch (IOException e) {
            e.printStackTrace();
         }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        // Full-screen pop-up arkaya düşme sorunu çözümü
        if (emailField != null && emailField.getScene() != null) {
            Stage stage = (Stage) emailField.getScene().getWindow();
            alert.initOwner(stage);
        }
        
        alert.showAndWait();
    }
}