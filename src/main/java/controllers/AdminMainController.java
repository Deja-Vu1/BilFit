package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class AdminMainController {
    
    @FXML private StackPane contentArea;
    @FXML private AdminSidebarController sidebarController;

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setMainController(this);
            System.out.println("Admin Sidebar successfully connected to the main screen.");
        } else {
            System.err.println("ATTENTION: Admin Sidebar failed to load! Please check the fx:id in the FXML file.");
        }
        
        loadHome(); 
    }

    public void loadView(String path) {
        try {
            System.out.println("Page Loading: " + path);
            Node view = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("Error occurred while loading view: " + path);
            e.printStackTrace();
        }
    }

    public void loadHome() {
        loadView("/views/dashboard/AdminHomeView.fxml");
    }
    
    public void loadAccount() {
        loadView("/views/dashboard/AdminProfileView.fxml");
    }

    public void loadReservations() {
     
        loadView("/views/auth/AdminReservation.fxml"); 
    }
    
    public void loadFacilities() {
        loadView("/views/dashboard/AdminFacilitiesView.fxml"); 
    }

    public void loadSettings() {
        loadView("/views/dashboard/AdminSettingsView.fxml");
    }

    public void loadTournaments() {
        loadView("/views/dashboard/AdminTournamentsView.fxml");
    }
}