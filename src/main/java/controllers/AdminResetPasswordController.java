package controllers;

import java.io.IOException;
import java.net.URL;

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

public class AdminResetPasswordController {

    @FXML private TextField emailField;
    
    private boolean isProcessing = false;
    private Database db = Database.getInstance();

    @FXML
    public void initialize() {} 

    @FXML
    public void sendActivationCode(ActionEvent event) {
        if (isProcessing) return;

        // VERİLERİ TEMİZLE
        String email = emailField.getText() != null ? emailField.getText().trim().toLowerCase() : "";

        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen şifresini sıfırlamak istediğiniz e-posta adresini giriniz.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Kod Gönderiliyor...");

        new Thread(() -> {
            try {
                // DOĞRU METOT: Database içindeki ortak metodu çağırıyoruz
                DbStatus status = db.createActivationCode(email); 

                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);

                    switch (status) {
                        case SUCCESS:
                            // AdminActivationController'ı ŞİFRE SIFIRLAMA modunda hazırlıyoruz
                            AdminActivationController.emailToActivate = email;
                            AdminActivationController.currentContext = AdminActivationController.ActivationContext.PASSWORD_RESET;
                            
                            showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Şifre sıfırlama kodu e-posta adresinize gönderildi!");
                            goToActivationPage(event);
                            break;
                        case DATA_NOT_FOUND:
                            showAlert(Alert.AlertType.ERROR, "Hata", "Bu e-posta adresine ait bir admin hesabı bulunamadı.");
                            break;
                        case CONNECTION_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Kod gönderilirken bir hata oluştu.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    private void goToActivationPage(ActionEvent event) {
        try {
            // Admin için olan Aktivasyon görünümüne gidiyoruz
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminLoginView.fxml"));
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
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        URL cssUrl = getClass().getResource("/views/dashboard/bilfit-exact.css");
        if (cssUrl != null) {
            alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }
        
        if (emailField != null && emailField.getScene() != null) {
            alert.initOwner(emailField.getScene().getWindow());
        }
        alert.showAndWait();
    }
}