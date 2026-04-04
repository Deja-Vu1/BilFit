package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class AdminMainController {
    
    @FXML private StackPane contentArea;
    
    // FXML'deki fx:id="sidebar" etiketi sayesinde JavaFX bu controller'ı otomatik doldurur
    @FXML private AdminSidebarController sidebarController;

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setMainController(this);
            System.out.println("Admin Sidebar başarıyla ana ekrana bağlandı.");
        } else {
            System.err.println("DİKKAT: Admin Sidebar yüklenemedi! FXML'deki fx:id kontrol edilmeli.");
        }
        
        // Uygulama açıldığında varsayılan olarak Home (Ana Sayfa) yüklensin
        loadHome(); 
    }

    // SAYFA YÜKLEME MOTORU (Çökmeye Karşı Korumalı)
    public void loadView(String path) {
        try {
            System.out.println("Sayfa Yükleniyor: " + path);
            Node view = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("HATA: '" + path + "' sayfası yüklenirken bir sorun oluştu!");
            e.printStackTrace();
        }
    }

    // --- YÖNLENDİRME METOTLARI ---
    
    public void loadHome() {
        // Not: Eğer Admin'e özel bir home sayfan varsa (Örn: AdminHomeView.fxml) burayı değiştirebilirsin
        loadView("/views/dashboard/HomeView.fxml");
    }

    public void loadAccount() {
        loadView("/views/dashboard/ProfileView.fxml");
    }

    public void loadReservations() {
        // Adminlerin tüm rezervasyonları gördüğü sayfa
        loadView("/views/dashboard/MyReservationsView.fxml"); 
    }

    public void loadSettings() {
        loadView("/views/dashboard/SettingsView.fxml");
    }
}