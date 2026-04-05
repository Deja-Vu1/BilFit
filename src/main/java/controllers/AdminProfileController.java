package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import managers.SessionManager;
import models.Admin;
import models.User;

public class AdminProfileController {

    @FXML private Label nameLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalFacilitiesLabel;
    @FXML private Label adminActionsLabel;

    private Database db = Database.getInstance();

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
                // Not: DB ekibi bu sayılar için özel metodlar yazdıysa burayı doğrudan onlara bağlayabilirsin.
                // Şimdilik sistemdeki tesisleri listeleyip sayısını alıyoruz.
                int facilitiesCount = db.getFacilities() != null ? db.getFacilities().size() : 0;
                
                // Eğer db.getAllStudents() gibi bir metod varsa:
                // int studentsCount = db.getAllStudents().size();
                int studentsCount = 42; // DB metodun yazılana kadar örnek veri
                
                // Admin'in yaptığı action sayısı (Örn: Gönderdiği bildirimler, açtığı turnuvalar)
                // int actionsCount = db.getAdminActionCount(currentAdmin.getBilkentEmail());
                int actionsCount = 15; // DB metodun yazılana kadar örnek veri

                // 2. Arayüzü (JavaFX Thread) Güncelle
                Platform.runLater(() -> {
                    if (nameLabel != null) {
                        nameLabel.setText(currentAdmin.getFullName());
                    }
                    
                    if (totalFacilitiesLabel != null) {
                        totalFacilitiesLabel.setText(String.valueOf(facilitiesCount));
                    }
                    
                    if (totalStudentsLabel != null) {
                        totalStudentsLabel.setText(String.valueOf(studentsCount));
                    }
                    
                    if (adminActionsLabel != null) {
                        adminActionsLabel.setText(String.valueOf(actionsCount));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}