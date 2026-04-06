package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import managers.AdminManager;
import managers.SessionManager;
import models.Admin;
import models.Student;

public class AdminStudentsController {

    private Database db = Database.getInstance();
    private AdminManager adminManager = new AdminManager(db);

    @FXML
    public void initialize() {
    }

    @FXML
    private void handleRecalculateStats(Student selectedStudent) {
        if (selectedStudent == null) {
            showCustomAlert("Hata", "Lütfen istatistiklerini güncellemek istediğiniz öğrenciyi seçin.");
            return;
        }

        new Thread(() -> {
            Admin currentAdmin = (Admin) SessionManager.getInstance().getCurrentUser();
            DbStatus status = adminManager.recalculateStudentMatchStats(currentAdmin, selectedStudent);

            Platform.runLater(() -> {
                if (status == DbStatus.SUCCESS) {
                    showCustomAlert("Başarılı", selectedStudent.getFullName() + " adlı öğrencinin maç ve kazanma oranı istatistikleri başarıyla senkronize edildi.\n" +
                            "Yeni Maç Sayısı: " + selectedStudent.getMatchesPlayed() + "\n" +
                            "Yeni Kazanma Oranı: %" + String.format("%.0f", selectedStudent.getWinRate() * 100));
                } else {
                    showCustomAlert("Hata", "Öğrenci istatistikleri senkronize edilemedi.");
                }
            });
        }).start();
    }

    private void showCustomAlert(String title, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        layout.setEffect(shadow);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");

        Button okBtn = new Button("Tamam");
        okBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        okBtn.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(titleLabel, msgLabel, okBtn);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }
}