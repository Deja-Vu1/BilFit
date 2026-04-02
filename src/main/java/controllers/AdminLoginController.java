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
        
        /*if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen e-posta ve şifre alanlarını doldurunuz.");
            return;
        }

        isProcessing = true;

        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
        
        // ODAK HİLESİ: Username kutusunun seçilmesini engelle
        clickedButton.getParent().requestFocus();

        clickedButton.setDisable(true);
        clickedButton.setText("Giriş Yapılıyor...");

        // Ekran donmasın diye arka planda çalıştırıyoruz
        new Thread(() -> {
            
            // AuthManager üzerinden Admin girişi
            DbStatus loginStatus = authManager.loginAdmin(email, password);

            Platform.runLater(() -> {
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalText);

                switch (loginStatus) {
                    case SUCCESS:
                        System.out.println("Admin girişi başarılı!");
                        deployAdminDashboard(event);
                        break;
                    case INVALID_CREDENTIALS:
                    case DATA_NOT_FOUND:
                        showAlert(Alert.AlertType.ERROR, "Giriş Başarısız", "Hatalı admin e-postası veya şifre girdiniz.");
                        break;
                    case CONNECTION_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Bilinmeyen bir hata oluştu.");
                        break;
                }
            });
            
        }).start();*/
                                System.out.println("Giriş başarılı!");
                        deployHomepage(event);
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
    
    @FXML
    public void goToAdminResetPassword(MouseEvent event){
        try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminResetPasswordView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}