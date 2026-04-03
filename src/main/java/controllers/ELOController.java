package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import managers.SessionManager;

import java.util.Optional;

public class ELOController {

    @FXML private Label activeReservationLabel;
    @FXML private Label duelloMainInfoLabel;
    @FXML private Label duelloSubInfoLabel;
    @FXML private Button requestButton;
    private Database db = Database.getInstance();
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadEloAndDuelloData();
    }

    private void loadEloAndDuelloData() {
        new Thread(() -> {
            try {
                // KRİTİK: Veriyi elle değil, ortak SessionManager'dan (Rezervasyon sayfasından) çekiyoruz!
                String reservationData = SessionManager.getInstance().getCurrentReservation();
                
                String duelloMainData = "Main Campus   |   Basketball Field YSS   |   Max 10 player   |   20.02.2026";
                String duelloSubData = "B**** j**** S**** |   Empty Slot: 4   |   Pro";

                Platform.runLater(() -> {
        if (activeReservationLabel != null) {
            if (reservationData != null) {
                activeReservationLabel.setText(reservationData);
            } else {
                activeReservationLabel.setText("Aktif bir rezervasyonunuz bulunmamaktadır.");
            }
        }
        if (duelloMainInfoLabel != null) duelloMainInfoLabel.setText(duelloMainData);
        if (duelloSubInfoLabel != null) duelloSubInfoLabel.setText(duelloSubData);

        // HAFIZA KONTROLÜ: Daha önce istek atılmışsa butonu kapalı ve "Requested" olarak getir!
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
        
        // KORUMA: Aktif rezervasyonu olmayan biri düello oluşturamaz!
        if (SessionManager.getInstance().getCurrentReservation() == null) {
            showAlert(Alert.AlertType.WARNING, "İşlem Reddedildi", "Düello oluşturabilmek için öncelikle bir saha rezervasyonu yapmalısınız.");
            return;
        }

        Button clickedButton = (Button) event.getSource();
        
        if (clickedButton.getText().equals("Created")) {
            return;
        }

        isProcessing = true;
        String originalText = clickedButton.getText();
        
        clickedButton.setDisable(true);
        clickedButton.setText("Creating...");

        new Thread(() -> {
            try {
                Thread.sleep(800); 

                Platform.runLater(() -> {
                    isProcessing = false;
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

        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {}

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Kod alanı boş bırakılamaz.");
                return;
            }
            showAlert(Alert.AlertType.INFORMATION, "İşlem Başarılı", "Kod kontrol ediliyor: " + code);
        });
    }

    @FXML
    public void handleRequestDuello(ActionEvent event) {
        if (isProcessing) return;

        Button clickedButton = (Button) event.getSource();
        
        if (clickedButton.getText().equals("Requested")) {
            return;
        }

        isProcessing = true;
        String originalText = clickedButton.getText();
        
        clickedButton.setDisable(true);
        clickedButton.setText("Sending...");

        new Thread(() -> {
            try {
                Thread.sleep(800);

              Platform.runLater(() -> {
        isProcessing = false;
        clickedButton.setText("Requested");
        
        // BAŞARILI OLUNCA HAFIZAYA KAYDET Kİ BİR DAHA UNUTMASIN
        SessionManager.getInstance().setDuelloRequested(true);
        
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

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm());
        } catch (Exception e) {}

        if (activeReservationLabel != null && activeReservationLabel.getScene() != null) {
            alert.initOwner(activeReservationLabel.getScene().getWindow());
        }
        alert.showAndWait();
    }
}