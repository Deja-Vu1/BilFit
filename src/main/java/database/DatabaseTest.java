package database;

import java.time.LocalDate;

import models.Student;

public class DatabaseTest {

    public static void main(String[] args) {
        System.out.println("Starting database connection test...");
        
        // Veritabanı instance'ını alıyoruz
        Database myDB = Database.getInstance();
        
        // Bağlantıyı test ediyoruz
        if (myDB.testConnection() == DbStatus.SUCCESS) {
            System.out.println("+ Database connection is SUCCESSFUL!");
        } else {
            System.err.println("- Database connection FAILED! Please check the database server and settings.");
        }
        String FacilityName = "Main Sports Hall - Court A";
        String TIME_SLOT = "15:30-17:00";
        LocalDate testDate = LocalDate.now();
        long start = System.currentTimeMillis(); 
        System.out.println(myDB.getFacilities());
        System.out.println(myDB.getReservationsByEmail("ilkan.seckin@ug.bilkent.edu.tr"));
        long finish = System.currentTimeMillis();

        System.out.println("in " + (finish-start) + "ms");
    }
}