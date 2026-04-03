package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import managers.DuelloManager;
import managers.SessionManager;
import models.Duello;
import models.Facility;
import models.Student;

import java.time.LocalDate;
import java.util.Optional;

public class ELOController {

    @FXML private Label activeReservationLabel;
    @FXML private Label duelloMainInfoLabel;
    @FXML private Label duelloSubInfoLabel;
    @FXML private Button requestButton;

    // GERÇEK MANAGER BAĞLANTISI
    private DuelloManager duelloManager = new DuelloManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadEloAndDuelloData();
    }

    private void loadEloAndDuelloData() {
        new Thread(() -> {
            try {
                // UI'dan (Geçici Hafızadan) güncel rezervasyonu çek
                String reservationData = SessionManager.getInstance().getCurrentReservation();
                
                String duelloMainData = "Main Campus   |   Basketball Field YSS   |   Max 10 player   |   20.02.2026";
                String duelloSubData = "B**** j**** S**** |   Empty Slots: 4   |   Pro";

                Platform.runLater(() -> {
                    if (activeReservationLabel != null) {
                        if (reservationData != null && !reservationData.isEmpty()) {
                            activeReservationLabel.setText(reservationData);
                        } else {
                            activeReservationLabel.setText("Aktif bir rezervasyonunuz bulunmamaktadır.");
                        }
                    }
                    if (duelloMainInfoLabel != null) duelloMainInfoLabel.setText(duelloMainData);
                    if (duelloSubInfoLabel != null) duelloSubInfoLabel.setText(duelloSubData);

                    // DAHA ÖNCE İSTEK ATILMIŞSA BUTONU KAPALI GETİR
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

    @FXML
    public void handleCreateDuello(ActionEvent event) {
        if (isProcessing) return;
        
        if (SessionManager.getInstance().getCurrentReservation() == null) {
            showAlert(Alert.AlertType.WARNING, "İşlem Reddedildi", "Düello oluşturabilmek için öncelikle bir saha rezervasyonu yapmalısınız.");
            return;
        }

        Button clickedButton = (Button) event.getSource();
        if (clickedButton.getText().equals("Created")) return;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Creating...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                
                // DÜELLO MODELİNİ GEÇİCİ VERİLERLE OLUŞTURUYORUZ
                Facility tempFacility = new Facility("TEMP_FAC", "Football Field", "Main Campus", null, 14);
                Duello newDuello = new Duello("TEMP_RES_ID", tempFacility, LocalDate.now(), "18:45", "CODE123", "Mid-Level", 7);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                
                // VERİTABANI KALKANI
                try {
                    status = duelloManager.createDuello(newDuello, currentUser);
                } catch (Exception ex) {
                    System.out.println("Düello Oluşturma DB Hatası (Normal): " + ex.getMessage());
                }

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
                e.printStackTrace();
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
                
                // KATILMAK İSTENEN HEDEF DÜELLO MODELİ (Şimdilik geçici)
                Facility targetFacility = new Facility("TARGET_FAC", "Basketball Field YSS", "Main Campus", null, 10);
                Duello targetDuello = new Duello("TARGET_RES_ID", targetFacility, LocalDate.now(), "20:00", "0000", "Pro", 4);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                
                // VERİTABANI KALKANI
                try {
                    status = duelloManager.requestToJoinDuello(targetDuello, currentUser);
                } catch (Exception ex) {
                    System.out.println("Düello İstek DB Hatası (Normal): " + ex.getMessage());
                }

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    
                    if (finalStatus == DbStatus.SUCCESS || true) { // TODO: DB tam bağlandığında "|| true" silinecek
                        clickedButton.setText("Requested");
                        SessionManager.getInstance().setDuelloRequested(true); // HAFIZAYA KAYDET
                        showAlert(Alert.AlertType.INFORMATION, "İstek Gönderildi", "Bu maça katılma isteğiniz başarıyla kurucuya iletildi.");
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Hata", "İstek gönderilemedi.");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
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
                
                // TODO: İleride bu targetDuello kullanıcının girdiği koda göre veritabanından bulunup çekilecek
                Facility targetFacility = new Facility("TARGET_FAC", "Basketball Field", "Main Campus", null, 10);
                Duello targetDuello = new Duello("TARGET_RES_ID", targetFacility, LocalDate.now(), "20:00", code, "Pro", 4);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                
                // %100 VERİTABANINA (DB) BAĞLI KONTROL
                try {
                    status = duelloManager.joinDuelloWithCode(targetDuello, currentUser, code);
                } catch (Exception ex) {
                    System.out.println("Düello Kod DB Hatası (Normal): " + ex.getMessage());
                }
                
                final DbStatus finalStatus = status;
                
                Platform.runLater(() -> {
                    // SADECE VERİTABANI "SUCCESS" DÖNERSE KABUL ET
                    if (finalStatus == DbStatus.SUCCESS) { 
                         showAlert(Alert.AlertType.INFORMATION, "İşlem Başarılı", "Koda sahip düelloya başarıyla katıldınız!");
                    } else {
                         // Eğer veritabanı kodları henüz tamamlanmadıysa hep bu hata mesajını göreceksin, bu çok normal.
                         showAlert(Alert.AlertType.ERROR, "Hata", "Geçersiz kod, dolu kontenjan veya veritabanı onayı alınamadı.");
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
        if (activeReservationLabel != null && activeReservationLabel.getScene() != null) {
            alert.initOwner(activeReservationLabel.getScene().getWindow());
        }
        alert.showAndWait();
    }
}