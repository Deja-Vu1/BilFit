package java.controllers;

import java.controllers.models.Student;
import java.util.ArrayList;
import java.util.List;

public class StudentRegisterController {
    
    // UI Elements (These will be linked to JavaFX later)
    private String nameField;
    private String idField;
    private String emailField;
    private String passwordField;

    // mock database to store registered users until PostgreSQL is ready
    public static List< Student > temporaryDatabase = new ArrayList<>();

    // Constructor for simulate JavaFX input temporarry
    public StudentRegisterController(String name, String id, String email, String password) {
        
        this.nameField = name;
        this.idField = id;
        this.emailField = email;
        this.passwordField = password;
    }

            public boolean registerNewStudent() {


                System.out.println("Attempting to register new student...");


                if (nameField.isEmpty() || idField.isEmpty() || emailField.isEmpty() || passwordField.isEmpty()) {


                    System.out.println("Error: All fields must be filled.");
                    return false;

                }

        
if (!emailField.endsWith("@ug.bilkent.edu.tr") && !emailField.endsWith("@bilkent.edu.tr")) {



    System.out.println("Error: Only valid Bilkent students are allowed to register the Bil-Fit");
    return false;


}


        if (passwordField.length() < 6) { //can be change


            System.out.println("Error: Password must be at least 6 characters long.");
            return false;


        }

        // creating the model (Student)
        String defaultNickname = emailField.split("@")[0];




        Student newStudent = new Student(nameField, emailField, defaultNickname, passwordField, idField);

        // should be saved to DB (now saving to the temporary list)
        temporaryDatabase.add(newStudent);



        System.out.println("Success: Student " + newStudent.getFullName() + " has been registered!");
        
        deployHomePage(); // redirect to main screen

        return true;
    }

    public void deployHomePage() {


        System.out.println("Redirecting to StudentMainView...");

        // JavaFX scene change logic implemantation TODO



    }

    public void goBack() {


        System.out.println("Redirecting back to LoginView...");
        // JavaFX scene change logic implemeanttaion TODO



    }
}