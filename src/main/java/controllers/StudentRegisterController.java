package controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

public class StudentRegisterController {

    @FXML private TextField fullnameField;
    @FXML private TextField emailField;
    @FXML private TextField studentIdField;
    @FXML private PasswordField passwordField;

    // Çift tıklamayı önlemek için kilit
    private boolean isProcessing = false;

    // Veritabanı ve Manager bağlantılarını başlatıyoruz
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptRegister(ActionEvent event) {
        
        // Eğer zaten kayıt işlemi sürüyorsa, butona tekrar basılmasını yoksay
        if (isProcessing) {
            return; 
        }

        String name = fullnameField.getText();
        String email = emailField.getText();
        String studentId = studentIdField.getText();
        String password = passwordField.getText();
        
        // 1. Boş alan kontrolü
        if (name == null || name.isEmpty() || 
            email == null || email.isEmpty() || 
            studentId == null || studentId.isEmpty() || 
            password == null || password.isEmpty()) {
            
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen tüm alanları doldurunuz.");
            return;
        }

        // KİLİDİ KAPAT: İşlem başladı
        isProcessing = true;
        
        // Tıklanan butonu yakala ve orijinal yazısını sakla
        Button clickedButton = (Button) event.getSource();
        String originalButtonText = clickedButton.getText(); 
        
        // ODAK HİLESİ: Username kutusunun seçilmesini engellemek için odağı arka plana at
        clickedButton.getParent().requestFocus();

        // Butonu inaktif (silik) yap ve yazısını değiştir
        clickedButton.setDisable(true);
        clickedButton.setText("Kayıt Yapılıyor...");

        // Arka plan işlemi başlatılıyor (Ekran donmasın diye)
        new Thread(() -> {
            
            // Veritabanı ve Mail atma işlemi
            DbStatus registerStatus = authManager.registerStudent(email, password, studentId, name);

            // İşlem bitti! Arayüze geri dön
            Platform.runLater(() -> {
                
                // KİLİDİ AÇ VE BUTONU ESKİ HALİNE GETİR
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalButtonText);

                // Veritabanından dönen sonuca göre işlem yap
                switch (registerStatus) {
                    case SUCCESS:
                        System.out.println("Kayıt başarılı! Aktivasyon maili gönderildi.");
                        showAlert(Alert.AlertType.INFORMATION, "Kayıt Başarılı", "Hesabınız oluşturuldu. Lütfen Bilkent e-posta adresinize gönderilen aktivasyon kodunu giriniz.");
                        goToActivation(event);
                        break;
                    case EMAIL_ALREADY_EXISTS:
                        showAlert(Alert.AlertType.ERROR, "Kayıt Başarısız", "Bu e-posta adresi ile sistemde zaten bir kayıt mevcut.");
                        break;
                    case ID_ALREADY_EXISTS:
                        showAlert(Alert.AlertType.ERROR, "Kayıt Başarısız", "Bu öğrenci numarası ile sistemde zaten bir kayıt mevcut.");
                        break;
                    case CONNECTION_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı. Lütfen internet bağlantınızı kontrol ediniz.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Kayıt sırasında beklenmeyen bir hata oluştu.");
                        break;
                }
            });
            
        }).start(); // Thread'i başlat
    }

    private void goToActivation(ActionEvent event) {
        System.out.println("ActivationView'a yönlendiriliyor...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/ActivationView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("ActivationView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Önceki Ekrana (Login) Dönülüyor...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentLoginView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}