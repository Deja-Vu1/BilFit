package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URL; 
import java.util.ResourceBundle; 

public class SelectionController implements Initializable {

    @FXML private ImageView logoImageView;
    @FXML private Button studentLogBtn;
    @FXML private Button adminLogBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("SelectionController başarıyla yüklendi.");
    }

    @FXML
    private void goToStudentLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // FULL SCREEN KORUMA: Sadece içindekini değiştiriyoruz
            stage.getScene().setRoot(root);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAdminLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/AdminLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // FULL SCREEN KORUMA: Sadece içindekini değiştiriyoruz
            stage.getScene().setRoot(root);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}