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
import javafx.scene.Node;

import database.Database;
import database.DbStatus;
import managers.AuthManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StudentRegisterController {

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

        String name = fullnameField.getText() != null ? fullnameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim().toLowerCase() : "";
        String studentId = studentIdField.getText() != null ? studentIdField.getText().trim() : "";
        String password = passwordField.getText(); 
      
        if (name.isEmpty() || email.isEmpty() || studentId.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen tüm alanları doldurunuz.");
            return;
        }
        
        if (!email.endsWith("@ug.bilkent.edu.tr") && !email.endsWith("@bilkent.edu.tr") && !email.endsWith("@alumni.bilkent.edu.tr")) {
            showAlert(Alert.AlertType.WARNING, "Geçersiz E-posta", "Sisteme sadece Bilkent e-posta adresleri ile kayıt olunabilir.");
            return;
        }

        // DB'nin reddetmesini engellemek için ID kuralı eklendi (Sadece sayılar ve tam 8 hane)
        if (!studentId.matches("\\d+") || studentId.length() < 7 || studentId.length() > 9) {
            showAlert(Alert.AlertType.WARNING, "Geçersiz ID", "Öğrenci numaranız sadece rakamlardan oluşmalı ve geçerli uzunlukta (Örn: 22200000) olmalıdır.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Zayıf Şifre", "Şifreniz en az 6 karakter uzunluğunda olmalıdır.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalButtonText = clickedButton.getText();
      
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Kayıt Yapılıyor...");

        new Thread(() -> {
            try {
                // Her şey tertemiz bir şekilde Manager'a iletiliyor
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
                        case QUERY_ERROR:
                            // EĞER HALA BU HATAYI ALIYORSAN AŞAĞIDAKİ YAZIYI OKU!
                            showAlert(Alert.AlertType.ERROR, "Veritabanı Reddi", "Veritabanı bu kaydı reddetti! Ya bilgilerde özel bir karakter var, ya da bu maille önceden yarım kalmış bir kayıt bulunuyor.");
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Kayıt sırasında beklenmeyen bir hata oluştu.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalButtonText);
                });
            }
        }).start();
    }

    private void goToActivation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/ActivationView.fxml"));
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

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch(Exception e) {}

        if (emailField != null && emailField.getScene() != null) {
            Stage stage = (Stage) emailField.getScene().getWindow();
            alert.initOwner(stage);
        }
      
        alert.showAndWait();
    }
}