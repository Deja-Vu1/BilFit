package controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import managers.SessionManager;

public class AdminSidebarController {
    
    private AdminMainController mainController;
    
    @FXML private Button btnHome;
    @FXML private Button btnAccount;
    @FXML private Button btnReservation;
    @FXML private Button btnFacilities; // EKLENDİ
    @FXML private Button btnSettings;
    
    @FXML 
    public void initialize() {
        // Başlangıçta ekstra bir şey yapmaya gerek yok
    }

    private void updateActiveButton(Button clickedButton) {
        // LİSTEYE btnFacilities EKLENDİ
        Button[] allButtons = {btnHome, btnAccount, btnReservation, btnFacilities, btnSettings};
        
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        
        if (clickedButton != null) {
            clickedButton.getStyleClass().add("active");
        }
    }

    public void setMainController(AdminMainController controller) {
        this.mainController = controller;
        updateActiveButton(btnHome); 
    }

    @FXML
    private void loadHome() {
        updateActiveButton(btnHome);
        if (mainController != null) mainController.loadHome();
    }

    @FXML
    private void loadAccount() {
        updateActiveButton(btnAccount);
        if (mainController != null) mainController.loadAccount();
    }

    @FXML 
    private void loadReservations() {
        updateActiveButton(btnReservation);
        if (mainController != null) mainController.loadReservations();
    }

    // YENİ EKLENEN FACILITIES YÖNLENDİRMESİ
    @FXML 
    private void loadFacilities() {
        updateActiveButton(btnFacilities);
        if (mainController != null) mainController.loadFacilities();
    }

    @FXML 
    private void loadSettings() {
        updateActiveButton(btnSettings);
        if (mainController != null) mainController.loadSettings();
    }

    @FXML 
    private void logout() {
        System.out.println("Admin çıkış yapıyor...");
        SessionManager.getInstance().logout();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnHome.getScene().getWindow(); 
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("HATA: Çıkış yapılırken SelectionView sayfası yüklenemedi!");
            e.printStackTrace();
        }
    }
}