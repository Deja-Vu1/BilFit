package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

public class StudentMainController {

    @FXML
    private StackPane contentArea;
    @FXML 
    private StudentSidebarController sidebarController;

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setMainController(this);
        }
        loadHome(); 
    }

    public void loadView(String path) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Page Loading Error");
            alert.setHeaderText("Interface file could not be loaded!");
            alert.setContentText("Faulty file: " + path + "\nReason: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void loadHome() { loadView("/views/dashboard/HomeView.fxml"); }
    public void loadAccount() { loadView("/views/dashboard/ProfileView.fxml"); }
    public void loadReservations(){ loadView("/views/dashboard/MyReservationsView.fxml"); }
    public void loadELO(){ loadView("/views/dashboard/ELOView.fxml"); }
    public void loadTournaments(){ loadView("/views/dashboard/TournamentsView.fxml"); }
    public void loadSettings(){ loadView("/views/dashboard/SettingsView.fxml"); }
    public void loadFriends(){ loadView("/views/dashboard/FriendsView.fxml"); }
}