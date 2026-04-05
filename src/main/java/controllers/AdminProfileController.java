package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import managers.AdminManager;
import managers.SessionManager;
import models.Admin;
import models.User;

import java.util.List;

public class AdminProfileController {

    @FXML private Label nameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalAdminsLabel; // YENİ EKLENDİ
    @FXML private Label totalFacilitiesLabel;
    @FXML private Label adminActionsLabel;

    private Database db = Database.getInstance();
    private AdminManager adminManager = new AdminManager(db); // MANAGER BAĞLANTISI

    @FXML
    public void initialize() {
        loadAdminProfileData();
    }

    private void loadAdminProfileData() {
        // Arayüz donmasın diye arka planda (Thread) çalıştırıyoruz
        new Thread(() -> {
            try {
                User sessionUser = SessionManager.getInstance().getCurrentUser();
                if (!(sessionUser instanceof Admin)) return;
                
                Admin currentAdmin = (Admin) sessionUser;

                // 1. Veritabanından İstatistikleri Çek
                int facilitiesCount = db.getFacilities() != null ? db.getFacilities().size() : 0;
                
                // Manager üzerinden List<Integer> çekiyoruz [0: student, 1: admin]
                List<Integer> userCounts = adminManager.getUsersCount();
                int studentsCount = (userCounts != null && userCounts.size() >= 2) ? userCounts.get(0) : 0;
                int adminsCount = (userCounts != null && userCounts.size() >= 2) ? userCounts.get(1) : 0;
                
                // Admin modeline eklediğin değişkenden çekiyoruz
                int actionsCount = currentAdmin.getActionsPerformed();

                // 2. Arayüzü (JavaFX Thread) Güncelle
                Platform.runLater(() -> {
                    if (nameLabel != null) nameLabel.setText(currentAdmin.getFullName());
                    
                    if (totalFacilitiesLabel != null) totalFacilitiesLabel.setText(String.valueOf(facilitiesCount));
                    if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(studentsCount));
                    if (totalAdminsLabel != null) totalAdminsLabel.setText(String.valueOf(adminsCount));
                    if (adminActionsLabel != null) adminActionsLabel.setText(String.valueOf(actionsCount));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}