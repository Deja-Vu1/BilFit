package core;

import database.Database;
import database.DbStatus;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage; // Sadece bu Stage kütüphanesi kalmalı
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
            stage.setResizable(false);
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

        if (myDB.verifyActivationCode("arda.ozcimen@ug.bilkent.edu.tr", "931078") == DbStatus.SUCCESS) {
            System.out.println("Aktivasyon kodu doğrulandı!");
            myDB.setProfileActivation("arda.ozcimen@ug.bilkent.edu.tr");
        } else {
            System.out.println("Aktivasyon kodu doğrulama başarısız!");
        }
        // JavaFX uygulamasını başlat
        launch(args);
    }
}