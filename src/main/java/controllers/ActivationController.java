package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    
    public static String emailToActivate = "";
    public static ActivationContext currentContext = ActivationContext.REGISTRATION;

    private boolean isProcessing = false;
    /*private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);*/

    @FXML
    public void submitCode(ActionEvent event) {
        if (isProcessing) return;

        // Kodu boşluklardan temizleyelim
        String code = activationCodeField.getText() != null ? activationCodeField.getText().trim() : "";
        
        if (code.isEmpty()) {
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
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                // AUTH MANAGER BAĞLANTISI BURADA
                if (currentContext == ActivationContext.REGISTRATION) {
                    status = authManager.activateAccount(emailToActivate, code);
                } else {
                    status = db.verifyActivationCode(emailToActivate, code);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final DbStatus finalStatus = status;

            Platform.runLater(() -> {
                isProcessing = false;
                clickedButton.setDisable(false);
                clickedButton.setText(originalText);

                switch (finalStatus) {
                    case SUCCESS:
                        if (currentContext == ActivationContext.REGISTRATION) {
                            showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Hesabınız aktive edildi! Giriş yapabilirsiniz.");
                            goToLogin(clickedButton);
                        } else {
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
                    case QUERY_ERROR:
                        showAlert(Alert.AlertType.ERROR, "Hata", "Girdiğiniz kod formatı geçersiz.");
                        break;
                    default:
                        showAlert(Alert.AlertType.ERROR, "Hata", "İşlem sırasında bilinmeyen bir sorun oluştu.");
                        break;
                }
            });
        }).start();*/
        goToLogin(event);
    }

    private void askForNewPassword(Node sourceNode) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Yeni Şifre Belirleme");
        dialog.setHeaderText("Kod doğrulandı. Lütfen yeni şifrenizi giriniz:");

        ButtonType saveButtonType = new ButtonType("Şifreyi Güncelle", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("En az 6 haneli yeni şifre");
        passwordField.setStyle("-fx-pref-width: 250px; -fx-pref-height: 35px;");
        dialog.getDialogPane().setContent(passwordField);

        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {}

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(newPassword -> {
            // Şifre uzunluğu kuralı kontrolü
            if (newPassword == null || newPassword.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Şifre boş bırakılamaz!");
            } else if (newPassword.length() < 6) {
                showAlert(Alert.AlertType.WARNING, "Zayıf Şifre", "Şifreniz en az 6 karakter olmalıdır.");
            } else {
                new Thread(() -> {
                    DbStatus updateStatus = db.updatePassword(emailToActivate, newPassword);
                    Platform.runLater(() -> {
                        if (updateStatus == DbStatus.SUCCESS) {
                            showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Şifreniz başarıyla güncellendi! Giriş yapabilirsiniz.");
                            goToLogin(sourceNode);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Hata", "Şifre güncellenirken bir sorun oluştu.");
                        }
                    });
                }).start();
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
        try {
            String viewPath = (currentContext == ActivationContext.PASSWORD_RESET) 
                              ? "/views/auth/ResetPasswordView.fxml" 
                              : "/views/auth/StudentRegisterView.fxml";
                              
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TASARIM KORUNDU
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {}
        
        if (activationCodeField != null && activationCodeField.getScene() != null) {
            Stage stage = (Stage) activationCodeField.getScene().getWindow();
            alert.initOwner(stage);
        }
        
        alert.showAndWait();
    }
}