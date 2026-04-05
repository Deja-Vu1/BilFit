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
        // Artık Admin'in kendi özel paneli (Duyuru Merkezi) açılacak!
        loadView("/views/dashboard/AdminHomeView.fxml");
    }
    
    public void loadAccount() {
        // DÜZELTME: Öğrenci profili yerine Admin'in özel profil sayfası yüklenecek
        loadView("/views/dashboard/AdminProfileView.fxml");
    }

    public void loadReservations() {
        // Adminlerin tüm rezervasyonları gördüğü sayfa
        loadView("/views/dashboard/MyReservationsView.fxml"); 
    }
    
    public void loadFacilities() {
        loadView("/views/dashboard/AdminFacilitiesView.fxml"); 
    }
    public void loadSettings() {
        // DÜZELTME: Öğrenci ayarları yerine Admin'in özel ayarlar sayfası yüklenecek
        loadView("/views/dashboard/AdminSettingsView.fxml");
    }
}