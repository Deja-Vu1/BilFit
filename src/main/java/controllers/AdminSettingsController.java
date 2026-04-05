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
import managers.AdminManager;
import managers.SessionManager;
import models.Admin;

public class AdminSettingsController {

    @FXML private TextField nameField;
    @FXML private TextField passwordField;
    @FXML private Button changeNameBtn;
    @FXML private Button changePasswordBtn;

    // MANAGER BAĞLANTISI EKLENDİ
    private AdminManager adminManager = new AdminManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
        if (currentAdmin != null) {
            if (nameField != null) {
                // Kutuya mevcut ismi silik olarak yaz
                nameField.setPromptText("Mevcut: " + currentAdmin.getFullName());
            }
        }
    }

    @FXML
    public void handleChangeName(ActionEvent event) {
        if (isProcessing) return;

        Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
        String newName = nameField.getText();

        if (newName == null || newName.trim().isEmpty()) {
            showCustomAlert("Uyarı", "İsim alanı boş bırakılamaz.");
            return;
        }

        isProcessing = true;
        changeNameBtn.setText("...");

        new Thread(() -> {
            // DÜZELTME: Doğrudan DB yerine AdminManager kullanıyoruz
            DbStatus status = adminManager.updateNickname(currentAdmin, newName.trim());

            Platform.runLater(() -> {
                isProcessing = false;
                changeNameBtn.setText("Change");

                if (status == DbStatus.SUCCESS) {
                    nameField.clear();
                    nameField.setPromptText("Mevcut: " + currentAdmin.getFullName());
                    showCustomAlert("Başarılı", "Yönetici isminiz başarıyla güncellendi.");
                } else {
                    showCustomAlert("Hata", "İsim güncellenirken sunucu kaynaklı bir sorun oluştu.");
                }
            });
        }).start();
    }

    @FXML
    public void handleChangePassword(ActionEvent event) {
        if (isProcessing) return;

        Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
        String newPassword = passwordField.getText();

        if (newPassword == null || newPassword.trim().isEmpty()) {
            showCustomAlert("Uyarı", "Şifre alanı boş olamaz.");
            return;
        }

        // ÖĞRENCİ TARAFINDAKİ 6 KARAKTER KURALI BURAYA DA EKLENDİ!
        if (newPassword.trim().length() < 6) {
            showCustomAlert("Zayıf Şifre", "Yeni şifreniz en az 6 karakter olmalıdır.");
            return;
        }

        isProcessing = true;
        changePasswordBtn.setText("...");

        new Thread(() -> {
            // DÜZELTME: Doğrudan DB yerine AdminManager kullanıyoruz
            DbStatus status = adminManager.updatePassword(currentAdmin, newPassword.trim());

            Platform.runLater(() -> {
                isProcessing = false;
                changePasswordBtn.setText("Change");

                if (status == DbStatus.SUCCESS) {
                    passwordField.clear();
                    showCustomAlert("Başarılı", "Şifreniz başarıyla değiştirildi.");
                } else if (status == DbStatus.SAME_PASSWORD) {
                    showCustomAlert("Hata", "Yeni şifre eskisinden farklı olmalıdır.");
                } else {
                    showCustomAlert("Hata", "Şifre güncellenirken sunucu kaynaklı bir sorun oluştu.");
                }
            });
        }).start();
    }

    // Şık Pop-up uyarımız
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
        okBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        okBtn.setOnAction(e -> dialogStage.close());

        layout.getChildren().addAll(titleLabel, msgLabel, okBtn);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }
}