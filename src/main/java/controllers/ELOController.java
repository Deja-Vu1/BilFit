package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class ELOController {

    // FXML Labels
    @FXML private Label activeReservationLabel;
    @FXML private Label duelloMainInfoLabel;
    @FXML private Label duelloSubInfoLabel;

    // Singleton Database connection
    private Database db = Database.getInstance();

    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        // Ekran yüklendiğinde verileri çek
        loadEloAndDuelloData();
    }

    private void loadEloAndDuelloData() {
        // UI donmasın diye arka planda çalıştırıyoruz
        new Thread(() -> {
            try {
                /*
                 * TODO: İleride Database sorguları buraya gelecek.
                 * Örnek: String resData = db.getActiveReservation(userId);
                 */
                
                String reservationData = "Main Campus   |   Football Field 1   |   20.02.2026   |   18.45-19.45";
                String duelloMainData = "Main Campus   |   Basketball Field YSS   |   Max 10 player   |   20.02.2026";
                String duelloSubData = "B**** j**** S**** |   Empty Slot: 4   |   Pro";

                Platform.runLater(() -> {
                    if (activeReservationLabel != null) activeReservationLabel.setText(reservationData);
                    if (duelloMainInfoLabel != null) duelloMainInfoLabel.setText(duelloMainData);
                    if (duelloSubInfoLabel != null) duelloSubInfoLabel.setText(duelloSubData);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleCreateDuello(ActionEvent event) {
        if (isProcessing) return;
        
        Button clickedButton = (Button) event.getSource();
        
        // Zaten oluşturulmuşsa bir daha tıklanmasını engelle
        if (clickedButton.getText().equals("Created")) {
            return;
        }

        isProcessing = true;
        String originalText = clickedButton.getText();
        
        clickedButton.setDisable(true);
        clickedButton.setText("Creating...");

        new Thread(() -> {
            try {
                // TODO: db.createDuello(reservationId);
                Thread.sleep(800); // Veritabanı gecikme simülasyonu

                Platform.runLater(() -> {
                    isProcessing = false;
                    // İşlem bittikten sonra butonu yeşil tutup yazısını Created yapıyoruz ve kapalı kalıyor
                    clickedButton.setText("Created");
                    
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Rezervasyonunuz başarıyla bir Düello'ya dönüştürüldü!");
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

        // Uygulama stiline uygun olması için CSS ekliyoruz
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Kod alanı boş bırakılamaz.");
                return;
            }
            
            /*
             * TODO: Veritabanı entegrasyonu
             * DbStatus status = db.joinDuelloWithCode(userId, code);
             */
            showAlert(Alert.AlertType.INFORMATION, "İşlem Başarılı", "Kod kontrol ediliyor: " + code + "\n(Veritabanı bağlantısı bekleniyor...)");
        });
    }

    @FXML
    public void handleRequestDuello(ActionEvent event) {
        if (isProcessing) return;

        Button clickedButton = (Button) event.getSource();
        
        // Zaten istek atılmışsa engelle
        if (clickedButton.getText().equals("Requested")) {
            return;
        }

        isProcessing = true;
        String originalText = clickedButton.getText();
        
        clickedButton.setDisable(true);
        clickedButton.setText("Sending...");

        new Thread(() -> {
            try {
                // TODO: db.sendDuelloRequest(userId, duelloId);
                Thread.sleep(800); 

                Platform.runLater(() -> {
                    isProcessing = false;
                    // İşlem bittiğinde yazıyı değiştirip butonu kapalı bırakıyoruz
                    clickedButton.setText("Requested");
                    
                    showAlert(Alert.AlertType.INFORMATION, "İstek Gönderildi", "Bu maça katılma isteğiniz başarıyla kurucuya iletildi.");
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

    // Ortak Pop-up metodu
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS dosyası yüklenemedi.");
        }

        // Full-screen pop-up arka plana düşme koruması
        if (activeReservationLabel != null && activeReservationLabel.getScene() != null) {
            alert.initOwner(activeReservationLabel.getScene().getWindow());
        }
        
        alert.showAndWait();
    }
}