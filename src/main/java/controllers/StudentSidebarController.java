package controllers;

import javafx.fxml.FXML;

public class StudentSidebarController {

    private StudentMainController mainController;

    @FXML
    public void setMainController(StudentMainController controller) {
        this.mainController = controller;
    }

    @FXML
    private void loadHome() {
        mainController.loadHome();
    }

    @FXML
    private void loadAccount() {
        mainController.loadAccount();
    }

    @FXML private void loadTournaments() {
        mainController.loadTournaments();
    }
    @FXML private void loadELO() {
        mainController.loadELO();
    }
    @FXML private void loadReservations() {
        mainController.loadReservations();
    }
    @FXML private void loadSettings() {
        mainController.loadSettings();
    }
    @FXML private void logout() {

    }
}
