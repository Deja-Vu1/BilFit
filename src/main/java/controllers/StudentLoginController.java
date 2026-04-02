package controllers;

import java.io.IOException;

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

public class StudentLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void attemptLogin(ActionEvent event) {
        
        String emailInput = emailField.getText();
        String passwordInput = passwordField.getText();

       /*  // 1. Boş alan kontrolü
        if (emailInput == null || emailInput.isEmpty() || passwordInput == null || passwordInput.isEmpty()) {
            System.out.println("Error: Email or password fields cannot be empty.");
            return;
        }

        boolean isLogged = false;

        for (Student student : StudentRegisterController.temporaryDatabase) {
            if (student.login(emailInput, passwordInput)) {
                System.out.println(" Login successful! \n Welcome, " + student.getNickname());
                deployHomepage(event);
                isLogged = true;
                break;
            }
        }

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
            
        }).start(); */// Thread'i başlat
                        System.out.println("Giriş başarılı!");
                        deployHomepage(event);
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

    @FXML
    public void goToRegister(MouseEvent event) {
        System.out.println("Redirecting to StudentRegisterView");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentRegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentRegisterView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToForgotPassword(MouseEvent event) {
        System.out.println("Şifremi Unuttum ekranına yönlendiriliyor...");
                try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/ResetPasswordView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("ResetPasswordView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }


    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to Main Selection Screen");
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
}