package controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import database.Database;
import database.DbStatus;
import managers.AuthManager;

public class AdminRegisterController {

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    
    // FXML'de eklediğin Admin Kod kutusu buraya bağlandı!
    @FXML private PasswordField adminAccessCodeField;

    private boolean isProcessing = false;
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptAdminRegister(ActionEvent event) {
        if (isProcessing) return;

        // VERİLERİ TEMİZLE: Boşlukları sil, küçük harfe zorla
        String name = fullnameField.getText() != null ? fullnameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim().toLowerCase() : "";
        String password = passwordField.getText();
        String adminSecretCode = adminAccessCodeField != null && adminAccessCodeField.getText() != null ? adminAccessCodeField.getText().trim() : "";
      
        if (name.isEmpty() || email.isEmpty() || password == null || password.isEmpty() || adminSecretCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen tüm alanları (Admin Yetki Kodu dahil) doldurunuz.");
            return;
        }

        if (!email.endsWith("@ug.bilkent.edu.tr") && !email.endsWith("@yahoo.com") && !email.endsWith("@bilkent.edu.tr")) {
            showAlert(Alert.AlertType.WARNING, "Geçersiz E-posta", "Sisteme sadece Bilkent e-posta adresleri ile kayıt olunabilir.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Zayıf Şifre", "Şifreniz en az 6 karakter uzunluğunda olmalıdır.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
      
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Kayıt Yapılıyor...");

        new Thread(() -> {
            try {
                // AuthManager üzerinden Admin kaydı işlemi
                DbStatus registerStatus = authManager.registerAdmin(email, password, adminSecretCode, name);

                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);

                    switch (registerStatus) {
                        case SUCCESS:
                            // Kayıt başarılıysa Aktivasyon sayfasına gönder
                            AdminActivationController.emailToActivate = email;
                            goToAdminActivation(event);
                            break;
                        case EMAIL_ALREADY_EXISTS:
                            showAlert(Alert.AlertType.ERROR, "Kayıt Başarısız", "Bu e-posta adresi ile zaten bir admin kaydı mevcut.");
                            break;
                        case CONNECTION_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                            break;
                        case QUERY_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Geçersiz İşlem", "Veriler veritabanı kurallarına uymuyor veya Admin Yetki Kodu hatalı.");
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Admin kaydı sırasında beklenmeyen bir hata oluştu.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Kritik Hata", "Sistemde beklenmeyen bir hata oluştu.");
                });
            }
        }).start();
    }

    // Doğrudan Login'e değil, aradaki Aktivasyon adımına gönderir
    private void goToAdminActivation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminActivationView.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
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
        
        try { alert.initStyle(javafx.stage.StageStyle.UNDECORATED); } catch (Exception ignored) {}
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {}
        
        if (emailField != null && emailField.getScene() != null) {
            Stage stage = (Stage) emailField.getScene().getWindow();
            alert.initOwner(stage);
        }
      
        alert.showAndWait();
    }
}