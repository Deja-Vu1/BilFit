package controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import models.Student;
import javafx.scene.Node;

public class StudentLoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void attemptLogin(ActionEvent event) {
        
        String emailInput = emailField.getText();
        String passwordInput = passwordField.getText();

        deployHomepage(event);
        /*
        System.out.println("Attempting to log in with email: " + emailInput);

        if (emailInput == null || emailInput.isEmpty() || passwordInput == null || passwordInput.isEmpty()) {
            System.out.println("Error: Email or password fields cannot be empty.");
            return;
        }

        boolean isLogged = false;

        for (Student student : StudentRegisterController.temporaryDatabase) {
            if (student.login(emailInput, passwordInput)) {
                System.out.println(" Login successful! \n Welcome, " + student.getNickname());
                deployHomepage(event);
                isLogged = true;
                break;
            }
        }

        if (!isLogged) {
            System.out.println("Error: Invalid email or password.");
        }
        */
    }

    public void deployHomepage(ActionEvent event) {
        System.out.println("Redirecting to StudentMainView");
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/StudentMainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentMainView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
        
    }

    @FXML
    public void goToRegister(MouseEvent event) {
        System.out.println("Redirecting to StudentRegisterView");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentRegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentRegisterView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToForgotPassword(MouseEvent event) {
        System.out.println("Redirecting to Forgot Password Screen...");
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to Main Selection Screen");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("SelectionView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }
}