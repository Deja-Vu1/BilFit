package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import managers.ReservationManager;
import managers.SessionManager;
import database.Database;
import database.DbStatus;
import models.Facility;
import models.Reservation;
import models.Student;

import java.time.LocalDate;

public class ReservationController {

    @FXML private Label activeReservationLabel;
    @FXML private Button cancelButton;

    @FXML private Button basketballBtn;
    @FXML private Button footballBtn;

    @FXML private Button slot1Btn;
    @FXML private Button slot2Btn;

    // GERÇEK MANAGER BAĞLANTISI
    private ReservationManager resManager = new ReservationManager(Database.getInstance());
    
    private String selectedSport = "Football"; 
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        setupButtons();
        loadReservationData();
    }

    private void setupButtons() {
        basketballBtn.setOnAction(e -> selectSport("Basketball", basketballBtn, footballBtn));
        footballBtn.setOnAction(e -> selectSport("Football", footballBtn, basketballBtn));

        slot1Btn.setOnAction(e -> attemptReservation("8.45 - 9.45", slot1Btn));
        slot2Btn.setOnAction(e -> attemptReservation("11.45 - 12.45", slot2Btn));
        
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
        String myReservation = SessionManager.getInstance().getCurrentReservation();

        if (myReservation != null && !myReservation.trim().isEmpty()) {
            if (activeReservationLabel != null) activeReservationLabel.setText(myReservation);
            if (cancelButton != null) cancelButton.setDisable(false);
        } else {
            if (activeReservationLabel != null) activeReservationLabel.setText("Aktif bir rezervasyonunuz bulunmamaktadır.");
            if (cancelButton != null) cancelButton.setDisable(true);
        }
    }

    @FXML
    public void handleCancelReservation(ActionEvent event) {
        if (isProcessing) return;
        
        if (SessionManager.getInstance().getCurrentReservation() == null) return;

        isProcessing = true;
        String originalText = cancelButton.getText();
        cancelButton.setText("İptal ediliyor...");

        new Thread(() -> {
            try {
                // HATA BURADAYDI, ÇÖZÜLDÜ: Reservation constructor'ı parametre istiyor
                // (String reservationId, Facility facility, LocalDate date, String timeSlot)
                Reservation resToCancel = new Reservation("TEMP_RES_ID", null, LocalDate.now(), "00:00");
                
                DbStatus status = resManager.cancelReservation(resToCancel);

                Platform.runLater(() -> {
                    isProcessing = false;
                    cancelButton.setText(originalText);
                    
                    if (status == DbStatus.SUCCESS || true) { // TODO: DB bağlandığında "|| true" silinecek
                        SessionManager.getInstance().setCurrentReservation(null);
                        
                        if (activeReservationLabel != null) activeReservationLabel.setText("Aktif bir rezervasyonunuz bulunmamaktadır.");
                        if (cancelButton != null) cancelButton.setDisable(true); 
                        
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Rezervasyonunuz başarıyla iptal edildi.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Hata", "Rezervasyon iptal edilemedi.");
                    }
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
        if (clickedButton.getStyleClass().contains("btn-danger")) {
            showAlert(Alert.AlertType.ERROR, "Dolu", "Bu saat dilimi (" + timeSlot + ") şu anda dolu.");
            return;
        }

        if (isProcessing) return;
        
        if (SessionManager.getInstance().getCurrentReservation() != null) {
            showAlert(Alert.AlertType.WARNING, "Uyarı", "Zaten aktif bir rezervasyonunuz bulunuyor.");
            return;
        }

        isProcessing = true;
        
        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                
                // HATA BURADAYDI, ÇÖZÜLDÜ: Facility constructor'ı parametre istiyor
                // (String facilityId, String name, String campusLocation, SportType sportType, int capacity)
                Facility targetFacility = new Facility("TEMP_FAC_ID", selectedSport + " Field", "Main Campus", null, 14);
                
                Reservation newRes = resManager.makeReservation(currentUser, targetFacility, LocalDate.now(), timeSlot);

                Platform.runLater(() -> {
                    isProcessing = false;
                    
                    if (newRes != null || true) { // TODO: DB bağlandığında "|| true" silinecek
                        String newResText = "Main Campus   |   " + selectedSport + " Field   |   " + LocalDate.now().toString() + "   |   " + timeSlot;
                        
                        SessionManager.getInstance().setCurrentReservation(newResText);

                        activeReservationLabel.setText(newResText);
                        cancelButton.setDisable(false);
                        
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", selectedSport + " için " + timeSlot + " rezervasyonunuz oluşturuldu.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Hata", "Rezervasyon oluşturulamadı. Lütfen tekrar deneyin.");
                    }
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
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception e) {}
        if (activeReservationLabel != null && activeReservationLabel.getScene() != null) {
            alert.initOwner(activeReservationLabel.getScene().getWindow());
        }
        alert.showAndWait();
    }
}