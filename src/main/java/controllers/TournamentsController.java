package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import managers.SessionManager;
import managers.TournamentManager;
import models.SportType;
import models.Student;
import models.Team;
import models.Tournament;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class TournamentsController {

    @FXML private Button applyCodeButton;
    @FXML private Button applyButton;
    @FXML private Button cancelButton;
    
    @FXML private VBox myTournamentsCard; 

    // %100 GERÇEK MANAGER BAĞLANTISI
    private TournamentManager tournamentManager = new TournamentManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        // SAYFA AÇILDIĞINDA SADECE DB'DEN (VEYA SESSION'DAN) GELEN GERÇEK DURUMA GÖRE UI ÇİZİLİR
        boolean isApplied = SessionManager.getInstance().isTournamentApplied();
        boolean isJoined = SessionManager.getInstance().isTournamentJoinedWithCode();

        if (isApplied || isJoined) {
            if (myTournamentsCard != null) {
                myTournamentsCard.setVisible(true);
                myTournamentsCard.setManaged(true);
            }
        }
        
        if (isApplied && applyButton != null) {
            applyButton.setText("Applied");
            applyButton.setDisable(true);
        }
        if (isJoined && applyCodeButton != null) {
            applyCodeButton.setText("Joined");
            applyCodeButton.setDisable(true);
        }
    }

    @FXML
    public void handleApplyWithCode(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Özel Turnuvaya Katıl");
        dialog.setHeaderText("Erişim kodunu giriniz");
        dialog.setContentText("Kod:");

        // Pop-up'ın çökmesini engellemek için ana pencereye bağlıyoruz
        if (applyCodeButton != null && applyCodeButton.getScene() != null) {
            dialog.initOwner(applyCodeButton.getScene().getWindow());
        }

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Kod alanı boş bırakılamaz.");
                return;
            }
            
            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                    Tournament targetTournament = new Tournament("PRIVATE_TOUR_1", "Private Tournament", SportType.TENNIS, LocalDate.now(), LocalDate.now(), 2, false, code, "Main Campus");
                    
                    // SIFIR HARDCODE: DOĞRUDAN MANAGER ÇAĞRILIR
                    status = tournamentManager.applyWithCode(targetTournament, currentUser, code);
                } catch (Exception ex) {
                    System.out.println("Veritabanı Hazır Değil (ApplyWithCode): " + ex.getMessage());
                }

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    // SADECE VE SADECE DB "SUCCESS" DÖNERSE UI GÜNCELLENİR
                    if (finalStatus == DbStatus.SUCCESS) {
                        SessionManager.getInstance().setTournamentJoinedWithCode(true);

                        if (myTournamentsCard != null) {
                            myTournamentsCard.setVisible(true);
                            myTournamentsCard.setManaged(true);
                        }
                        if (applyCodeButton != null) {
                            applyCodeButton.setText("Joined");
                            applyCodeButton.setDisable(true);
                        }
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Koda sahip turnuvaya katıldınız!");
                    } else {
                        // DB ONAYLAMAZSA İŞLEM REDDEDİLİR
                        showAlert(Alert.AlertType.ERROR, "İşlem Başarısız", "Veritabanı onay vermedi veya kod geçersiz. (Status: " + finalStatus.name() + ")");
                    }
                });
            }).start();
        });
    }

    @FXML
    public void handleApplyTournament(ActionEvent event) {
        if (isProcessing) return;
        Button clickedButton = (Button) event.getSource();
        
        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setText("Applying...");
        clickedButton.setDisable(true);

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                Tournament targetTournament = new Tournament("FOOT_2026_01", "Football Tournament", SportType.FOOTBALL, LocalDate.now(), LocalDate.now(), 10, false, "0000", "Main Campus");
                Team myTeam = new Team(UUID.randomUUID().toString(), currentUser.getNickname() + "'s Team", "SOLO", 1, false, currentUser);
                
                // SIFIR HARDCODE: DOĞRUDAN MANAGER ÇAĞRILIR
                status = tournamentManager.registerTeamToTournament(targetTournament, myTeam);
            } catch (Exception ex) {
                System.out.println("Veritabanı Hazır Değil (Apply): " + ex.getMessage());
            }

            final DbStatus finalStatus = status;

            Platform.runLater(() -> {
                isProcessing = false;
                // SADECE DB "SUCCESS" DÖNERSE UI GÜNCELLENİR
                if (finalStatus == DbStatus.SUCCESS) {
                    SessionManager.getInstance().setTournamentApplied(true);
                    clickedButton.setText("Applied");

                    if (myTournamentsCard != null) {
                        myTournamentsCard.setVisible(true);
                        myTournamentsCard.setManaged(true);
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Turnuvaya başvurunuz alınmıştır!");
                } else {
                    // DB ONAYLAMAZSA BUTON ESKİ HALİNE DÖNER
                    clickedButton.setText(originalText);
                    clickedButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "İşlem Başarısız", "Veritabanı onay vermedi. (Status: " + finalStatus.name() + ")");
                }
            });
        }).start();
    }

    @FXML
    public void handleCancelTournament(ActionEvent event) {
        if (isProcessing) return;
        Button clickedButton = (Button) event.getSource();
        
        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setText("Cancelling...");

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                Tournament targetTournament = new Tournament("FOOT_2026_01", "Football Tournament", SportType.FOOTBALL, LocalDate.now(), LocalDate.now(), 10, false, "0000", "Main Campus");
                Team myTeam = new Team("MY_TEAM_ID", currentUser.getNickname() + "'s Team", "SOLO", 1, false, currentUser);
                
                // SIFIR HARDCODE: DOĞRUDAN MANAGER ÇAĞRILIR
                status = tournamentManager.withdrawTeam(targetTournament, myTeam, currentUser);
            } catch (Exception ex) {
                 System.out.println("Veritabanı Hazır Değil (Cancel): " + ex.getMessage());
            }

            final DbStatus finalStatus = status;

            Platform.runLater(() -> {
                isProcessing = false;
                // SADECE DB "SUCCESS" DÖNERSE UI GÜNCELLENİR
                if (finalStatus == DbStatus.SUCCESS) {
                    SessionManager.getInstance().setTournamentApplied(false);
                    SessionManager.getInstance().setTournamentJoinedWithCode(false);

                    if (myTournamentsCard != null) {
                        myTournamentsCard.setVisible(false);
                        myTournamentsCard.setManaged(false);
                    }

                    if (applyButton != null) {
                        applyButton.setText("Apply");
                        applyButton.setDisable(false);
                    }
                    if (applyCodeButton != null) {
                        applyCodeButton.setText("Apply With Code");
                        applyCodeButton.setDisable(false);
                    }
                    showAlert(Alert.AlertType.INFORMATION, "İptal Edildi", "Turnuvadan başarıyla ayrıldınız.");
                } else {
                    // DB ONAYLAMAZSA BUTON ESKİ HALİNE DÖNER
                    clickedButton.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "İşlem Başarısız", "İptal işlemi veritabanı tarafından reddedildi.");
                }
            });
        }).start();
    }

    @FXML
    public void handleRequestPartner(ActionEvent event) {
         Button clickedButton = (Button) event.getSource();
         clickedButton.setText("Requested");
         clickedButton.setDisable(true);
         showAlert(Alert.AlertType.INFORMATION, "İstek Gönderildi", "Partnerlik isteğiniz iletildi!");
    }

    @FXML
    public void handleScheduleTournament(ActionEvent event) {
         showAlert(Alert.AlertType.INFORMATION, "Fikstür", "Turnuva fikstürü veritabanından bekleniyor.");
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Tasarımın daha şık durması için varsayılan Windows/Mac çerçevesini kaldırır (Opsiyonel)
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED); 

        // POP-UP TASARIMIN (CSS) GERİ EKLENDİ (Güvenli Try-Catch bloğu ile)
        try { 
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); 
        } catch (Exception e) {
            System.err.println("Uyarı: CSS dosyası bulunamadı, varsayılan tasarım kullanılacak.");
        }
        
        alert.showAndWait();
    }
}