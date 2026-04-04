package controllers;

import java.io.IOException;
import java.util.Optional;

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

public class AdminActivationController {

    // ÖĞRENCİ TARAFINDAKİ GİBİ CONTEXT (DURUM) YÖNETİMİ EKLENDİ
    public enum ActivationContext {
        REGISTRATION,
        PASSWORD_RESET
    }

    @FXML private TextField activatonCode;
    
    public static String emailToActivate = "";
    public static ActivationContext currentContext = ActivationContext.REGISTRATION;

    private boolean isProcessing = false;
    private Database db = Database.getInstance();
    private AuthManager authManager = new AuthManager(db);

    @FXML
    public void initialize() {} 

    @FXML
    public void submitCode(ActionEvent event) {
        if (isProcessing) return;

        String code = activatonCode != null && activatonCode.getText() != null ? activatonCode.getText().trim() : "";
        
        if (code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Eksik Bilgi", "Lütfen size gönderilen aktivasyon kodunu giriniz.");
            return;
        }

        if (emailToActivate == null || emailToActivate.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "İşlem yapılacak admin e-posta adresi bulunamadı.");
            return;
        }

        isProcessing = true;
        Button clickedButton = (Button) event.getSource();
        String originalText = clickedButton.getText();
        
        clickedButton.getParent().requestFocus();
        clickedButton.setDisable(true);
        clickedButton.setText("Doğrulanıyor...");

        new Thread(() -> {
            try {
                DbStatus status = DbStatus.QUERY_ERROR;
                
                // CONTEXT'E GÖRE FARKLI VERİTABANI İŞLEMİ (Şifre Sıfırlama vs. Kayıt)
                if (currentContext == ActivationContext.REGISTRATION) {
                    status = authManager.activateAccount(emailToActivate, code);
                } else {
                    status = db.verifyActivationCode(emailToActivate, code);
                }

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);

                    switch (finalStatus) {
                        case SUCCESS:
                            // EĞER KAYITSA DİREKT GİRİŞE, ŞİFRE SIFIRLAMAYSA POP-UP'A GÖNDER
                            if (currentContext == ActivationContext.REGISTRATION) {
                                showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Admin hesabınız başarıyla aktive edildi! Giriş yapabilirsiniz.");
                                goToAdminLogin(clickedButton);
                            } else {
                                askForNewPassword(clickedButton);
                            }
                            break;
                        case INVALID_CODE:
                        case DATA_NOT_FOUND:
                            showAlert(Alert.AlertType.ERROR, "Hata", "Girdiğiniz aktivasyon kodu hatalı veya geçersiz.");
                            break;
                        case CONNECTION_ERROR:
                            showAlert(Alert.AlertType.ERROR, "Bağlantı Hatası", "Veritabanına bağlanılamadı.");
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "Sistem Hatası", "Aktivasyon sırasında beklenmeyen bir sorun oluştu.");
                            break;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Kritik Hata", "Sistemde beklenmeyen bir hata oluştu.");
                });
            }
        }).start();
    }

    // ŞİFRE SIFIRLAMA İÇİN YENİ ŞİFRE SORAN DİNAMİK POP-UP
    private void askForNewPassword(Node sourceNode) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Yeni Şifre Belirleme");
        dialog.setHeaderText("Kod doğrulandı. Lütfen yeni admin şifrenizi giriniz:");

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
                            goToAdminLogin(sourceNode);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Hata", "Şifre güncellenirken bir sorun oluştu.");
                        }
                    });
                }).start();
            }
        });
    }

    private void goToAdminLogin(Node sourceNode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminLoginView.fxml"));
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
            // GERİ BUTONU DA CONTEXT'E GÖRE DOĞRU YERE DÖNÜYOR
            String viewPath = (currentContext == ActivationContext.PASSWORD_RESET) 
                              ? "/views/auth/AdminResetPasswordView.fxml" 
                              : "/views/auth/AdminRegisterView.fxml";
                              
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
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
        
        try { alert.initStyle(javafx.stage.StageStyle.UNDECORATED); } catch (Exception ignored) {}
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {}
        
        if (activatonCode != null && activatonCode.getScene() != null) {
            Stage stage = (Stage) activatonCode.getScene().getWindow();
            alert.initOwner(stage);
        }
        
        alert.showAndWait();
    }
}