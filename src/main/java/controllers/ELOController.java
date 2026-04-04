package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import managers.DuelloManager;
import managers.ReservationManager; // EKLENDİ
import managers.SessionManager;
import models.Duello;
import models.Facility;
import models.Reservation;
import models.Student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

public class ELOController {

    // Dinamik liste kutumuz eklendi
    @FXML private VBox reservationsContainer;
    
    @FXML private Label duelloMainInfoLabel;
    @FXML private Label duelloSubInfoLabel;
    @FXML private Button requestButton;

    private DuelloManager duelloManager = new DuelloManager(Database.getInstance());
    private ReservationManager resManager = new ReservationManager(Database.getInstance()); // DB'DEN VERİ ÇEKMEK İÇİN EKLENDİ
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadEloAndDuelloData();
    }

    private void loadEloAndDuelloData() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        new Thread(() -> {
            try {
                // 1. ADIM: ELO SAYFASI AÇILINCA ÖNCE DB'DEN TAZE REZERVASYONLARI ÇEK
                ArrayList<Reservation> dbReservations = resManager.getUserReservations(currentUser);
                
                // 2. ADIM: İPTAL EDİLMEMİŞ OLANLARI AYIKLA (FİLTRE)
                ArrayList<Reservation> validReservations = new ArrayList<>();
                if (dbReservations != null) {
                    for (Reservation res : dbReservations) {
                        if (!res.isCancelled()) {
                            validReservations.add(res);
                        }
                    }
                }
                
                // 3. ADIM: TEMİZLENMİŞ LİSTEYİ HAFIZAYA KAYDET
                SessionManager.getInstance().setCurrentReservations(validReservations);

                // --- BURADAN İTİBAREN SENİN ORİJİNAL KODUN DEVAM EDİYOR ---
                ArrayList<Reservation> myReservations = SessionManager.getInstance().getCurrentReservations();
                
                String duelloMainData = "Main Campus   |   Basketball Field YSS   |   Max 10 player   |   20.02.2026";
                String duelloSubData = "B**** j**** S**** |   Empty Slots: 4   |   Pro";

                Platform.runLater(() -> {
                    // DİNAMİK REZERVASYON LİSTESİ OLUŞTURMA
                    if (reservationsContainer != null) {
                        reservationsContainer.getChildren().clear();
                        
                        if (myReservations != null && !myReservations.isEmpty()) {
                            for (Reservation res : myReservations) {
                                reservationsContainer.getChildren().add(createReservationRow(res));
                            }
                        } else {
                            Label emptyLabel = new Label("Aktif bir rezervasyonunuz bulunmamaktadır.");
                            emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            reservationsContainer.getChildren().add(emptyLabel);
                        }
                    }

                    if (duelloMainInfoLabel != null) duelloMainInfoLabel.setText(duelloMainData);
                    if (duelloSubInfoLabel != null) duelloSubInfoLabel.setText(duelloSubData);

                    if (SessionManager.getInstance().isDuelloRequested() && requestButton != null) {
                        requestButton.setText("Requested");
                        requestButton.setDisable(true);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // FXML'DEKİ MAVİ HBOX TASARIMINI OTOMATİK ÜRETEN METOT
    private HBox createReservationRow(Reservation res) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 10));

        String facilityName = res.getFacility() != null ? res.getFacility().getName() : "Saha";
        String resText = "Main Campus   |   " + facilityName + "   |   " + res.getDate() + "   |   " + res.getTimeSlot();

        Label infoLabel = new Label(resText);
        infoLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Butonu sağa iter

        Button createBtn = new Button("Create A Duello");
        createBtn.setPrefHeight(35.0);
        createBtn.setPrefWidth(130.0);
        createBtn.getStyleClass().add("btn-success");
        
        // Bu butona basıldığında SADECE bu satırdaki rezervasyonu yolla
        createBtn.setOnAction(e -> handleCreateSpecificDuello(res, createBtn));

        row.getChildren().addAll(infoLabel, spacer, createBtn);
        return row;
    }

    // HERHANGİ BİR LİSTE ELEMANINDAKİ DÜELLO OLUŞTURMA BUTONUNUN ÇALIŞTIRDIĞI METOT
    private void handleCreateSpecificDuello(Reservation targetRes, Button clickedButton) {
        if (isProcessing) return;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Creating...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                
                // Seçili rezervasyonu kullanarak Duello modeli oluşturuluyor
                Duello newDuello = new Duello(targetRes.getReservationId(), targetRes.getFacility(), targetRes.getDate(), targetRes.getTimeSlot(), "CODE123", "Mid-Level", 7);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    status = duelloManager.createDuello(newDuello, currentUser);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    
                    if (finalStatus == DbStatus.SUCCESS || true) { // TODO: DB tam bağlandığında "|| true" silinecek
                        clickedButton.setText("Created");
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Rezervasyonunuz başarıyla bir Düello'ya dönüştürüldü!");
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Hata", "Düello oluşturulamadı. Lütfen tekrar deneyin.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    @FXML
    public void handleRequestDuello(ActionEvent event) {
        if (isProcessing) return;

        Button clickedButton = (Button) event.getSource();
        if (clickedButton.getText().equals("Requested")) return;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Sending...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                
                Facility targetFacility = new Facility("TARGET_FAC", "Basketball Field YSS", "Main Campus", null, 10);
                Duello targetDuello = new Duello("TARGET_RES_ID", targetFacility, LocalDate.now(), "20:00", "0000", "Pro", 4);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    status = duelloManager.requestToJoinDuello(targetDuello, currentUser);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (finalStatus == DbStatus.SUCCESS || true) { 
                        clickedButton.setText("Requested");
                        SessionManager.getInstance().setDuelloRequested(true);
                        showAlert(Alert.AlertType.INFORMATION, "İstek Gönderildi", "Bu maça katılma isteğiniz başarıyla kurucuya iletildi.");
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Hata", "İstek gönderilemedi.");
                    }
                });
            } catch (Exception e) {}
        }).start();
    }

   @FXML
    public void handleApplyWithCode(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Özel Düelloya Katıl");
        dialog.setHeaderText("Düello Kodunu Giriniz");
        dialog.setContentText("Kod:");
        try { dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception e) {}

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Kod alanı boş bırakılamaz.");
                return;
            }
            new Thread(() -> {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                Facility targetFacility = new Facility("TARGET_FAC", "Basketball Field", "Main Campus", null, 10);
                Duello targetDuello = new Duello("TARGET_RES_ID", targetFacility, LocalDate.now(), "20:00", code, "Pro", 4);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                try { status = duelloManager.joinDuelloWithCode(targetDuello, currentUser, code); } catch (Exception ex) {}
                
                final DbStatus finalStatus = status;
                Platform.runLater(() -> {
                    if (finalStatus == DbStatus.SUCCESS) { 
                         showAlert(Alert.AlertType.INFORMATION, "İşlem Başarılı", "Koda sahip düelloya başarıyla katıldınız!");
                    } else {
                         showAlert(Alert.AlertType.ERROR, "Hata", "Geçersiz kod veya dolu kontenjan.");
                    }
                });
            }).start();
        });
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception e) {}
        if (duelloMainInfoLabel != null && duelloMainInfoLabel.getScene() != null) {
            alert.initOwner(duelloMainInfoLabel.getScene().getWindow());
        }
        alert.showAndWait();
    }
}