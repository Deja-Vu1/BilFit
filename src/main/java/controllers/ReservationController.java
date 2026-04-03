package controllers;

import javafx.application.Platform;
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
import javafx.fxml.FXML;

import managers.ReservationManager;
import managers.SessionManager;
import database.Database;
import database.DbStatus;
import models.Facility;
import models.Reservation;
import models.SportType;
import models.Student;

import java.time.LocalDate;
import java.util.ArrayList;

public class ReservationController {

    // Artık tek bir label/buton yok, dinamik liste kutumuz var
    @FXML private VBox reservationsContainer;

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
        // Geçici bir "Loading" mesajı ekleyelim
        Label loadingLabel = new Label("Loading...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 14px;");
        if(reservationsContainer != null) reservationsContainer.getChildren().add(loadingLabel);
        
        loadReservationDataFromManager();
    }

    private void setupButtons() {
        basketballBtn.setOnAction(e -> selectSport("Basketball", basketballBtn, footballBtn));
        footballBtn.setOnAction(e -> selectSport("Football", footballBtn, basketballBtn));

        slot1Btn.setOnAction(e -> attemptReservation("8.45-9.45", slot1Btn));
        slot2Btn.setOnAction(e -> attemptReservation("11.45-12.45", slot2Btn));
        
        selectSport("Football", footballBtn, basketballBtn);
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

    private String formatReservationText(Reservation res) {
        if (res == null) return "";
        String facilityName = (res.getFacility() != null) ? res.getFacility().getName() : selectedSport + " Field";
        return "Main Campus   |   " + facilityName + "   |   " + res.getDate().toString() + "   |   " + res.getTimeSlot();
    }

    private void loadReservationDataFromManager() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Platform.runLater(this::refreshUI);
            return;
        }

        new Thread(() -> {
            try {
                ArrayList<Reservation> dbList = resManager.getUserReservations(currentUser);
                SessionManager.getInstance().setCurrentReservations(dbList);
            } catch (Throwable t) { 
                System.err.println("DB Rezervasyon Çekme Hatası: " + t.getMessage());
            } finally {
                Platform.runLater(this::refreshUI);
            }
        }).start();
    }

    // LİSTEYİ DİNAMİK OLARAK ÇİZEN METOT
    private void refreshUI() {
        if (reservationsContainer == null) return;
        reservationsContainer.getChildren().clear(); // Kutuyu temizle

        ArrayList<Reservation> myList = SessionManager.getInstance().getCurrentReservations();

        if (myList != null && !myList.isEmpty()) {
            for (Reservation r : myList) {
                // Her bir rezervasyon için yeni bir kart oluştur ve ekle
                reservationsContainer.getChildren().add(createReservationRow(r));
            }
        } else {
            Label emptyLabel = new Label("Aktif bir rezervasyonunuz bulunmamaktadır.");
            emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 14px;");
            reservationsContainer.getChildren().add(emptyLabel);
        }
    }

    // FXML'DEKİ ŞIK HBOX TASARIMINI JAVA KODUYLA ÜRETEN SİHİRLİ METOT
    private HBox createReservationRow(Reservation res) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 10));

        Label infoLabel = new Label(formatReservationText(res));
        infoLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Butonu sağa yaslamak için

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setPrefHeight(35.0);
        cancelBtn.setPrefWidth(100.0);
        cancelBtn.getStyleClass().add("btn-danger");
        
        // Butona tıklandığında sadece o rezervasyonu silecek metodu bağla
        cancelBtn.setOnAction(e -> cancelSpecificReservation(res, cancelBtn));

        row.getChildren().addAll(infoLabel, spacer, cancelBtn);
        return row;
    }

    // SPESİFİK BİR REZERVASYONU İPTAL EDEN METOT
    private void cancelSpecificReservation(Reservation resToCancel, Button clickedBtn) {
        if (isProcessing) return;
        
        isProcessing = true;
        String originalText = clickedBtn.getText();
        clickedBtn.setText("İptal...");
        clickedBtn.setDisable(true);

        new Thread(() -> {
            try {
                DbStatus status = resManager.cancelReservation(resToCancel);

                Platform.runLater(() -> {
                    isProcessing = false;
                    
                    if (status == DbStatus.SUCCESS) { 
                        SessionManager.getInstance().getCurrentReservations().remove(resToCancel);
                        refreshUI(); // Ekrandan silinmesi için UI'ı yenile
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Rezervasyonunuz başarıyla iptal edildi.");
                    } else {
                        clickedBtn.setText(originalText);
                        clickedBtn.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Hata", "Rezervasyon iptal edilemedi. (DB Reddi)");
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
                        SessionManager.getInstance().addReservation(newRes);
                        refreshUI(); // Yeni kartın ekranda hemen belirmesi için UI'ı yenile
                        
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", selectedSport + " için " + timeSlot + " rezervasyonunuz oluşturuldu.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "İşlem Başarısız", "Rezervasyon oluşturulamadı. (Tesis dolu olabilir veya uygun değilsiniz.)");
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