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
        long start = System.currentTimeMillis(); 
        String FacilityName = "Main Sports Hall - Court A";
        String TIME_SLOT = "17:00-18:30";
        LocalDate testDate = LocalDate.now();
        boolean isAvailable = myDB.checkFacilityAvailability(FacilityName, testDate, TIME_SLOT);
        if (isAvailable) {
            System.out.println("+ Facility '" + FacilityName + "' is available on " + testDate + " at " + TIME_SLOT);
        } else {
            System.out.println("- Facility '" + FacilityName + "' is NOT available on " + testDate + " at " + TIME_SLOT);
        }
        long finish = System.currentTimeMillis();

        System.out.println("in " + (finish-start) + "ms");
    }
}