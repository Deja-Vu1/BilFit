package database;

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
        for (Student s : myDB.getAllPublicStudents()) {
            System.out.println("Student: " + s.getFullName() + ", Email: " + s.getBilkentEmail());
            System.out.println("Elo Points: " + s.getEloPoint() + ", Penalty Points: " + s.getPenaltyPoints());
            System.out.println("Reliability Score: " + s.getReliabilityScore() + ", Rating Count: " + s.getRatingCount());
            System.out.print("Interests: " + s.getInterests());
            System.out.println();
        }
        long finish = System.currentTimeMillis();

        System.out.println("in " + (finish-start) + "ms");
    }
}