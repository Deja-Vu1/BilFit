package controllers;

import java.util.List;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import managers.SessionManager;
import managers.TeamManager;
import managers.TournamentManager;
import models.Student;
import models.Team;

public class TeamEditController {

    @FXML private Label teamNameLabel;
    @FXML private HBox addPlayerContainer;
    @FXML private TextField newPlayerEmailField;
    @FXML private Button addPlayerButton;
    @FXML private VBox membersContainer;

    private Team team;
    private Student currentUser;
    private boolean isCaptain;
    
    private TournamentManager tournamentManager;
    private TeamManager teamManager;

    @FXML
    public void initialize() {
        Database db = Database.getInstance();
        tournamentManager = new TournamentManager(db);
        teamManager = new TeamManager(db);
        
        try {
            currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTeam(Team team) {
        this.team = team;
        this.isCaptain = team.getCaptain() != null && team.getCaptain().getBilkentEmail().equals(currentUser.getBilkentEmail());
        
        teamNameLabel.setText(team.getTeamName() + " Members");

        // Kaptan değilse oyuncu ekleme menüsünü gizle
        if (isCaptain) {
            addPlayerContainer.setVisible(true);
            addPlayerContainer.setManaged(true);
        } else {
            addPlayerContainer.setVisible(false);
            addPlayerContainer.setManaged(false);
        }

        // Üyeleri dinamik olarak veritabanından çek ve yükle
        loadMembers();
    }

    private void loadMembers() {
        membersContainer.getChildren().clear();
        
        // Ekrana yükleniyor uyarısı koyalım
        Label loadingLabel = new Label("Loading members...");
        membersContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            try {
                // 1. Veritabanından güncel takım üyelerini çek
                List<Student> fetchedMembers = teamManager.getTeamMembers(team.getTeamId());
                
                // 2. Team objesinin içini güncel verilerle doldur
                team.setMembers(fetchedMembers);

                Platform.runLater(() -> {
                    membersContainer.getChildren().clear();

                    if (fetchedMembers == null || fetchedMembers.isEmpty()) {
                        membersContainer.getChildren().add(new Label("No members found."));
                        return;
                    }

                    // 3. UI'ı dinamik olarak oluştur
                    for (Student member : fetchedMembers) {
                        HBox row = new HBox();
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF;");
                        row.setPadding(new Insets(10, 15, 10, 15));

                        boolean isMemberCaptain = member.getBilkentEmail().equals(team.getCaptain().getBilkentEmail());
                        String role = isMemberCaptain ? " (Captain)" : "";
                        
                        Label nameLabel = new Label(member.getFullName() + role);
                        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14.0));
                        nameLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        row.getChildren().addAll(nameLabel, spacer);

                        // Eğer giriş yapan kişi kaptansa ve listelenen kişi kendi değilse "Kick" butonunu koy
                        if (this.isCaptain && !isMemberCaptain) {
                            Button kickBtn = new Button("Kick");
                            kickBtn.setStyle("-fx-background-color: #EE5D50; -fx-text-fill: white; -fx-background-radius: 5;");
                            kickBtn.setOnAction(e -> handleKickPlayer(member));
                            row.getChildren().add(kickBtn);
                        }

                        membersContainer.getChildren().add(row);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    membersContainer.getChildren().clear();
                    membersContainer.getChildren().add(new Label("Error loading members."));
                });
            }
        }).start();
    }

    @FXML
    public void handleAddPlayer(ActionEvent event) {
        String email = newPlayerEmailField.getText();
        if (email == null || email.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Email field cannot be empty.");
            return;
        }

        addPlayerButton.setText("Sending...");
        addPlayerButton.setDisable(true);

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                // Sadece email adresi ile geçici bir obje gönderiyoruz, DB email'den tanıyacak
                Student dummyReceiver = new Student("", email, "");
                status = tournamentManager.sendTeamInvite(team.getTeamId(), dummyReceiver);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            final DbStatus finalStatus = status;
            Platform.runLater(() -> {
                addPlayerButton.setText("Invite");
                addPlayerButton.setDisable(false);
                
                if (finalStatus == DbStatus.SUCCESS) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Invite sent successfully.");
                    newPlayerEmailField.clear();
                } else if (finalStatus == DbStatus.ALREADY_IN_TOURNAMENT) {
                    showAlert(Alert.AlertType.WARNING, "Warning", "User is already in a team or already invited.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not send invite. Check email address.");
                }
            });
        }).start();
    }

    private void handleKickPlayer(Student member) {
        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = teamManager.removeMemberFromTeam(team.getTeamId(), member);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            final DbStatus finalStatus = status;
            Platform.runLater(() -> {
                if (finalStatus == DbStatus.SUCCESS) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Player kicked successfully.");
                    // Listeyi baştan çekerek UI'ı tazele
                    loadMembers(); 
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not kick player.");
                }
            });
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(javafx.stage.StageStyle.UNDECORATED); 
        try { 
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); 
        } catch (Exception e) {}
        alert.showAndWait();
    }
}