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

public class AdminLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    // Çift tıklamayı ve odak kaymasını önlemek için kilit
    private boolean isProcessing = false;

    // Veritabanı ve Manager bağlantıları
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptAdminLogin(ActionEvent event) {
        
        if (isProcessing) {
            return;
        }

        String email = emailField.getText();
        String password = passwordField.getText();
        
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
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
            
        }).start();
    }

    // Başarılı girişte Admin Paneline yönlendirme
    private void deployAdminDashboard(ActionEvent event) {
        System.out.println("AdminDashboardView'a yönlendiriliyor...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/AdminDashboardView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("AdminDashboardView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToAdminRegister(MouseEvent event) {
        System.out.println("Admin Kayıt Ekranına Yönlendiriliyor...");
        try {
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

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