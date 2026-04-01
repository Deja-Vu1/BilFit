package core;

import database.Database;
import database.DbStatus;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCombination;
import java.io.IOException;

// HATALI IMPORTLAR SİLİNDİ:
// import org.postgresql.shaded.com.ongres.scram.client.MessageFlow.Stage; <-- BU SİLİNDİ
// import com.apple.eawt.Application; <-- BU SİLİNDİ (Sadece Mac'e özel ve JavaFX ile çakışır)

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // NOT: Eğer FXML dosyan src/main/resources/com/example/bilfit altındaysa 
        // yolu tam belirtmen gerekebilir: "/com/example/bilfit/SelectionView.fxml"
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/auth/SelectionView.fxml"));

        try {
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            stage.setTitle("Bilfit - Giriş Paneli");
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.show();
        } catch (Exception e) {
            System.err.println("FXML yüklenirken hata oluştu! Yolun doğruluğunu kontrol et.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Veritabanı bağlantısını ana thread'de kontrol ediyoruz
        Database myDB = new Database();
        if (myDB.testConnection() == DbStatus.SUCCESS){
            System.out.println("Veritabanı bağlantısı başarılı!");
        } else {
            System.out.println("Veritabanı bağlantı hatası!");
        }
        
        // JavaFX uygulamasını başlat
        launch(args);
    }
}