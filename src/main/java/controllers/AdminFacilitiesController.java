package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.Facility;
import models.SportType;

public class AdminFacilitiesController {

    @FXML private VBox facilitiesContainer;
    
    // Toplu işlemler için CheckBox'ları takip edeceğimiz harita
    private Map<Facility, CheckBox> facilitySelectionMap = new HashMap<>();

    private Database db = Database.getInstance();
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadFacilities();
    }

    // --- TESİSLERİ VERİTABANINDAN ÇEK VE EKRANA ÇİZ ---
    private void loadFacilities() {
        if (facilitiesContainer == null) return;
        
        facilitiesContainer.getChildren().clear();
        facilitySelectionMap.clear(); // Listeyi her yüklemede temizle
        
        Label loadingLabel = new Label("Tesisler yükleniyor...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
        facilitiesContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            ArrayList<Facility> facilities = db.getFacilities();

            Platform.runLater(() -> {
                facilitiesContainer.getChildren().clear();

                if (facilities.isEmpty()) {
                    Label emptyLabel = new Label("Sistemde henüz kayıtlı bir tesis bulunmuyor.");
                    emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                    facilitiesContainer.getChildren().add(emptyLabel);
                } else {
                    for (Facility f : facilities) {
                        facilitiesContainer.getChildren().add(createFacilityCard(f));
                    }
                }
            });
        }).start();
    }

    // --- HER TESİS İÇİN ŞIK BİR KART ÜRETEN METOT ---
    private HBox createFacilityCard(Facility facility) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        card.setPadding(new Insets(15, 20, 15, 20));

        // Toplu işlem için CheckBox
        CheckBox selectBox = new CheckBox();
        selectBox.setStyle("-fx-cursor: hand;");
        facilitySelectionMap.put(facility, selectBox);

        // --- SOL TARAF: İSİM VE DURUM YAN YANA ---
        HBox nameAndStatusBox = new HBox(10);
        nameAndStatusBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(facility.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2B3674;");
        
        // Durum Etiketi (Artık ismin hemen yanında!)
        Label statusLabel = new Label();
        statusLabel.setPadding(new Insets(3, 8, 3, 8));
        statusLabel.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 11px;");

        // İşlem Butonu (En sağda kalacak)
        Button actionBtn = new Button();
        actionBtn.setPrefHeight(35);
        actionBtn.setPrefWidth(140);
        actionBtn.setStyle("-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");

        if (facility.isUnderMaintenance()) {
            statusLabel.setText("BAKIMDA");
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FCE8E8; -fx-text-fill: #D93025;");
            actionBtn.setText("Kullanıma Aç");
            actionBtn.setStyle(actionBtn.getStyle() + "-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E;");
        } else {
            statusLabel.setText("AKTİF");
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E;");
            actionBtn.setText("Bakıma Al");
            actionBtn.setStyle(actionBtn.getStyle() + "-fx-background-color: #FCE8E8; -fx-text-fill: #D93025;");
        }

        nameAndStatusBox.getChildren().addAll(nameLabel, statusLabel); // İsim ve durumu aynı satıra koyduk

        // --- ALT TARAF: DETAYLAR ---
        VBox textContainer = new VBox(5);
        String sportName = facility.getSportType() != null ? facility.getSportType().name().replace("_", " ") : "UNKNOWN";
        Label detailsLabel = new Label("Kampüs: " + facility.getCampusLocation() + "  |  Spor: " + sportName + "  |  Kapasite: " + facility.getCapacity());
        detailsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #A3AED0;");
        
        textContainer.getChildren().addAll(nameAndStatusBox, detailsLabel); // Önce isim-durum, alta detaylar

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Tekli buton tıklaması
        actionBtn.setOnAction(e -> handleToggleMaintenance(facility, actionBtn));

        // Checkbox, yazılar, boşluk ve tekli buton karta ekleniyor (statusLabel artık textContainer'ın içinde)
        card.getChildren().addAll(selectBox, textContainer, spacer, actionBtn);
        return card;
    }

    // --- TEKLİ TESİSİ BAKIMA SOKMA VE BİLDİRİM GÖNDERME ---
    private void handleToggleMaintenance(Facility facility, Button clickedBtn) {
        if (isProcessing) return;
        isProcessing = true;
        clickedBtn.setText("İşleniyor...");

        new Thread(() -> {
            boolean newStatus = !facility.isUnderMaintenance(); // Tersine çevir
            
            DbStatus status = db.updateFacilityMaintenance(facility.getName(), newStatus);
            
            if (status == DbStatus.SUCCESS) {
                if (newStatus) {
                    db.insertNotification("BROADCAST", 
                        "Tesis Bakımı: " + facility.getName(), 
                        facility.getName() + " isimli tesisimiz bakıma alınmıştır. Geçici bir süre rezervasyon yapılamayacaktır.");
                } else {
                    db.insertNotification("BROADCAST", 
                        "Tesis Kullanıma Açıldı", 
                        facility.getName() + " isimli tesisimizin bakımı tamamlanmış olup tekrar rezervasyona açılmıştır.");
                }
            }

            final DbStatus finalStatus = status;

            Platform.runLater(() -> {
                isProcessing = false;
                if (finalStatus == DbStatus.SUCCESS) {
                    showCustomAlert("Başarılı", "Tesis durumu güncellendi ve kullanıcılara bildirim gönderildi.");
                    loadFacilities(); 
                } else {
                    showCustomAlert("Hata", "Tesis güncellenirken veritabanında bir hata oluştu.");
                    loadFacilities(); 
                }
            });
        }).start();
    }

    // --- TOPLU KAPATMA (Bakıma Alma) ---
    @FXML
    public void handleBulkClose(ActionEvent event) {
        processBulkMaintenance(true, "Tesis Bakımı", " isimli tesisimiz bakıma alınmıştır.");
    }

    // --- TOPLU AÇMA (Kullanıma Açma) ---
    @FXML
    public void handleBulkOpen(ActionEvent event) {
        processBulkMaintenance(false, "Tesis Kullanıma Açıldı", " isimli tesisimiz tekrar rezervasyona açılmıştır.");
    }

    // --- TOPLU İŞLEM MOTORU ---
    private void processBulkMaintenance(boolean shouldMaintain, String notifTitle, String notifMsg) {
        if (isProcessing) return;
        
        java.util.List<Facility> selectedOnes = new java.util.ArrayList<>();
        for (Map.Entry<Facility, CheckBox> entry : facilitySelectionMap.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedOnes.add(entry.getKey());
            }
        }

        if (selectedOnes.isEmpty()) {
            showCustomAlert("Uyarı", "Lütfen işlem yapmak istediğiniz tesisleri seçin.");
            return;
        }

        isProcessing = true;
        new Thread(() -> {
            int count = 0;
            for (Facility f : selectedOnes) {
                if (f.isUnderMaintenance() == shouldMaintain) continue;

                DbStatus status = db.updateFacilityMaintenance(f.getName(), shouldMaintain);
                if (status == DbStatus.SUCCESS) {
                    db.insertNotification("BROADCAST", notifTitle, f.getName() + notifMsg);
                    count++;
                }
            }
            
            final int finalCount = count;
            Platform.runLater(() -> {
                isProcessing = false;
                if (finalCount > 0) {
                    showCustomAlert("İşlem Tamamlandı", finalCount + " adet tesisin durumu güncellendi ve bildirimler gönderildi.");
                } else {
                    showCustomAlert("Bilgi", "Seçilen tesislerin durumu zaten istediğiniz gibi.");
                }
                loadFacilities();
            });
        }).start();
    }

    // --- YENİ TESİS EKLEME MENÜSÜ ---
    @FXML
    public void handleAddFacility(ActionEvent event) {
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

        Label titleLabel = new Label("Yeni Tesis Ekle");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

        TextField nameField = new TextField();
        nameField.setPromptText("Tesis Adı (Örn: Doğu Kampüs Halı Saha)");
        nameField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        ComboBox<String> locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll("Main Campus", "East Campus");
        locationCombo.setPromptText("Kampüs Seçin");
        locationCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        TextField capacityField = new TextField();
        capacityField.setPromptText("Kapasite (Örn: 14)");
        capacityField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        ComboBox<String> sportCombo = new ComboBox<>();
        for (SportType sport : SportType.values()) {
            sportCombo.getItems().add(sport.name().replace("_", " "));
        }
        sportCombo.setPromptText("Spor Türü Seçin");
        sportCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("İptal");
        cancelBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button addBtn = new Button("Kaydet");
        addBtn.setStyle("-fx-background-color: #1E8E3E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        
        addBtn.setOnAction(e -> {
            String fName = nameField.getText();
            String fLoc = locationCombo.getValue();
            String capStr = capacityField.getText();
            String fSport = sportCombo.getValue();

            if (fName == null || fName.trim().isEmpty() || fLoc == null || fSport == null || capStr == null || capStr.trim().isEmpty()) {
                showCustomAlert("Eksik Bilgi", "Lütfen tüm alanları doldurun.");
                return;
            }

            try {
                int capacity = Integer.parseInt(capStr.trim());
                
                new Thread(() -> {
                    DbStatus status = db.insertFacility(fName.trim(), fLoc, capacity, fSport, false);
                    
                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) {
                            showCustomAlert("Başarılı", "Tesis başarıyla eklendi!");
                            dialogStage.close();
                            loadFacilities();
                        } else {
                            showCustomAlert("Hata", "Tesis eklenemedi. Bu isimde bir tesis zaten var olabilir.");
                        }
                    });
                }).start();
                
            } catch (NumberFormatException ex) {
                showCustomAlert("Geçersiz Format", "Kapasite sadece sayılardan oluşmalıdır.");
            }
        });

        btnBox.getChildren().addAll(cancelBtn, addBtn);
        layout.getChildren().addAll(titleLabel, nameField, locationCombo, capacityField, sportCombo, btnBox);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    // --- ÖZEL UYARI MESAJI KUTUSU ---
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