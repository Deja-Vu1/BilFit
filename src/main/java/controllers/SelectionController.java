package controllers; // Paket adının klasör yapınla (src/main/java/controllers) uyumlu olduğundan emin ol

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.net.URL; // Bu importun doğruluğunu kontrol et
import java.util.ResourceBundle; // Bu importun doğruluğunu kontrol et

public class SelectionController implements Initializable {

    @FXML
    private ImageView logoImageView;

    @FXML
    private Button studentLogBtn;

    @FXML
    private Button adminLogBtn;

    /**
     * Bu metod, FXML dosyası yüklendiğinde otomatik olarak bir kez çalışır.
     * Initializable arayüzü bu metodun burada olmasını ŞART koşar.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("SelectionController başarıyla yüklendi.");
        
        // Örnek: Logo yükleme işlemini burada yapabilirsin
        try {
            // Eğer resim src/main/resources/images altındaysa:
            // Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
            // logoImageView.setImage(logo);
        } catch (Exception e) {
            System.err.println("Logo yüklenirken hata: " + e.getMessage());
        }
    }

    @FXML
    private void goToStudentLogin(ActionEvent event) {
        System.out.println("Öğrenci giriş ekranına yönlendiriliyor...");
        
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
    private void goToAdminLogin(ActionEvent event) {
         System.out.println("Öğrenci giriş ekranına yönlendiriliyor...");
        
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
}