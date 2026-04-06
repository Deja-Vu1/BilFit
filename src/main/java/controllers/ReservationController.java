package controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import managers.ReservationManager;
import managers.SessionManager;
import models.Facility;
import models.Reservation;
import models.Student;

public class ReservationController {

    @FXML private VBox reservationsContainer;
    
    @FXML private ComboBox<String> facilityComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Button refreshBtn;
    @FXML private GridPane timeSlotGrid;

    private ReservationManager resManager = new ReservationManager(Database.getInstance());
    private boolean isProcessing = false;
    private List<Facility> allFacilities = new ArrayList<>();
    private long currentGridUpdateId = 0;

    @FXML
    public void initialize() {
        if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }

        Label loadingLabel = new Label("Loading your reservations...");
        loadingLabel.setStyle("-fx-text-fill: #A3AED0; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        if (reservationsContainer != null) {
            reservationsContainer.getChildren().clear();
            reservationsContainer.getChildren().add(loadingLabel);
        }
        
        if (facilityComboBox != null) {
            facilityComboBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshTimeSlots());
        }
        if (datePicker != null) {
            datePicker.valueProperty().addListener((obs, oldVal, newVal) -> refreshTimeSlots());
        }
        if (refreshBtn != null) {
            refreshBtn.setOnAction(e -> refreshTimeSlots());
        }
        
        loadFacilities();
        fetchFreshReservations();
    }

    private void loadFacilities() {
        new Thread(() -> {
            allFacilities = Database.getInstance().getFacilities();
            
            Platform.runLater(() -> {
                if (facilityComboBox != null) {
                    facilityComboBox.getItems().clear();
                    for (Facility f : allFacilities) {
                        String statusLabel = f.isUnderMaintenance() ? " (Under Maintenance)" : " (Active)";
                        facilityComboBox.getItems().add(f.getName() + statusLabel);
                    }
                    if (!allFacilities.isEmpty()) {
                        facilityComboBox.getSelectionModel().selectFirst();
                    }
                }
            });
        }).start();
    }

    private void refreshTimeSlots() {
        String selectedVal = facilityComboBox.getValue();
        LocalDate selectedDate = datePicker.getValue();

        if (selectedVal == null || selectedDate == null || timeSlotGrid == null) {
            return;
        }

        String selectedFacilityName = selectedVal.replace(" (Under Maintenance)", "").replace(" (Active)", "").replace(" (Bakımda)", "").replace(" (Aktif)", "").trim();

        final long updateId = ++currentGridUpdateId;

        timeSlotGrid.getChildren().clear();
        timeSlotGrid.setHgap(10);
        timeSlotGrid.setVgap(10);

        int initHour = 8;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                Button loadingBtn = new Button("...");
                loadingBtn.setPrefHeight(40.0);
                loadingBtn.setPrefWidth(110.0);
                loadingBtn.getStyleClass().add("btn-secondary");
                loadingBtn.setDisable(true);
                timeSlotGrid.add(loadingBtn, col, row);
                initHour++;
            }
        }

        new Thread(() -> {
            Database db = Database.getInstance();
            Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
            boolean[] availabilities = new boolean[15];
            
            int checkHour = 8;
            for (int i = 0; i < 15; i++) {
                String ts = String.format("%02d.00-%02d.00", checkHour, checkHour + 1);
                availabilities[i] = db.checkFacilityAvailability(selectedFacilityName, selectedDate, ts, currentUser);
                checkHour++;
            }

            Platform.runLater(() -> {
                if (updateId != currentGridUpdateId) return;

                timeSlotGrid.getChildren().clear();
                int currentHour = 8;
                
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 5; col++) {
                        String timeSlot = String.format("%02d.00-%02d.00", currentHour, currentHour + 1);
                        boolean isAvailableFromDb = availabilities[(row * 5) + col];
                        
                        boolean isPast = false;
                        if (selectedDate.isBefore(LocalDate.now())) {
                            isPast = true;
                        } else if (selectedDate.isEqual(LocalDate.now())) {
                            if (LocalTime.now().getHour() >= currentHour) {
                                isPast = true;
                            }
                        }

                        Button slotBtn = new Button(timeSlot);
                        slotBtn.setPrefHeight(40.0);
                        slotBtn.setPrefWidth(110.0);

                        if (isPast) {
                            slotBtn.getStyleClass().add("btn-danger");
                            slotBtn.setDisable(true);
                            slotBtn.setTooltip(new Tooltip("This session has expired."));
                        } else if (isAvailableFromDb) {
                            slotBtn.getStyleClass().add("btn-success"); 
                            slotBtn.setOnAction(e -> attemptReservation(timeSlot, slotBtn));
                        } else {
                            slotBtn.getStyleClass().add("btn-danger"); 
                            slotBtn.setDisable(true);
                            slotBtn.setTooltip(new Tooltip("This session is full or the facility is under maintenance."));
                        }

                        timeSlotGrid.add(slotBtn, col, row);
                        currentHour++;
                    }
                }
            });
        }).start();
    }

    private void attemptReservation(String timeSlot, Button clickedButton) {
        if (isProcessing) return;

        String selectedVal = facilityComboBox.getValue();
        
        String selectedFacilityName = selectedVal.replace(" (Under Maintenance)", "")
                                                 .replace(" (Active)", "")
                                                 .replace(" (Bakımda)", "")
                                                 .replace(" (Aktif)", "")
                                                 .trim();
                                                 
        LocalDate selectedDate = datePicker.getValue();

        Facility foundFacility = null;
        for (Facility f : allFacilities) {
            if (f.getName().equals(selectedFacilityName)) {
                foundFacility = f;
                break;
            }
        }

        if (foundFacility == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "The selected facility was not found in the system.");
            return;
        }

        if (foundFacility.isUnderMaintenance()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Reservations cannot be made for this facility as it is currently under maintenance.");
            return;
        }

        final Facility targetFacility = foundFacility;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setText("Processing");
        clickedButton.setDisable(true);
        
        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                Reservation newRes = resManager.makeReservation(currentUser, targetFacility, selectedDate, timeSlot);

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (newRes != null) { 
                        refreshTimeSlots(); 
                        fetchFreshReservations(); 
                        showAlert(Alert.AlertType.INFORMATION, "Successful", targetFacility.getName() + " reservation for " + timeSlot + " has been created.");
                    } else {
                        clickedButton.setText(originalText);
                        clickedButton.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Operation Failed", "Reservation could not be created. (The facility may be full, you may have penalty points, or you may have exceeded the date limit.)");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setText(originalText);
                    clickedButton.setDisable(false);
                });
            }
        }).start();
    }

    private void fetchFreshReservations() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Platform.runLater(this::refreshUI);
            return;
        }

        new Thread(() -> {
            try {
                ArrayList<Reservation> dbList = resManager.getUserReservations(currentUser);
                
                // YENİ MANTIK: İptal edilenleri VE süresi dolmuş (deadline'ı geçmiş) rezervasyonları anında listeden uçur!
                dbList.removeIf(r -> {
                    if (r.isCancelled()) return true;
                    
                    if (r.getDate().isBefore(LocalDate.now())) {
                        return true; 
                    } else if (r.getDate().isEqual(LocalDate.now())) {
                        try {
                            // "13.00-14.00" stringinden bitiş saatini (14) çekiyoruz
                            String endTimeStr = r.getTimeSlot().split("-")[1];
                            int endHour = Integer.parseInt(endTimeStr.split("\\.")[0]);
                            
                            // Şu anki saat, rezervasyonun bitiş saatini geçmiş veya eşitse listeden uçur
                            if (LocalTime.now().getHour() >= endHour) {
                                return true;
                            }
                        } catch (Exception e) {
                            // Hata durumunda silmeyi pas geç
                        }
                    }
                    return false;
                });
                
                SessionManager.getInstance().setCurrentReservations(dbList);
            } catch (Throwable t) { 
                t.printStackTrace();
            } finally {
                Platform.runLater(this::refreshUI);
            }
        }).start();
    }

    private void refreshUI() {
        if (reservationsContainer == null) return;
        reservationsContainer.getChildren().clear();

        ArrayList<Reservation> myList = SessionManager.getInstance().getCurrentReservations();

        if (myList != null && !myList.isEmpty()) {
            for (Reservation r : myList) {
                reservationsContainer.getChildren().add(createReservationRow(r));
            }
        } else {
            Label emptyLabel = new Label("No active or past reservations found.");
            emptyLabel.setStyle("-fx-text-fill: #A3AED0; -fx-font-weight: bold; -fx-font-size: 14px;");
            reservationsContainer.getChildren().add(emptyLabel);
        }
    }

    private String formatReservationText(Reservation res) {
        if (res == null) return "";
        String facilityName = (res.getFacility() != null) ? res.getFacility().getName() : "Unknown Facility";
        
        String status;
        if (res.getFacility() != null && res.getFacility().isUnderMaintenance()) {
            status = " [Under Maintenance / Invalid]";
        } else if (res.isHasAttended()) {
            status = " [Attended]";
        } else {
            status = " [Active]";
        }
        
        return res.getDate().toString() + "   |   " + res.getTimeSlot() + "   |   " + facilityName + status;
    }

    private HBox createReservationRow(Reservation res) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12px; -fx-background-radius: 12px;");
        row.setPadding(new Insets(10, 20, 10, 20));

        Label infoLabel = new Label(formatReservationText(res));
        
        if (res.getFacility() != null && res.getFacility().isUnderMaintenance()) {
            infoLabel.setStyle("-fx-text-fill: #D93025; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            infoLabel.setStyle("-fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-font-size: 14px;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(infoLabel, spacer);

        if (!res.isHasAttended() && !res.getDate().isBefore(LocalDate.now())) {
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setPrefHeight(35.0);
            cancelBtn.setPrefWidth(100.0);
            cancelBtn.getStyleClass().add("btn-danger");
            
            cancelBtn.setOnAction(e -> cancelSpecificReservation(res, cancelBtn));
            row.getChildren().add(cancelBtn);
        }

        return row;
    }

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
                        refreshTimeSlots(); 
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