package controllers;

import java.io.IOException;

import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import managers.AuthManager;

public class AdminRegisterController {

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField adminAccessCodeField;
    private boolean isProcessing = false;
    @FXML
    public void attemptAdminRegister(ActionEvent event) {
        
        if (isProcessing) {
            return;
        }

        String name = fullnameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        
        if (name == null || name.isEmpty() || email == null || email.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen tüm alanları doldurunuz.");
            return;
        }
        if (!email.endsWith("@ug.bilkent.edu.tr") && !email.endsWith("@alumni.bilkent.edu.tr") && !email.endsWith("@bilkent.edu.tr")) {
            showAlert(Alert.AlertType.WARNING, "Geçersiz E-posta", "Sisteme sadece Bilkent e-posta adresleri ile kayıt olunabilir.");
            return;
        }

        // FXML'de admin yetki kodu kutusu olmadığı için şimdilik manuel gönderiyoruz
        String adminSecretCode = "ADMIN_KOD"; 

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Kayıt Yapılıyor...");

        new Thread(() -> {
            
            // AuthManager üzerinden Admin kaydı işlemi
            DbStatus registerStatus = AuthManager.registerAdmin(email, password, adminSecretCode, name);

            Platform.runLater(() -> {
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalText);

                switch (registerStatus) {
                    case SUCCESS:
                       
                        
                        goToAdminLogin(event);
                        break;
                    case EMAIL_ALREADY_EXISTS:
                        showAlert(Alert.AlertType.ERROR, "Kayıt Başarısız", "Bu e-posta adresi ile zaten bir admin kaydı mevcut.");
                        break;
                    case CONNECTION_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Admin kaydı sırasında bir hata oluştu veya aktivasyon kodunuz geçersiz.");
                        break;
                }
            });
            
        }).start();
    }

     // Başarılı kayıttan sonra Login sayfasına yönlendiren yardımcı metod
    private void goToAdminLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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