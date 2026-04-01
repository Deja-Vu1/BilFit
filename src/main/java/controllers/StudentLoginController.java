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

public class StudentLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // Çift tıklamayı önlemek için kilit
    private boolean isProcessing = false;

    // Veritabanı ve Manager bağlantılarını başlatıyoruz
    private Database db = new Database();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptLogin(ActionEvent event) {
        
        // Eğer zaten giriş işlemi arka planda sürüyorsa, butona tekrar basılmasını yoksay
        if (isProcessing) {
            return;
        }

        String emailInput = emailField.getText();
        String passwordInput = passwordField.getText();

        // 1. Boş alan kontrolü
        if (emailInput == null || emailInput.isEmpty() || passwordInput == null || passwordInput.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Uyarı", "E-posta veya şifre alanları boş bırakılamaz.");
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
        clickedButton.setText("Giriş Yapılıyor...");

        // Veritabanı işlemini arka plana (ayrı bir Thread'e) alıyoruz ki ekran donmasın
        new Thread(() -> {
            
            // AuthManager üzerinden gerçek veritabanı sorgusu yapılıyor
            DbStatus loginStatus = authManager.loginStudent(emailInput, passwordInput);

            // İşlem bitti! Arayüze (Platform.runLater) geri dönüp sonucu gösteriyoruz
            Platform.runLater(() -> {
                
                // KİLİDİ AÇ VE BUTONU ESKİ HALİNE GETİR
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalButtonText);

                // DbStatus enum'una göre yönlendirme veya hata mesajı
                switch (loginStatus) {
                    case SUCCESS:
                        System.out.println("Giriş başarılı!");
                        deployHomepage(event);
                        break;
                    case ACCOUNT_NOT_ACTIVATED:
                        showAlert(Alert.AlertType.INFORMATION, "Aktivasyon Gerekli", "Hesabınız henüz aktive edilmemiş. Lütfen e-postanızı kontrol edin.");
                        break;
                    case INVALID_CREDENTIALS:
                    case DATA_NOT_FOUND:
                        showAlert(Alert.AlertType.ERROR, "Giriş Başarısız", "Hatalı e-posta veya şifre girdiniz.");
                        break;
                    case CONNECTION_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı. Lütfen internet bağlantınızı kontrol edin.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Bilinmeyen bir hata oluştu.");
                        break;
                }
            });
            
        }).start(); // Thread'i başlat
    }

    public void deployHomepage(ActionEvent event) {
        System.out.println("MainDashboardView'a yönlendiriliyor...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/MainDashboardView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("MainDashboardView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister(MouseEvent event) {
        System.out.println("StudentRegisterView'a yönlendiriliyor...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentRegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("StudentRegisterView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToForgotPassword(MouseEvent event) {
        System.out.println("Şifremi Unuttum ekranına yönlendiriliyor...");
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Seçim ekranına dönülüyor...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("SelectionView yüklenirken hata oluştu!");
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