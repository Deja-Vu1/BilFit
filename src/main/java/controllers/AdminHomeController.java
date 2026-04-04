package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AdminHomeController {

    // TURNUVA İÇİN EKLENEN LABEL'LAR
    @FXML private Label tournament1Label;
    @FXML private Label tournament2Label;

    @FXML private TextField titleField;
    @FXML private TextArea messageArea;
    @FXML private TextArea emailsArea;
    @FXML private CheckBox broadcastCheckBox;
    @FXML private Button sendBtn;

    private Database db = Database.getInstance();
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        // Sayfa açıldığında turnuvaları yükle
        loadTournaments();

        // Broadcast seçildiğinde email girilen kutuyu kapatıyoruz
        broadcastCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            emailsArea.setDisable(newVal);
            if (newVal) {
                emailsArea.setPromptText("Sistemdeki tüm öğrencilere gönderilecek (Broadcast aktif)");
                emailsArea.clear();
            } else {
                emailsArea.setPromptText("Örn: ali@ug.bilkent.edu.tr, veli@ug.bilkent.edu.tr (Virgülle ayırarak yazın)");
            }
        });
    }

    // TURNUVALARI YÜKLEYEN METOT
    private void loadTournaments() {
        new Thread(() -> {
            try {
                // Not: İleride Database'e "getAllTournaments" yazıldığında buradan çekilecek
                String t1 = "Football Tournament  | 25.02.2026 - 20.03.2026  |  Max 10 player  | Ge250-251";
                String t2 = "Tennis Tournament  | 25.02.2026 - 20.03.2026  |  Max 4 player  | Ge250-251";

                Platform.runLater(() -> {
                    if (tournament1Label != null) tournament1Label.setText(t1);
                    if (tournament2Label != null) tournament2Label.setText(t2);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleSendNotification(ActionEvent event) {
        if (isProcessing) return;

        String title = titleField.getText();
        String message = messageArea.getText();
        boolean isBroadcast = broadcastCheckBox.isSelected();
        String emailsText = emailsArea.getText();

        if (title == null || title.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            showCustomAlert("Uyarı", "Başlık ve mesaj alanları boş bırakılamaz.");
            return;
        }

        if (!isBroadcast && (emailsText == null || emailsText.trim().isEmpty())) {
            showCustomAlert("Uyarı", "Lütfen en az bir mail adresi girin veya 'Herkese Gönder (Broadcast)' seçeneğini işaretleyin.");
            return;
        }

        isProcessing = true;
        sendBtn.setText("Gönderiliyor...");
        sendBtn.setDisable(true);

        new Thread(() -> {
            int successCount = 0;
            int failCount = 0;
            StringBuilder failEmails = new StringBuilder();

            if (isBroadcast) {
                DbStatus status = db.insertNotification("BROADCAST", title.trim(), message.trim());
                if (status == DbStatus.SUCCESS) successCount++;
                else failCount++;
            } else {
                String[] emails = emailsText.split(",");
                for (String email : emails) {
                    String cleanEmail = email.trim();
                    if (!cleanEmail.isEmpty()) {
                        DbStatus status = db.insertNotification(cleanEmail, title.trim(), message.trim());
                        if (status == DbStatus.SUCCESS) {
                            successCount++;
                        } else {
                            failCount++;
                            failEmails.append(cleanEmail).append("\n");
                        }
                    }
                }
            }

            final int finalSuccess = successCount;
            final int finalFail = failCount;
            final String finalFailStr = failEmails.toString();

            Platform.runLater(() -> {
                isProcessing = false;
                sendBtn.setText("Send Notification");
                sendBtn.setDisable(false);

                if (isBroadcast) {
                    if (finalSuccess > 0) {
                        showCustomAlert("Başarılı", "Broadcast (Genel Duyuru) başarıyla tüm kullanıcılara gönderildi.");
                        titleField.clear();
                        messageArea.clear();
                    } else {
                        showCustomAlert("Hata", "Broadcast gönderilirken veritabanı hatası oluştu.");
                    }
                } else {
                    if (finalFail == 0 && finalSuccess > 0) {
                        showCustomAlert("Başarılı", "Bildirim " + finalSuccess + " kişiye başarıyla gönderildi!");
                        titleField.clear();
                        messageArea.clear();
                        emailsArea.clear();
                    } else if (finalSuccess > 0 && finalFail > 0) {
                        showCustomAlert("Kısmi Başarı", finalSuccess + " kişiye başarıyla gönderildi ancak şu kişilere gönderilemedi (Kullanıcı bulunamadı):\n" + finalFailStr);
                    } else {
                        showCustomAlert("Hata", "Hiçbir bildirim gönderilemedi. Girdiğiniz e-posta adreslerinin sistemde kayıtlı olduğundan emin olun.");
                    }
                }
            });
        }).start();
    }

    private void showCustomAlert(String title, String message) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
        
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
        okBtn.setOnAction(e -> dialogStage.close());

        layout.getChildren().addAll(titleLabel, msgLabel, okBtn);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }
}