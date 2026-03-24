package core;

import database.Database;
import database.DbStatus;
import java.sql.ResultSet;

public class Main {
    public static void main(String[] args) {
        System.out.println("Let's get the party started!");

        
        Database myDB = new Database();
        if (myDB.connect() == DbStatus.SUCCESS) {
             System.out.println("Database connection established!");
        }
    }
}