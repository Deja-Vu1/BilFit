package java.core;
import java.database.Database;
import java.database.DbStatus;
import java.sql.ResultSet;

public class Main {
    public static void main(String[] args){
        System.out.println("Let's get the party started!");

        // Let's try to connect to our database
        Database myDB = new Database();
        if (myDB.connect() == DbStatus.SUCCESS){
            System.out.println("Done!");
        }
        else{
            System.out.println("Error!");
        }

        // myDB.addUser("Ada", 20);
        // myDB.deleteUser("Ada");
        ResultSet myResults = myDB.getAllUsers();
        try{
            while (myResults.next()) {
                System.out.println(
                    myResults.getInt("id") + " | " +
                    myResults.getString("name") + " | " +
                    myResults.getInt("age")
                );
            }
        }catch (Exception e) {
            e.getStackTrace();
        }
    }
}
