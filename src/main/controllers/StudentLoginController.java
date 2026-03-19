package controllers;

import models.Student;

public class StudentLoginController {
    
    // UI Elements
    private String emailField;
    private String passwordField;

    public StudentLoginController(String email, String password) {

        this.emailField = email;
        this.passwordField = password;



    }

    public boolean logIn() {





        System.out.println("Attempting to log in with email: " + emailField);

        // Input validation
        if (emailField.isEmpty() || passwordField.isEmpty()) {


            System.out.println("Error: Email or password fields  cannot be empty.");

            return false;


        }

        // must check in DB (currently checks in temp)
        for (Student student : StudentRegisterController.temporaryDatabase) {


            if (student.login(emailField, passwordField)) {


                System.out.println(" Login successful! \n Welcome, " + student.getNickname());





                deployHomepage();
                return true;



            }
        }

        System.out.println("Error: Invalid email or password.");
        return false;

    }

    public void deployHomepage() {

        System.out.println("Redirecting to StudentMainView");

    }

    public void deployStudentRegister() {


        System.out.println("Redirecting to StudentRegisterView");


    }

    public void goBack() {




        System.out.println("Redirecting to Main Selection Screen");
    }
}