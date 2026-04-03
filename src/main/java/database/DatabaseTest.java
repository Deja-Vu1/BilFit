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
        String wrongFacilityName = "Main Sports Hall - Court A";
        String TIME_SLOT = "14:00-15:30";
        LocalDate testDate = LocalDate.now();
        boolean isAvailable = myDB.checkFacilityAvailability(wrongFacilityName, testDate, TIME_SLOT);
        if (isAvailable) {
            System.out.println("+ Facility '" + wrongFacilityName + "' is available on " + testDate + " at " + TIME_SLOT);
        } else {
            System.out.println("- Facility '" + wrongFacilityName + "' is NOT available on " + testDate + " at " + TIME_SLOT);
        }
        long finish = System.currentTimeMillis();

        System.out.println("in " + (finish-start) + "ms");
    }
}