package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import managers.ReservationManager;

import java.time.LocalDate;

public class ReservationController {

    // FXML Bağlantıları
    @FXML private Label activeReservationLabel;
    @FXML private Button cancelButton;

    @FXML private Button basketballBtn;
    @FXML private Button footballBtn;

    @FXML private Button slot1Btn;
    @FXML private Button slot2Btn;

    // Singleton Database ve Manager instance'ları
    private Database db = Database.getInstance();
    private ReservationManager reservationManager = new ReservationManager(db);

    private String selectedSport = "Football"; // Varsayılan seçim
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadReservationData();
        setupButtons();
    }

    private void setupButtons() {
        // Spor seçimi buton olayları
        basketballBtn.setOnAction(e -> selectSport("Basketball", basketballBtn, footballBtn));
        footballBtn.setOnAction(e -> selectSport("Football", footballBtn, basketballBtn));

        // *** DEĞİŞİKLİK BURADA ***
        // Saat butonlarına tıklandığında butonun KENDİSİNİ de metoda gönderiyoruz ki rengini kontrol edebilelim
        slot1Btn.setOnAction(e -> attemptReservation("8.45 - 9.45", slot1Btn));
        slot2Btn.setOnAction(e -> attemptReservation("11.45 - 12.45", slot2Btn));
        
        // Ekran açıldığında varsayılan olarak Football seçili gelsin
        selectSport("Football", footballBtn, basketballBtn);
    }

    private void selectSport(String sport, Button activeBtn, Button passiveBtn) {
        this.selectedSport = sport;
        
        activeBtn.setStyle("-fx-opacity: 1.0;");
        activeBtn.getStyleClass().remove("btn-secondary");
        if (!activeBtn.getStyleClass().contains("btn-success")) {
            activeBtn.getStyleClass().add("btn-success");
        }
        
        passiveBtn.setStyle("-fx-opacity: 0.5;");
        passiveBtn.getStyleClass().remove("btn-success");
        if (!passiveBtn.getStyleClass().contains("btn-secondary")) {
            passiveBtn.getStyleClass().add("btn-secondary");
        }
    }

    private void loadReservationData() {
        new Thread(() -> {
            try {
                String myReservation = "Main Campus   |   Football Field 1   |   " + LocalDate.now().toString() + "   |   18.45-19.45";

                Platform.runLater(() -> {
                    if (activeReservationLabel != null) {
                        activeReservationLabel.setText(myReservation);
                    }
                    if (cancelButton != null) {
                        cancelButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleCancelReservation(ActionEvent event) {
        if (isProcessing) return;
        isProcessing = true;
        
        String originalText = cancelButton.getText();
        cancelButton.setText("İptal ediliyor...");

        new Thread(() -> {
            try {
                Thread.sleep(600); 

                Platform.runLater(() -> {
                    isProcessing = false;
                    cancelButton.setText(originalText);
                    
                    if (activeReservationLabel != null) {
                        activeReservationLabel.setText("Aktif bir rezervasyonunuz bulunmamaktadır.");
                    }
                    if (cancelButton != null) {
                        cancelButton.setDisable(true); 
                    }
                    
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Rezervasyonunuz başarıyla iptal edildi.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    isProcessing = false;
                    cancelButton.setText(originalText);
                });
            }
        }).start();
    }

    private void attemptReservation(String timeSlot, Button clickedButton) {
        // *** YENİ KONTROL ***
        // Eğer tıklanan butonun CSS'inde "btn-danger" (yani Kırmızı) varsa, rezervasyona izin verme!
        if (clickedButton.getStyleClass().contains("btn-danger")) {
            showAlert(Alert.AlertType.ERROR, "Dolu", "Bu saat dilimi (" + timeSlot + ") şu anda dolu. Lütfen uygun (yeşil) saatleri seçiniz.");
            return;
        }

        if (isProcessing) return;
        
        if (cancelButton != null && !cancelButton.isDisabled()) {
            showAlert(Alert.AlertType.WARNING, "Uyarı", "Zaten aktif bir rezervasyonunuz bulunuyor. Yeni bir tane yapmadan önce mevcut olanı iptal etmelisiniz.");
            return;
        }

        isProcessing = true;
        
        new Thread(() -> {
            try {
                Thread.sleep(800); // Veritabanı simülasyonu

                Platform.runLater(() -> {
                    isProcessing = false;
                    
                    String newResText = "Main Campus   |   " + selectedSport + " Field   |   " + LocalDate.now().toString() + "   |   " + timeSlot;
                    activeReservationLabel.setText(newResText);
                    cancelButton.setDisable(false);
                    
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", selectedSport + " için " + timeSlot + " saatine rezervasyonunuz oluşturuldu.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> isProcessing = false);
            }
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS dosyası bulunamadı.");
        }

        if (activeReservationLabel != null && activeReservationLabel.getScene() != null) {
            alert.initOwner(activeReservationLabel.getScene().getWindow());
        }
        
        alert.showAndWait();
    }
}