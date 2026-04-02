package database;

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

        if (myDB.loginStudent("talha.zorlu@ug.bilkent.edu.tr", "1") == DbStatus.SUCCESS) {
            System.out.println("+ Login successful!");
        } else {
            System.err.println("- Login failed.");
        }
    }
}