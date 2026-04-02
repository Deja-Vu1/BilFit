package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class AdminMainController {
    
    @FXML
    private StackPane contentArea;
    @FXML 
    private AdminSidebarController sidebarController;

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setMainController(this);
        }
        loadHome(); 
    }


    public void loadView(String path) {
        try {
            Node view = FXMLLoader.load(
                getClass().getResource(path)
            );
            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

 
    public void loadHome() {
        loadView("/views/dashboard/HomeView.fxml");
    }

    public void loadAccount() {
        loadView("/views/dashboard/ProfileView.fxml");
    }

    public void loadReservations(){
        loadView("/views/dashboard/MyReservationsView.fxml");
    }

    public void loadSettings(){
        loadView("/views/dashboard/SettingsView.fxml");
    }
}
