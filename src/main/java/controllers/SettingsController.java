package controllers;

import java.util.List;

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
import javafx.scene.layout.FlowPane;
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
    @FXML private FlowPane interestsContainer; // FXML'den bu Container'ı alıyoruz

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
            
            // Kullanıcının ilgi alanlarını DB'den çekip ekrana diziyoruz
            loadUserInterests(currentUser);
        }
    }

    private void loadUserInterests(Student currentUser) {
        new Thread(() -> {
            // DB'den taze interest listesini çekiyoruz
            List<SportType> dbInterests = studentManager.getUserInterests(currentUser);
            
            Platform.runLater(() -> {
                if (interestsContainer != null) {
                    interestsContainer.getChildren().clear(); // Eski/sabit verileri temizle
                    
                    if (dbInterests != null && !dbInterests.isEmpty()) {
                        for (SportType sport : dbInterests) {
                            interestsContainer.getChildren().add(createInterestButton(sport));
                        }
                    } else {
                        Label emptyLabel = new Label("Kayıtlı ilgi alanınız bulunmamaktadır.");
                        emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                        interestsContainer.getChildren().add(emptyLabel);
                    }
                }
            });
        }).start();
    }

    private Button createInterestButton(SportType sportType) {
        String displayName = sportType.name().replace("_", " ");
        
        Button btn = new Button("🗑 " + displayName);
        
        // Varsayılan (Default) Tasarım ve Tıklama İmleci (hand cursor) eklendi
        String defaultStyle = "-fx-background-color: #F4F7FE; -fx-text-fill: #4318FF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-cursor: hand;";
        
        // Üzerine Gelince (Hover) Görünecek Kırmızı Tasarım
        String hoverStyle = "-fx-background-color: #D93025; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #D93025; -fx-border-radius: 10; -fx-cursor: hand;";

        btn.setStyle(defaultStyle);
        btn.setPrefHeight(40.0);

        // Mouse butonun üstüne geldiğinde stili kırmızı yap
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        
        // Mouse butonun üzerinden çekildiğinde stili eski haline döndür
        btn.setOnMouseExited(e -> btn.setStyle(defaultStyle));
        
        btn.setOnAction(e -> handleRemoveInterest(sportType, btn));
        return btn;
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

        boolean targetStatus = !currentUser.isPublicProfile(); 
        currentUser.setPublicProfile(targetStatus);
        updateToggleVisual(publicAccountToggle, targetStatus);

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = studentManager.updateProfileVisibility(currentUser, targetStatus);
            } catch (Exception e) {}

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

        boolean targetStatus = !currentUser.isEloMatchingEnabled();
        currentUser.setEloMatchingEnabled(targetStatus);
        updateToggleVisual(eloToggle, targetStatus);

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = studentManager.toggleEloMatching(currentUser, targetStatus);
            } catch (Exception e) {}

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

    // Artık ActionEvent beklemiyor, doğrudan obje ve buton alıyor (Çünkü dinamik yaratıldı)
    private void handleRemoveInterest(SportType sportType, Button clickedBtn) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        
        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = studentManager.removeInterest(currentUser, sportType);
            } catch (Exception e) {}
            
            final DbStatus finalStatus = status;
            
            Platform.runLater(() -> {
                if (finalStatus == DbStatus.SUCCESS) {
                    // DB'den silindiyse, ekrandaki butonu tamamen yok et
                    if (interestsContainer != null) {
                        interestsContainer.getChildren().remove(clickedBtn);
                    }
                    showCustomAlert("Silindi", sportType.name().replace("_", " ") + " ilgi alanlarınızdan çıkarıldı.");
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