package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import managers.AuthManager;
import managers.SessionManager;
import managers.StudentManager;
import models.SportType;
import models.Student;

// ControlsFX ToggleSwitch importu
import org.controlsfx.control.ToggleSwitch; 

public class SettingsController {

    @FXML private ToggleSwitch publicAccountSwitch;
    @FXML private ToggleSwitch eloSwitch;
    @FXML private TextField passwordField;
    @FXML private Button changePasswordBtn;

    private StudentManager studentManager = new StudentManager(Database.getInstance());
    private AuthManager authManager = new AuthManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            // 1. Sayfa açıldığında değerleri kullanıcının ayarlarına göre set et
            publicAccountSwitch.setSelected(currentUser.isPublicProfile());
            eloSwitch.setSelected(currentUser.isEloMatchingEnabled());

            // 2. Public Account için Listener (Değişiklikleri dinle)
            publicAccountSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
                // Sadece değer gerçekten değiştiğinde DB isteği at (sayfa yüklenirken atmaması için)
                if (currentUser.isPublicProfile() != newValue) {
                    updatePublicAccount(currentUser, newValue);
                }
            });

            // 3. ELO için Listener (Değişiklikleri dinle)
            eloSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (currentUser.isEloMatchingEnabled() != newValue) {
                    updateEloSetting(currentUser, newValue);
                }
            });
        }
    }

    // --- TOGGLE SWITCH VERİTABANI METOTLARI ---

    private void updatePublicAccount(Student currentUser, boolean targetStatus) {
        // 1. OPTIMISTIC UPDATE: Beklemeden anında RAM'de değiştir
        currentUser.setPublicProfile(targetStatus);

        // 2. ARKA PLAN: Veritabanına bildir
        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = studentManager.updateProfileVisibility(currentUser, targetStatus);
            } catch (Exception e) {}

            // 3. ROLLBACK: Eğer DB hata verirse, çaktırmadan eski haline çevir
            if (status != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    currentUser.setPublicProfile(!targetStatus);
                    publicAccountSwitch.setSelected(!targetStatus);
                    showCustomAlert("Hata", "Gizlilik ayarı değiştirilemedi. Sunucu hatası.");
                });
            }
        }).start();
    }

    private void updateEloSetting(Student currentUser, boolean targetStatus) {
        // 1. OPTIMISTIC UPDATE
        currentUser.setEloMatchingEnabled(targetStatus);

        // 2. ARKA PLAN İŞLEMİ
        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = studentManager.toggleEloMatching(currentUser, targetStatus);
            } catch (Exception e) {}

            // 3. ROLLBACK (Hata olursa geri al)
            if (status != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    currentUser.setEloMatchingEnabled(!targetStatus);
                    eloSwitch.setSelected(!targetStatus);
                    showCustomAlert("Hata", "ELO ayarı değiştirilemedi. Sunucu hatası.");
                });
            }
        }).start();
    }

    // --- ŞİFRE VE İLGİ ALANI METOTLARI  ---

    @FXML
    public void handleChangePassword(ActionEvent event) {
        if (isProcessing) return;
        
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        String newPass = passwordField.getText();

        if (newPass == null || newPass.trim().isEmpty()) {
            showCustomAlert("Uyarı", "Şifre alanı boş olamaz.");
            return;
        }

        isProcessing = true;
        changePasswordBtn.setText("...");

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = studentManager.updatePassword(currentUser, newPass);
            } catch (Exception e) {}

            final DbStatus finalStatus = status;

            Platform.runLater(() -> {
                isProcessing = false;
                changePasswordBtn.setText("Change");
                
                if (finalStatus == DbStatus.SUCCESS) {
                    passwordField.clear();
                    showCustomAlert("Başarılı", "Şifreniz başarıyla değiştirildi.");
                } else {
                    showCustomAlert("Hata", "Şifre güncellenemedi. Lütfen tekrar deneyin.");
                }
            });
        }).start();
    }

    @FXML
    public void handleRemoveInterest(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        
        String sportName = clickedBtn.getText().replace("🗑 ", "").trim().toUpperCase();
        
        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                SportType type = SportType.valueOf(sportName);
                status = studentManager.removeInterest(currentUser, type);
            } catch (Exception e) {}
            
            final DbStatus finalStatus = status;
            
            Platform.runLater(() -> {
                if (finalStatus == DbStatus.SUCCESS) {
                    clickedBtn.setVisible(false); 
                    clickedBtn.setManaged(false);
                    showCustomAlert("Silindi", sportName + " ilgi alanlarınızdan çıkarıldı.");
                } else {
                    showCustomAlert("Hata", "Veritabanından silinemedi.");
                }
            });
        }).start();
    }

    @FXML
    public void handleAddInterest(ActionEvent event) {
         showCustomAlert("Eklenecek", "Add Interest için pop-up tasarımı gelince Manager'a bağlanacak.");
    }

    // %100 BİLFİT TASARIMINA UYGUN, ÖZEL YAPIM POP-UP
    private void showCustomAlert(String title, String message) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT); 

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
        
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        layout.setEffect(shadow);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");

        Button okBtn = new Button("Tamam");
        okBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40;");
        okBtn.setOnAction(e -> dialogStage.close());

        layout.getChildren().addAll(titleLabel, msgLabel, okBtn);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }
}