package controllers; import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
public class ResetPasswordController {
    @FXML public void initialize() {} 
    @FXML
    public void goBack(MouseEvent event) {
        System.out.println("Redirecting to StudentLogin Screen");
        try {
            // 1. Yeni FXML dosyasını yükle (Yolun doğru olduğundan emin ol)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auth/StudentLoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("StudentLoginView yüklenirken hata oluştu!");
            e.printStackTrace();
        }
    }
    @FXML
    public void attemptLogin(MouseEvent event) {
        System.out.println("Attempting to reset password...");
    }


}