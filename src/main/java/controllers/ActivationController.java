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
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import database.Database;
import database.DbStatus;
import managers.AuthManager;

import java.io.IOException;
import java.util.Optional;

public class ActivationController {

    public enum ActivationContext {
        REGISTRATION,
        PASSWORD_RESET
    }

    @FXML private TextField activationCodeField;
    
    // Diğer sayfalardan bu ekrana geçerken atanacak statik değişkenler
    public static String emailToActivate = "";
    public static ActivationContext currentContext = ActivationContext.REGISTRATION;

    private boolean isProcessing = false;
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void submitCode(ActionEvent event) {
        if (isProcessing) return;

        String code = activationCodeField.getText();
        
        if (code == null || code.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen 6 haneli aktivasyon kodunu giriniz.");
            return;
        }

        if (emailToActivate == null || emailToActivate.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "İşlem yapılacak e-posta adresi bulunamadı.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Doğrulanıyor...");

        new Thread(() -> {
            DbStatus status;
            
            // CONTEXT'e göre farklı manager/database metodu çağırıyoruz
            if (currentContext == ActivationContext.REGISTRATION) {
                status = authManager.activateAccount(emailToActivate, code);
            } else {
                // Şifre sıfırlama: Sadece kodu doğrula
                status = db.verifyActivationCode(emailToActivate, code);
            }

            Platform.runLater(() -> {
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalText);

                switch (status) {
                    case SUCCESS:
                        if (currentContext == ActivationContext.REGISTRATION) {
                            showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Hesabınız aktive edildi! Giriş yapabilirsiniz.");
                            goToLogin(clickedButton);
                        } else {
                            // Şifre sıfırlama için kod doğruysa yeni şifre penceresi aç
                            askForNewPassword(clickedButton);
                        }
                        break;
                    case INVALID_CODE:
                    case INVALID_CREDENTIALS:
                    case DATA_NOT_FOUND:
                        showAlert(Alert.AlertType.ERROR, "Hata", "Girdiğiniz kod hatalı veya süresi dolmuş.");
                        break;
                    case CONNECTION_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Hata", "İşlem sırasında bilinmeyen bir sorun oluştu.");
                        break;
                }
            });
        }).start();
    }

    // Şifre sıfırlama için yeni şifre isteyen dinamik JavaFX Popup'ı
    private void askForNewPassword(Node sourceNode) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Yeni Şifre Belirleme");
        dialog.setHeaderText("Kod doğrulandı. Lütfen yeni şifrenizi giriniz:");

        ButtonType saveButtonType = new ButtonType("Şifreyi Güncelle", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Yeni Şifre");
        passwordField.setStyle("-fx-pref-width: 250px; -fx-pref-height: 35px;");
        dialog.getDialogPane().setContent(passwordField);

        // Sahne css'ini dialog'a da ekleyelim (Uygulamanın temasını bozmamak için)
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(newPassword -> {
            if (newPassword.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Şifre boş bırakılamaz!");
            } else {
                DbStatus updateStatus = db.updatePassword(emailToActivate, newPassword);
                if (updateStatus == DbStatus.SUCCESS) {
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Şifreniz başarıyla güncellendi! Giriş yapabilirsiniz.");
                    goToLogin(sourceNode);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Hata", "Şifre güncellenirken bir sorun oluştu.");
                }
            }
        });
    }

    private void goToLogin(Node sourceNode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to ResetPassword Screen");
        try {
            // Geri butonu context'e göre doğru yere dönmeli
            String viewPath = (currentContext == ActivationContext.PASSWORD_RESET) 
                              ? "/views/auth/ResetPasswordView.fxml" 
                              : "/views/auth/StudentRegisterView.fxml";
                              
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("ResetPasswordView yüklenirken hata oluştu!");
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
        
        if (activationCodeField != null && activationCodeField.getScene() != null) {
            Stage stage = (Stage) activationCodeField.getScene().getWindow();
            alert.initOwner(stage);
        }
        
        alert.showAndWait();
    }
}