package controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import models.Admin;

public class AdminSidebarController {
    private AdminMainController mainController;
    @FXML 
    private Button btnHome;
    
    @FXML 
    private Button btnAccount;
    
    
    @FXML 
    private Button btnReservation;

    @FXML 
    private Button btnSettings;
    @FXML public void initialize() {}
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
        @FXML
    public void setMainController(AdminMainController controller) {
        updateActiveButton(btnHome);
        this.mainController = controller;
    }

    @FXML
    private void loadHome() {
        updateActiveButton(btnHome);
        mainController.loadHome();
    }

    @FXML
    private void loadAccount() {
        updateActiveButton(btnAccount);
        mainController.loadAccount();
    }

    @FXML private void loadReservations() {
        updateActiveButton(btnReservation);
        mainController.loadReservations();
    }
    @FXML private void loadSettings() {
        updateActiveButton(btnSettings);
        mainController.loadSettings();
    }
    @FXML private void logout() {
        System.out.println("Redirecting to Main Selection Screen");
        try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) btnHome.getScene().getWindow(); 
        stage.getScene().setRoot(root);

    } catch (IOException e) {
        System.err.println("Error: Could not load SelectionView.");
        e.printStackTrace();
    }
    }
}
