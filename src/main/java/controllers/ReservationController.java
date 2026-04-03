package controllers;

import java.time.LocalDate;
import java.util.ArrayList;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import managers.ReservationManager;
import managers.SessionManager;
import models.Facility;
import models.Reservation;
import models.SportType;
import models.Student;

public class ReservationController {

    // Rezervasyon listesi için dinamik VBox konteyner
    @FXML private VBox reservationsContainer;

    // Rezervasyon yapma butonları
    @FXML private Button basketballBtn;
    @FXML private Button footballBtn;
    @FXML private Button slot1Btn;
    @FXML private Button slot2Btn;

    private ReservationManager resManager = new ReservationManager(Database.getInstance());
    private String selectedSport = "Football"; 
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        setupButtons();
        
        Label loadingLabel = new Label("Loading your reservations...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        if (reservationsContainer != null) {
            reservationsContainer.getChildren().clear();
            reservationsContainer.getChildren().add(loadingLabel);
        }
        
        // Sayfa açılır açılmaz veritabanından GÜNCEL veriyi çek
        fetchFreshReservations();
    }

    private void setupButtons() {
        if (basketballBtn != null && footballBtn != null) {
            basketballBtn.setOnAction(e -> selectSport("Basketball", basketballBtn, footballBtn));
            footballBtn.setOnAction(e -> selectSport("Football", footballBtn, basketballBtn));
            selectSport("Football", footballBtn, basketballBtn);
        }

        if (slot1Btn != null) slot1Btn.setOnAction(e -> attemptReservation("8.45-9.45", slot1Btn));
        if (slot2Btn != null) slot2Btn.setOnAction(e -> attemptReservation("11.45-12.45", slot2Btn));
    }

    private void selectSport(String sport, Button activeBtn, Button passiveBtn) {
        this.selectedSport = sport;
        
        activeBtn.setStyle("-fx-opacity: 1.0;");
        activeBtn.getStyleClass().remove("btn-secondary");
        if (!activeBtn.getStyleClass().contains("btn-success")) activeBtn.getStyleClass().add("btn-success");
        
        passiveBtn.setStyle("-fx-opacity: 0.5;");
        passiveBtn.getStyleClass().remove("btn-success");
        if (!passiveBtn.getStyleClass().contains("btn-secondary")) passiveBtn.getStyleClass().add("btn-secondary");
    }

    // VERİTABANINDAN GÜNCEL LİSTEYİ ÇEKEN METOT
    private void fetchFreshReservations() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Platform.runLater(this::refreshUI);
            return;
        }

        new Thread(() -> {
            try {
                // Eski veriye bakmaksızın DB'den sıfırdan çekiyoruz
                ArrayList<Reservation> dbList = resManager.getUserReservations(currentUser);
                SessionManager.getInstance().setCurrentReservations(dbList);
            } catch (Throwable t) { 
                t.printStackTrace();
            } finally {
                Platform.runLater(this::refreshUI);
            }
        }).start();
    }

    // ARAYÜZÜ YENİDEN ÇİZEN METOT
    private void refreshUI() {
        if (reservationsContainer == null) return;
        reservationsContainer.getChildren().clear();

        ArrayList<Reservation> myList = SessionManager.getInstance().getCurrentReservations();

        if (myList != null && !myList.isEmpty()) {
            for (Reservation r : myList) {
                reservationsContainer.getChildren().add(createReservationRow(r));
            }
        } else {
            Label emptyLabel = new Label("Geçmiş veya aktif bir rezervasyonunuz bulunmamaktadır.");
            emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 14px;");
            reservationsContainer.getChildren().add(emptyLabel);
        }
    }

    private String formatReservationText(Reservation res) {
        if (res == null) return "";
        String facilityName = (res.getFacility() != null) ? res.getFacility().getName() : selectedSport + " Field";
        String status = res.isCancelled() ? " [Cancelled]" : (res.isHasAttended() ? " [Attended]" : " [Active]");
        return res.getDate().toString() + "   |   " + res.getTimeSlot() + "   |   " + facilityName + status;
    }

    private HBox createReservationRow(Reservation res) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 10));

        Label infoLabel = new Label(formatReservationText(res));
        infoLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        // İptal edilmiş rezervasyonların üstünü çizgiyle kapat
        if (res.isCancelled()) {
            infoLabel.setStyle("-fx-text-fill: #e82c2c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-strikethrough: true;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(infoLabel, spacer);

        // Sadece geçmişte kalmamış, iptal edilmemiş ve katılınmamış rezervasyonlarda iptal butonu göster
        if (!res.isCancelled() && !res.isHasAttended() && !res.getDate().isBefore(LocalDate.now())) {
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setPrefHeight(35.0);
            cancelBtn.setPrefWidth(100.0);
            cancelBtn.getStyleClass().add("btn-danger");
            
            cancelBtn.setOnAction(e -> cancelSpecificReservation(res, cancelBtn));
            row.getChildren().add(cancelBtn);
        }

        return row;
    }

    // İPTAL ETME İŞLEMİ VE SONRASINDA OTOMATİK LİSTE YENİLEME
    private void cancelSpecificReservation(Reservation resToCancel, Button clickedBtn) {
        if (isProcessing) return;
        
        isProcessing = true;
        String originalText = clickedBtn.getText();
        clickedBtn.setText("Cancelling...");
        clickedBtn.setDisable(true);

        new Thread(() -> {
            try {
                DbStatus status = resManager.cancelReservation(resToCancel);

                Platform.runLater(() -> {
                    isProcessing = false;
                    
                    if (status == DbStatus.SUCCESS) { 
                        // Veritabanından en güncel hali çek ve listeyi anında yenile
                        fetchFreshReservations(); 
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Reservation has been cancelled successfully.");
                    } else {
                        clickedBtn.setText(originalText);
                        clickedBtn.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Error", "Past or already cancelled reservations cannot be modified.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedBtn.setText(originalText);
                    clickedBtn.setDisable(false);
                });
            }
        }).start();
    }

    // YENİ REZERVASYON YAPMA İŞLEMİ VE SONRASINDA OTOMATİK LİSTE YENİLEME
    private void attemptReservation(String timeSlot, Button clickedButton) {
        if (clickedButton.getStyleClass().contains("btn-danger")) {
            showAlert(Alert.AlertType.ERROR, "Dolu", "Bu saat dilimi (" + timeSlot + ") şu anda dolu.");
            return;
        }

        if (isProcessing) return;
        isProcessing = true;
        
        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                SportType sportEnum = SportType.valueOf(selectedSport.toUpperCase());
                
                Facility targetFacility = new Facility(selectedSport.toUpperCase() + "_FIELD_1", selectedSport + " Field", "Main Campus", sportEnum, 14);
                
                Reservation newRes = resManager.makeReservation(currentUser, targetFacility, LocalDate.now(), timeSlot);

                Platform.runLater(() -> {
                    isProcessing = false;
                    
                    if (newRes != null) { 
                        // Başarılı olduğunda veritabanındaki GÜNCEL listeyi yeniden çek
                        fetchFreshReservations();
                        
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", selectedSport + " için " + timeSlot + " rezervasyonunuz oluşturuldu.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "İşlem Başarısız", "Rezervasyon oluşturulamadı. (Tesis dolu olabilir, ceza puanınız olabilir veya tarih sınırını aşmış olabilirsiniz.)");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> isProcessing = false);
            }
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try { alert.initStyle(StageStyle.UNDECORATED); } catch (Exception ignored) {}
        
        try { 
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); 
        } catch (Exception e) {}
        
        if (reservationsContainer != null && reservationsContainer.getScene() != null) {
            alert.initOwner(reservationsContainer.getScene().getWindow());
        }
        alert.showAndWait();
    }
}