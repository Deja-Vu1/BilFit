package controllers; import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
public class AdminResetPasswordController {
    @FXML public void initialize() {} 
    


    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to StudentLogin Screen");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentLoginView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }
    @FXML
    public void sendActivationCode(ActionEvent event) {
        System.out.println("Sending Activation Code to email...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminPasswordActivationView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}