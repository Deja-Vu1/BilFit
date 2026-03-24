package core;

import database.Database;
import database.DbStatus;

public class Main {
    public static void main(String[] args) {
        System.out.println("Bil-Fit Uygulaması Başlatılıyor...");

        // Veritabanı bağlantısını test et
        Database myDB = new Database();
        if (myDB.connect() == DbStatus.SUCCESS) {
            System.out.println("Veritabanı bağlantısı başarılı! Sistem hazır.");
        } else {
            System.out.println("Veritabanı bağlantısı kurulamadı. Lütfen db.properties dosyasını kontrol edin.");
        }
    }
}