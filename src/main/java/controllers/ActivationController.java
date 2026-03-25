package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class ActivationController {

    @FXML private TextField activationCodeField;

    @FXML
    public void submitCode(ActionEvent event) {
        String code = activationCodeField.getText();
        
        System.out.println("Verifying Activation Code: " + code);
        
        if (code.isEmpty()) {
            System.out.println("Error: Activation code cannot be empty.");
            return;
        }
    }

    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to Previous Screen...");
    }
}