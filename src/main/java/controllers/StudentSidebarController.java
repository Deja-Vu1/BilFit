package controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class StudentSidebarController {

    private StudentMainController mainController;

    @FXML 
    private Button btnHome;
    
    @FXML 
    private Button btnAccount;
    
    @FXML 
    private Button btnTournaments;
    
    @FXML 
    private Button btnELO;
    
    @FXML 
    private Button btnReservation;

    @FXML 
    private Button btnSettings;

    @FXML 
    private Button btnFriends;

    private void updateActiveButton(Button clickedButton) {
        Button[] allButtons = {btnHome, btnAccount, btnTournaments, btnELO, btnReservation, btnSettings, btnFriends};
        
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
    public void setMainController(StudentMainController controller) {
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

    @FXML private void loadTournaments() {
        updateActiveButton(btnTournaments);
        mainController.loadTournaments();
    }
    @FXML private void loadELO() {
        updateActiveButton(btnELO);
        mainController.loadELO();
    }
    @FXML private void loadReservations() {
        updateActiveButton(btnReservation);
        mainController.loadReservations();
    }
    @FXML private void loadSettings() {
        updateActiveButton(btnSettings);
        mainController.loadSettings();
    }
     @FXML private void loadFriends() {
        updateActiveButton(btnFriends);
        mainController.loadFriends();
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
