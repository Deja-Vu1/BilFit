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
        if (isProcessing) return;

        // VERİLERİ TEMİZLE: Boşlukları sil ve küçük harfe zorla (SQL hatasını önler)
        String email = emailField.getText() != null ? emailField.getText().trim().toLowerCase() : "";
        String password = passwordField.getText();
      
        if (email.isEmpty() || password == null || password.isEmpty()) {
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
            try {
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
                            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Giriş sırasında bir hata oluştu.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Kritik Hata", "Beklenmeyen bir sistem hatası oluştu.");
                });
            }
        }).start();
    }

    // Başarılı girişte Admin Paneline yönlendirme
    private void deployAdminDashboard(ActionEvent event) {
        System.out.println("AdminDashboardView'a yönlendiriliyor...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/AdminMainView.fxml")); // Admin main view olarak varsayıldı
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Eğer AdminMainView'ı sidebar'lı bir yapı yaptıysanız doğrudan root olarak set edebiliriz
            stage.getScene().setRoot(root);
            
            // Not: İstersen yukarıdaki setRoot yerine senin orijinal Scene yaratma kodunu da kullanabilirsin
        } catch (IOException e) {
            System.err.println("AdminMainView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToAdminRegister(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminRegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // FXML'DE OLUP JAVA'DA EKSİK OLAN METOT EKLENDİ!
    @FXML
    public void goToAdminResetPassword(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminResetPasswordView.fxml")); // Veya AdminResetPasswordInputView.fxml
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Şifre sıfırlama sayfası yüklenemedi.");
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

    // POP-UP TASARIM KORUMASI (Try-Catch kalkanlı)
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