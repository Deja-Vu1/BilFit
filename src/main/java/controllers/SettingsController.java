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

public class SettingsController {

    @FXML private Button publicAccountToggle;
    @FXML private Button eloToggle;
    @FXML private TextField passwordField;
    @FXML private Button changePasswordBtn;

    private StudentManager studentManager = new StudentManager(Database.getInstance());
    private AuthManager authManager = new AuthManager(Database.getInstance());
    private boolean isProcessing = false;

    private final String COLOR_ON = "-fx-background-color: #1E8E3E; -fx-background-radius: 20;";
    private final String COLOR_OFF = "-fx-background-color: #E2E8F0; -fx-background-radius: 20;";

    @FXML
    public void initialize() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateToggleVisual(publicAccountToggle, currentUser.isPublicProfile());
            updateToggleVisual(eloToggle, currentUser.isEloMatchingEnabled());
        }
    }

    private void updateToggleVisual(Button btn, boolean isOn) {
        if (btn != null) {
            btn.setStyle(isOn ? COLOR_ON : COLOR_OFF);
        }
    }

    @FXML
    public void handleTogglePublic(ActionEvent event) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // 1. OPTIMISTIC UPDATE: Butonu ve hafızayı beklemeden ANINDA değiştir (Şimşek hızı!)
        boolean targetStatus = !currentUser.isPublicProfile(); 
        currentUser.setPublicProfile(targetStatus);
        updateToggleVisual(publicAccountToggle, targetStatus);

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
                    updateToggleVisual(publicAccountToggle, !targetStatus);
                    showCustomAlert("Hata", "Gizlilik ayarı değiştirilemedi. Sunucu hatası.");
                });
            }
        }).start();
    }

    @FXML
    public void handleToggleElo(ActionEvent event) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // 1. OPTIMISTIC UPDATE: ANINDA DEĞİŞTİR
        boolean targetStatus = !currentUser.isEloMatchingEnabled();
        currentUser.setEloMatchingEnabled(targetStatus);
        updateToggleVisual(eloToggle, targetStatus);

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
                    updateToggleVisual(eloToggle, !targetStatus);
                    showCustomAlert("Hata", "ELO ayarı değiştirilemedi. Sunucu hatası.");
                });
            }
        }).start();
    }

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
        dialogStage.initStyle(StageStyle.TRANSPARENT); // Arka planı şeffaf yapıp kendi köşelerimizi çizeceğiz

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
        
        // Tasarımındaki o güzel gölgelendirme efekti
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
        scene.setFill(Color.TRANSPARENT); // Siyah kare çıkmasını engeller
        dialogStage.setScene(scene);
        
        // Ortada açılması için
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }
}