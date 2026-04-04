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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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
    
    // YENİ EKLENEN NİCKNAME ELEMANLARI
    @FXML private TextField nicknameField;
    @FXML private Button changeNicknameBtn;
    
    @FXML private FlowPane interestsContainer; 

    private StudentManager studentManager = new StudentManager(Database.getInstance());
    private boolean isProcessing = false;

    private final String COLOR_ON = "-fx-background-color: #1E8E3E; -fx-background-radius: 20;";
    private final String COLOR_OFF = "-fx-background-color: #E2E8F0; -fx-background-radius: 20;";

    @FXML
    public void initialize() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateToggleVisual(publicAccountToggle, currentUser.isPublicProfile());
            updateToggleVisual(eloToggle, currentUser.isEloMatchingEnabled());
            
            if (nicknameField != null) {
                nicknameField.clear(); 
                // getNickname yerine getFullName kullanıyoruz
                nicknameField.setPromptText("Mevcut: " + currentUser.getFullName()); 
            }
            
            loadUserInterests(currentUser);
        }
    }

    private void loadUserInterests(Student currentUser) {
        new Thread(() -> {
            List<SportType> dbInterests = studentManager.getUserInterests(currentUser);
            
            Platform.runLater(() -> {
                if (interestsContainer != null) {
                    interestsContainer.getChildren().clear(); 
                    
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
        
        String defaultStyle = "-fx-background-color: #F4F7FE; -fx-text-fill: #4318FF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #D93025; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: #D93025; -fx-border-radius: 10; -fx-cursor: hand;";

        btn.setStyle(defaultStyle);
        btn.setPrefHeight(40.0);

        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
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

        // 6 KARAKTER KURALI EKLENDİ!
        if (newPass.length() < 6) {
            showCustomAlert("Zayıf Şifre", "Yeni şifreniz en az 6 karakter olmalıdır.");
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
                    showCustomAlert("Hata", "Şifre güncellenemedi. Yeni şifre eskisinden farklı olmalıdır.");
                }
            });
        }).start();
    }

    // --- YENİ EKLENEN: NICKNAME DEĞİŞTİRME FONKSİYONU ---
 @FXML
    public void handleChangeNickname(ActionEvent event) {
        if (isProcessing) return;
        
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        String newNick = nicknameField.getText();

        if (newNick == null || newNick.trim().isEmpty()) {
            showCustomAlert("Uyarı", "İsim alanı boş bırakılamaz.");
            return;
        }

        isProcessing = true;
        changeNicknameBtn.setText("...");

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                // Veritabanına yolluyoruz
                status = studentManager.updateNickname(currentUser, newNick);
            } catch (Exception e) {}

            final DbStatus finalStatus = status;

            Platform.runLater(() -> {
                isProcessing = false;
                changeNicknameBtn.setText("Change");
                
                if (finalStatus == DbStatus.SUCCESS) {
                    // Başarılıysa kutuyu temizle ve silik ipucunu yeni isminle (getFullName) güncelle!
                    nicknameField.clear(); 
                    nicknameField.setPromptText("Mevcut: " + currentUser.getFullName());
                    
                    showCustomAlert("Başarılı", "İsminiz başarıyla değiştirildi!");
                } else {
                    showCustomAlert("Hata", "İsim güncellenirken sunucu kaynaklı bir sorun oluştu.");
                }
            });
        }).start();
    }

    // --- IŞIK HIZINDA SİLME (OPTIMISTIC UI) ---
    private void handleRemoveInterest(SportType sportType, Button clickedBtn) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        
        if (interestsContainer != null) {
            interestsContainer.getChildren().remove(clickedBtn);
        }

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = studentManager.removeInterest(currentUser, sportType);
            } catch (Exception e) {}
            
            final DbStatus finalStatus = status;
            
            Platform.runLater(() -> {
                if (finalStatus != DbStatus.SUCCESS) {
                    if (interestsContainer != null) {
                        interestsContainer.getChildren().add(clickedBtn);
                    }
                    showCustomAlert("Hata", "Veritabanından silinemedi.");
                }
            });
        }).start();
    }

    @FXML
    public void handleAddInterest(ActionEvent event) {
        showAddInterestDialog();
    }

    private void showAddInterestDialog() {
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

        Label titleLabel = new Label("Yeni İlgi Alanı Ekle");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

        ComboBox<String> sportCombo = new ComboBox<>();
        sportCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-width: 200; -fx-pref-height: 40;");
        for (SportType sport : SportType.values()) {
            sportCombo.getItems().add(sport.name().replace("_", " "));
        }
        if (!sportCombo.getItems().isEmpty()) {
            sportCombo.getSelectionModel().selectFirst();
        }

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("İptal");
        cancelBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 90; -fx-pref-height: 35; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button addBtn = new Button("Ekle");
        addBtn.setStyle("-fx-background-color: #1E8E3E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 90; -fx-pref-height: 35; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            String selectedSportString = sportCombo.getValue();
            if (selectedSportString != null) {
                SportType selectedType = SportType.valueOf(selectedSportString.replace(" ", "_"));
                processAddInterest(selectedType); 
                dialogStage.close();
            }
        });

        btnBox.getChildren().addAll(cancelBtn, addBtn);
        layout.getChildren().addAll(titleLabel, sportCombo, btnBox);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    // --- IŞIK HIZINDA EKLEME (OPTIMISTIC UI) ---
    private void processAddInterest(SportType sportType) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        if (interestsContainer != null) {
            interestsContainer.getChildren().removeIf(node -> node instanceof Label);
            
            boolean alreadyExists = interestsContainer.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> ((Button) node).getText())
                .anyMatch(text -> text.contains(sportType.name().replace("_", " ")));
            
            if (alreadyExists) {
                showCustomAlert("Uyarı", "Bu ilgi alanı zaten ekli.");
                return;
            }
            
            Button newBtn = createInterestButton(sportType);
            interestsContainer.getChildren().add(newBtn);

            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    status = studentManager.addInterest(currentUser, sportType);
                } catch (Exception e) {}
                
                final DbStatus finalStatus = status;
                
                Platform.runLater(() -> {
                    if (finalStatus != DbStatus.SUCCESS) {
                        interestsContainer.getChildren().remove(newBtn);
                        showCustomAlert("Hata", "Bu ilgi alanı zaten ekli olabilir veya sunucu bağlantısı kurulamadı.");
                    }
                });
            }).start();
        }
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