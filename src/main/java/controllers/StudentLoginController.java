package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import models.Student;

public class StudentLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void attemptLogin(ActionEvent event) {
        
        String emailInput = emailField.getText();
        String passwordInput = passwordField.getText();

        System.out.println("Attempting to log in with email: " + emailInput);

        if (emailInput == null || emailInput.isEmpty() || passwordInput == null || passwordInput.isEmpty()) {
            System.out.println("Error: Email or password fields cannot be empty.");
            return;
        }

        boolean isLogged = false;

        for (Student student : StudentRegisterController.temporaryDatabase) {
            if (student.login(emailInput, passwordInput)) {
                System.out.println(" Login successful! \n Welcome, " + student.getNickname());
                deployHomepage();
                isLogged = true;
                break;
            }
        }

        if (!isLogged) {
            System.out.println("Error: Invalid email or password.");
        }
    }

    public void deployHomepage() {
        System.out.println("Redirecting to StudentMainView");
    }

    @FXML
    public void goToRegister(MouseEvent event) {
        System.out.println("Redirecting to StudentRegisterView");
    }

    @FXML
    public void goToForgotPassword(MouseEvent event) {
        System.out.println("Redirecting to Forgot Password Screen...");
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to Main Selection Screen");
    }
}