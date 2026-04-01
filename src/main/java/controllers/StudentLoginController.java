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

public class StudentLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private boolean isProcessing = false;
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void attemptLogin(ActionEvent event) {
        if (isProcessing) return;

        String emailInput = emailField.getText();
        String passwordInput = passwordField.getText();

        if (emailInput == null || emailInput.isEmpty() || passwordInput == null || passwordInput.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Uyarı", "E-posta veya şifre alanları boş bırakılamaz.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalButtonText = clickedButton.getText();
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Giriş Yapılıyor...");

        new Thread(() -> {
            DbStatus loginStatus = authManager.loginStudent(emailInput, passwordInput);

            Platform.runLater(() -> {
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalButtonText);

                switch (loginStatus) {
                    case SUCCESS:
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
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Bilinmeyen bir hata oluştu.");
                        break;
                }
            });
        }).start();
    }

    public void deployHomepage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/MainDashboardView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToRegister(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentRegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToForgotPassword(MouseEvent event) {
        System.out.println("Şifremi Unuttum ekranına yönlendiriliyor...");
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
        alert.showAndWait();
    }
}