package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import database.Database;
import database.DbStatus;
import managers.AuthManager;

import java.io.IOException;

public class ActivationController {

    @FXML private TextField activationCodeField;
    public static String emailToActivate = "";

    private boolean isProcessing = false;
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void submitCode(ActionEvent event) {
        if (isProcessing) return;

        String code = activationCodeField.getText();
        
        if (code == null || code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen 6 haneli aktivasyon kodunu giriniz.");
            return;
        }

        if (emailToActivate == null || emailToActivate.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Aktive edilecek e-posta adresi bulunamadı.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Doğrulanıyor...");

        new Thread(() -> {
            DbStatus status = authManager.activateAccount(emailToActivate, code);

            Platform.runLater(() -> {
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalText);

                switch (status) {
                    case SUCCESS:
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Hesabınız aktive edildi! Giriş yapabilirsiniz.");
                        goToLogin(event);
                        break;
                    case INVALID_CREDENTIALS:
                    case DATA_NOT_FOUND:
                        showAlert(Alert.AlertType.ERROR, "Hata", "Aktivasyon kodu hatalı.");
                        break;
                    case CONNECTION_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Hata", "Aktivasyon sırasında sorun oluştu.");
                        break;
                }
            });
        }).start();
    }

    private void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentRegisterView.fxml"));
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