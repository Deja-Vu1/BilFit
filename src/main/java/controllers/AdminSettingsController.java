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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import managers.AdminManager;
import managers.SessionManager;
import models.Admin;
import models.Student;

public class AdminSettingsController {

    // KİŞİSEL AYARLAR
    @FXML private TextField nameField;
    @FXML private TextField passwordField;
    @FXML private Button changeNameBtn;
    @FXML private Button changePasswordBtn;

    // CEZA VE BAN YÖNETİMİ
    @FXML private TextField studentEmailField;
    @FXML private TextField penaltyPointsField;
    @FXML private Button addPenaltyBtn;
    @FXML private Button reducePenaltyBtn;
    @FXML private Button unbanStudentBtn;
    @FXML private Button banStudentBtn;

    private AdminManager adminManager = new AdminManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
        if (currentAdmin != null) {
            if (nameField != null) {
                nameField.setPromptText("Mevcut: " + currentAdmin.getFullName());
            }
        }
    }

    // --- CEZA EKLEME İŞLEMİ ---
    @FXML
    public void handleAddPenalty(ActionEvent event) {
        processPenaltyModification(true, addPenaltyBtn, "Ceza Puanı Ekle");
    }

    // --- CEZA SİLME İŞLEMİ ---
    @FXML
    public void handleReducePenalty(ActionEvent event) {
        processPenaltyModification(false, reducePenaltyBtn, "Ceza Puanı Sil");
    }

    // Ceza artırma ve azaltma motoru (100 Puan Kontrolü Eklendi)
    private void processPenaltyModification(boolean isAddition, Button clickedBtn, String originalBtnText) {
        if (isProcessing) return;

        String email = studentEmailField.getText();
        String pointsStr = penaltyPointsField.getText();

        if (email == null || email.trim().isEmpty()) {
            showCustomAlert("Uyarı", "Lütfen işlem yapılacak öğrencinin e-postasını giriniz.");
            return;
        }

        int points = 0;
        try {
            points = Integer.parseInt(pointsStr.trim());
            if (points <= 0) {
                showCustomAlert("Hata", "Ceza puanı mutlaka pozitif bir tam sayı olmalıdır.");
                return;
            }
        } catch (NumberFormatException e) {
            showCustomAlert("Hata", "Lütfen 'Ceza Puanı' alanına geçerli bir sayı giriniz.");
            return;
        }

        isProcessing = true;
        clickedBtn.setText("İşleniyor...");

        final int finalPoints = points;
        new Thread(() -> {
            Database db = Database.getInstance();
            Student targetStudent = new Student("", email.trim(), "");
            DbStatus fillStatus = db.fillStudentDataByEmail(targetStudent, email.trim());
            
            if (fillStatus != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedBtn.setText(originalBtnText);
                    showCustomAlert("Bulunamadı", "Sistemde böyle bir e-postaya sahip öğrenci bulunamadı.");
                });
                return;
            }
            
            int currentPoints = targetStudent.getPenaltyPoints();
            int projectedPoints = isAddition ? (currentPoints + finalPoints) : (currentPoints - finalPoints);

            // 100 PUAN SINIRI KONTROLÜ
            if (isAddition && projectedPoints >= 100) {
                Platform.runLater(() -> {
                    showCustomConfirmation(
                        "Kritik Uyarı", 
                        "Bu ceza eklendiğinde öğrencinin toplam puanı " + projectedPoints + " olacak ve 100 sınırını geçecek.\nÖğrenci SİSTEMDEN DİREKT BANLANACAKTIR.\nEmin misiniz?",
                        () -> executePenaltyAndBan(targetStudent, finalPoints, true, clickedBtn, originalBtnText, true), // EVET'e basarsa
                        () -> { // İPTAL'e basarsa
                            isProcessing = false;
                            clickedBtn.setText(originalBtnText);
                        }
                    );
                });
            } else {
                // 100'ü geçmiyorsa normal işlemine devam et
                executePenaltyAndBan(targetStudent, finalPoints, isAddition, clickedBtn, originalBtnText, false);
            }

        }).start();
    }

    // Gerçek Veritabanı İşlemini Yapan Arka Plan Görevi
    private void executePenaltyAndBan(Student targetStudent, int finalPoints, boolean isAddition, Button clickedBtn, String originalBtnText, boolean autoBan) {
        new Thread(() -> {
            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            DbStatus status;
            
            if (isAddition) {
                status = adminManager.givePenaltyPoint(currentAdmin, targetStudent, finalPoints);
            } else {
                status = adminManager.reducePenaltyPoint(currentAdmin, targetStudent, finalPoints);
            }

            // EĞER 100'Ü GEÇTİĞİ İÇİN OTOMATİK BAN ONAYLANDIYSA VE CEZA EKLENDİYSE BANLA
            if (status == DbStatus.SUCCESS && autoBan) {
                adminManager.banStudent(currentAdmin, targetStudent);
            }

            Platform.runLater(() -> {
                isProcessing = false;
                clickedBtn.setText(originalBtnText);

                if (status == DbStatus.SUCCESS) {
                    studentEmailField.clear();
                    penaltyPointsField.clear();
                    
                    if (autoBan) {
                        showCustomAlert("Banlandı!", targetStudent.getFullName() + " adlı öğrencinin ceza puanı 100'ü geçtiği için SİSTEMDEN UZAKLAŞTIRILDI.");
                    } else {
                        String action = isAddition ? " eklendi." : " silindi.";
                        showCustomAlert("Başarılı", targetStudent.getFullName() + " adlı öğrencinin hanesinden " + finalPoints + " ceza puanı" + action + "\n(Güncel Ceza Puanı: " + targetStudent.getPenaltyPoints() + ")");
                    }
                } else {
                    showCustomAlert("Hata", "İşlem sırasında sunucu kaynaklı bir hata oluştu.");
                }
            });
        }).start();
    }

    // --- BAN KALDIRMA İŞLEMİ ---
    @FXML
    public void handleUnbanStudent(ActionEvent event) {
        if (isProcessing) return;

        String email = studentEmailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showCustomAlert("Uyarı", "Lütfen banı kaldırılacak öğrencinin e-postasını giriniz.");
            return;
        }

        isProcessing = true;
        unbanStudentBtn.setText("İşleniyor...");

        new Thread(() -> {
            Database db = Database.getInstance();
            Student targetStudent = new Student("", email.trim(), "");
            DbStatus fillStatus = db.fillStudentDataByEmail(targetStudent, email.trim());

            if (fillStatus != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    unbanStudentBtn.setText("Ban Kaldır");
                    showCustomAlert("Bulunamadı", "Sistemde böyle bir e-postaya sahip öğrenci bulunamadı.");
                });
                return;
            }

            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            DbStatus status = adminManager.unbanStudent(currentAdmin, targetStudent);

            Platform.runLater(() -> {
                isProcessing = false;
                unbanStudentBtn.setText("Ban Kaldır");

                if (status == DbStatus.SUCCESS) {
                    studentEmailField.clear();
                    penaltyPointsField.clear();
                    showCustomAlert("Ban Kaldırıldı!", targetStudent.getFullName() + " adlı öğrencinin banı başarıyla kaldırıldı ve kendisine bilgilendirme gönderildi.");
                } else {
                    showCustomAlert("Hata", "Öğrencinin banı kaldırılırken bir hata oluştu.");
                }
            });
        }).start();
    }

    // --- DİREKT BANLAMA İŞLEMİ ---
    @FXML
    public void handleBanStudent(ActionEvent event) {
        if (isProcessing) return;

        String email = studentEmailField.getText();

        if (email == null || email.trim().isEmpty()) {
            showCustomAlert("Uyarı", "Lütfen banlanacak öğrencinin e-postasını giriniz.");
            return;
        }

        isProcessing = true;
        banStudentBtn.setText("İşleniyor...");

        new Thread(() -> {
            Database db = Database.getInstance();
            Student targetStudent = new Student("", email.trim(), "");
            DbStatus fillStatus = db.fillStudentDataByEmail(targetStudent, email.trim());

            if (fillStatus != DbStatus.SUCCESS) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    banStudentBtn.setText("Sistemden Banla");
                    showCustomAlert("Bulunamadı", "Sistemde böyle bir e-postaya sahip öğrenci bulunamadı.");
                });
                return;
            }

            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            DbStatus status = adminManager.banStudent(currentAdmin, targetStudent);

            Platform.runLater(() -> {
                isProcessing = false;
                banStudentBtn.setText("Sistemden Banla");

                if (status == DbStatus.SUCCESS) {
                    studentEmailField.clear();
                    penaltyPointsField.clear();
                    showCustomAlert("Banlandı!", targetStudent.getFullName() + " adlı öğrenci sistemden uzaklaştırıldı ve kendisine bilgilendirme gönderildi.");
                } else {
                    showCustomAlert("Hata", "Öğrenci banlanırken bir hata oluştu.");
                }
            });
        }).start();
    }

    // --- KİŞİSEL İSİM VE ŞİFRE DEĞİŞTİRME ---
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
            DbStatus status = adminManager.updateNickname(currentAdmin, newName.trim());

            Platform.runLater(() -> {
                isProcessing = false;
                changeNameBtn.setText("Change");

                if (status == DbStatus.SUCCESS) {
                    nameField.clear();
                    nameField.setPromptText("Mevcut: " + currentAdmin.getFullName());
                    showCustomAlert("Başarılı", "Yönetici isminiz başarıyla güncellendi!");
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

        if (newPassword.trim().length() < 6) {
            showCustomAlert("Zayıf Şifre", "Yeni şifreniz en az 6 karakter olmalıdır.");
            return;
        }

        isProcessing = true;
        changePasswordBtn.setText("...");

        new Thread(() -> {
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

    // YENİ: İKİ BUTONLU ONAY KUTUSU (EVET / İPTAL)
    private void showCustomConfirmation(String title, String message, Runnable onConfirm, Runnable onCancel) {
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
        // Uyarı olduğu için kırmızı renk
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #D93025;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("İptal Et");
        cancelBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            dialogStage.close();
            if (onCancel != null) onCancel.run();
        });

        Button confirmBtn = new Button("Evet, Banla");
        confirmBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            dialogStage.close();
            if (onConfirm != null) onConfirm.run();
        });

        btnBox.getChildren().addAll(cancelBtn, confirmBtn);
        layout.getChildren().addAll(titleLabel, msgLabel, btnBox);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    // Şık Uyarı Kutusu (Tek Butonlu Orijinal)
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