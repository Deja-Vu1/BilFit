package controllers; import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
public class AdminResetPasswordInputController {
    @FXML public void initialize() {} 
    


    @FXML
    public void goBack(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminResetPasswordView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("An error occurred while loading the view!");
            e.printStackTrace();
        }
    }
    @FXML
    public void resetPassword(ActionEvent event) {
        System.out.println("Resetting password...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("An error occurred while loading the view!");
            e.printStackTrace();
        }
    }


}