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
import managers.AdminManager;
import managers.SessionManager;
import models.Admin;
import models.Facility;
import models.SportType;

public class AdminFacilitiesController {

    @FXML private VBox facilitiesContainer;
    
    
    private Map<CheckBox, Facility> facilitySelectionMap = new HashMap<>();

    private Database db = Database.getInstance();
    private AdminManager adminManager = new AdminManager(db);
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadFacilities();
    }

    
    private void loadFacilities() {
        if (facilitiesContainer == null) return;
        
        facilitiesContainer.getChildren().clear();
        facilitySelectionMap.clear(); 
        
        Label loadingLabel = new Label("Loading facilities...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
        facilitiesContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            ArrayList<Facility> facilities = db.getFacilities();

            Platform.runLater(() -> {
                facilitiesContainer.getChildren().clear();

                if (facilities.isEmpty()) {
                    Label emptyLabel = new Label("No facilities found in the system.");
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

   
    private HBox createFacilityCard(Facility facility) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        card.setPadding(new Insets(15, 20, 15, 20));

        
        CheckBox selectBox = new CheckBox();
        selectBox.setStyle("-fx-cursor: hand;");
        
        
        facilitySelectionMap.put(selectBox, facility);

       
        HBox nameAndStatusBox = new HBox(10);
        nameAndStatusBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(facility.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2B3674;");
        
        Label statusLabel = new Label();
        statusLabel.setPadding(new Insets(3, 8, 3, 8));
        statusLabel.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 11px;");

        Button actionBtn = new Button();
        actionBtn.setPrefHeight(35);
        actionBtn.setPrefWidth(140);
        actionBtn.setStyle("-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");

        if (facility.isUnderMaintenance()) {
            statusLabel.setText("UNDER MAINTENANCE");
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FCE8E8; -fx-text-fill: #D93025;");
            actionBtn.setText("Make Available");
            actionBtn.setStyle(actionBtn.getStyle() + "-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E;");
        } else {
            statusLabel.setText("ACTIVE");
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E;");
            actionBtn.setText("Put Under Maintenance");
            actionBtn.setStyle(actionBtn.getStyle() + "-fx-background-color: #FCE8E8; -fx-text-fill: #D93025;");
        }

        nameAndStatusBox.getChildren().addAll(nameLabel, statusLabel); 

       
        VBox textContainer = new VBox(5);
        String sportName = facility.getSportType() != null ? facility.getSportType().name().replace("_", " ") : "UNKNOWN";
        Label detailsLabel = new Label("Campus: " + facility.getCampusLocation() + "  |  Sport: " + sportName + "  |  Capacity: " + facility.getCapacity());
        detailsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #A3AED0;");
        
        textContainer.getChildren().addAll(nameAndStatusBox, detailsLabel); 

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        
        actionBtn.setOnAction(e -> handleToggleMaintenance(facility, actionBtn));

        card.getChildren().addAll(selectBox, textContainer, spacer, actionBtn);
        return card;
    }

    
    private void handleToggleMaintenance(Facility facility, Button clickedBtn) {
        if (isProcessing) return;
        isProcessing = true;
        clickedBtn.setText("İşleniyor...");

        new Thread(() -> {
            boolean newStatus = !facility.isUnderMaintenance(); 
            
            DbStatus status = db.updateFacilityMaintenance(facility.getName(), newStatus);
            
            if (status == DbStatus.SUCCESS) {
                if (newStatus) {
                    db.insertNotification("BROADCAST", 
                        "Facility Maintenance: " + facility.getName(), 
                        facility.getName() + " has been put under maintenance. Reservations will be unavailable for a temporary period.");
                } else {
                    db.insertNotification("BROADCAST", 
                        "Facility Available", 
                        facility.getName() + " is now available for reservations.");
                }
            }

            final DbStatus finalStatus = status;

            Platform.runLater(() -> {
                isProcessing = false;
                if (finalStatus == DbStatus.SUCCESS) {
                    showCustomAlert("Success", "Facility status updated and notification sent to users.");
                    loadFacilities(); 
                } else {
                    showCustomAlert("Error", "An error occurred while updating the facility status.");
                    loadFacilities(); 
                }
            });
        }).start();
    }

    
    @FXML
    public void handleBulkDelete(ActionEvent event) {
        if (isProcessing) return;

        java.util.List<Facility> selectedOnes = new java.util.ArrayList<>();
        
        
        for (Map.Entry<CheckBox, Facility> entry : facilitySelectionMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                selectedOnes.add(entry.getValue());
            }
        }

        if (selectedOnes.isEmpty()) {
            showCustomAlert("Warning", "Please select the facilities you want to delete from the checkboxes on the left.");
            return;
        }

        showCustomConfirmation(
            "Delete Facilities",
            "Are you sure you want to permanently delete the selected " + selectedOnes.size() + " facilities? This action cannot be undone.",
            () -> {
                isProcessing = true;
                new Thread(() -> {
                    int count = 0;
                    Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
                    
                    for (Facility f : selectedOnes) {
                        DbStatus status = adminManager.removeFacility(currentAdmin, f);
                        if (status == DbStatus.SUCCESS) {
                            count++;
                        }
                    }

                    final int finalCount = count;
                    Platform.runLater(() -> {
                        isProcessing = false;
                        if (finalCount > 0) {
                            showCustomAlert("Success", finalCount + " facilities deleted successfully.");
                        } else {
                            showCustomAlert("Error", "Failed to delete facilities. There might be existing reservations associated with these facilities.");
                        }
                        loadFacilities();
                    });
                }).start();
            },
            null
        );
    }

    
    @FXML
    public void handleBulkClose(ActionEvent event) {
        processBulkMaintenance(true, "Facility Maintenance", " has been put under maintenance.");
    }

    
    @FXML
    public void handleBulkOpen(ActionEvent event) {
        processBulkMaintenance(false, "Facility Available", " is now available for reservations.");
    }

   
    private void processBulkMaintenance(boolean shouldMaintain, String notifTitle, String notifMsg) {
        if (isProcessing) return;
        
        java.util.List<Facility> selectedOnes = new java.util.ArrayList<>();
        for (Map.Entry<CheckBox, Facility> entry : facilitySelectionMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                selectedOnes.add(entry.getValue());
            }
        }

        if (selectedOnes.isEmpty()) {
            showCustomAlert("Warning", "Please select the facilities you want to update from the checkboxes on the left.");
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
                    showCustomAlert("Success", finalCount + " facilities updated and notifications sent.");
                } else {
                    showCustomAlert("Information", "The selected facilities are already in the desired state.");
                }
                loadFacilities();
            });
        }).start();
    }

    
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

        Label titleLabel = new Label("Add New Facility");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

        TextField nameField = new TextField();
        nameField.setPromptText("Facility Name (e.g., East Campus Indoor Court)");
        nameField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        ComboBox<String> locationCombo = new ComboBox<>();
        locationCombo.getItems().addAll("Main Campus", "East Campus");
        locationCombo.setPromptText("Select Campus");
        locationCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacity (e.g., 14)");
        capacityField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        ComboBox<String> sportCombo = new ComboBox<>();
        for (SportType sport : SportType.values()) {
            sportCombo.getItems().add(sport.name().replace("_", " "));
        }
        sportCombo.setPromptText("Select Sport Type");
        sportCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button addBtn = new Button("Save");
        addBtn.setStyle("-fx-background-color: #1E8E3E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        
        addBtn.setOnAction(e -> {
            String fName = nameField.getText();
            String fLoc = locationCombo.getValue();
            String capStr = capacityField.getText();
            String fSport = sportCombo.getValue();

            if (fName == null || fName.trim().isEmpty() || fLoc == null || fSport == null || capStr == null || capStr.trim().isEmpty()) {
                showCustomAlert("Missing Information", "Please fill in all fields.");
                return;
            }

            try {
                int capacity = Integer.parseInt(capStr.trim());
                
                new Thread(() -> {
                    DbStatus status = db.insertFacility(fName.trim(), fLoc, capacity, fSport, false);
                    
                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) {
                            showCustomAlert("Success", "Facility added successfully!");
                            dialogStage.close();
                            loadFacilities();
                        } else {
                            showCustomAlert("Error", "Failed to add facility. A facility with this name may already exist.");
                        }
                    });
                }).start();
                
            } catch (NumberFormatException ex) {
                showCustomAlert("Invalid Format", "Capacity must contain only numbers.");
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

    
    private void showCustomConfirmation(String title, String message, Runnable onConfirm, Runnable onCancel) {
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
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #D93025;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            dialogStage.close();
            if (onCancel != null) onCancel.run();
        });

        Button confirmBtn = new Button("Yes, Delete");
        confirmBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            dialogStage.close();
            if (onConfirm != null) onConfirm.run();
        });

        btnBox.getChildren().addAll(cancelBtn, confirmBtn);
        layout.getChildren().addAll(titleLabel, msgLabel, btnBox);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    
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

        Button okBtn = new Button("OK");
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