package com.example.bilfit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

public class SelectionController {

    @FXML private ImageView logoImageView;
    @FXML private Button studentLogBtn;
    @FXML private Button adminLogBtn;

    @FXML
    public void goToStudentLogin(ActionEvent event) {
        System.out.println("Öğrenci Giriş Ekranına Yönlendiriliyor...");
        // DB  buraya ekran değiştirme kodunu yazacak
    }

    @FXML
    public void goToAdminLogin(ActionEvent event) {
        System.out.println("Admin Giriş Ekranına Yönlendiriliyor...");
        // DB  buraya ekran değiştirme kodunu yazacak
    }
}
