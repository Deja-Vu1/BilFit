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
    @FXML private Button btnSettings;
    
    @FXML 
    public void initialize() {
        // Başlangıçta ekstra bir şey yapmaya gerek yok, MainController bağlanınca Home aktif olacak
    }

    // Aktif butonu CSS ile vurgulamak için
    private void updateActiveButton(Button clickedButton) {
        Button[] allButtons = {btnHome, btnAccount, btnReservation, btnSettings};
        
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        
        if (clickedButton != null) {
            clickedButton.getStyleClass().add("active");
        }
    }

    // AdminMainController tarafından çağrılır ve iki controller'ı birbirine bağlar
    public void setMainController(AdminMainController controller) {
        this.mainController = controller;
        updateActiveButton(btnHome); // Varsayılan olarak Home butonunu aktif yap
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

    @FXML 
    private void loadSettings() {
        updateActiveButton(btnSettings);
        if (mainController != null) mainController.loadSettings();
    }

    @FXML 
    private void logout() {
        System.out.println("Admin çıkış yapıyor...");
        
        // ÇÖZÜM: SessionManager üzerinden oturumu kapatıyoruz ki eski veriler hafızada kalmasın!
        SessionManager.getInstance().logout();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            
            // Mevcut pencereyi (Stage) bul ve sahneyi değiştir
            Stage stage = (Stage) btnHome.getScene().getWindow(); 
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("HATA: Çıkış yapılırken SelectionView sayfası yüklenemedi!");
            e.printStackTrace();
        }
    }
}