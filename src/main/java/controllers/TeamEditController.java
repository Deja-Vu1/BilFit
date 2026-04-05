package controllers;

import java.util.ArrayList;
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
    private boolean USE_MOCK_DATA = true;
    private TournamentManager tournamentManager;

    @FXML
    public void initialize() {
        if (!USE_MOCK_DATA) {
            tournamentManager = new TournamentManager(Database.getInstance());
        }
        try {
            if (USE_MOCK_DATA) {
                currentUser = new Student("Onur Arda Özçimen", "onur@ug.bilkent.edu.tr", "22200000");
            } else {
                currentUser = (Student) SessionManager.getInstance().getCurrentUser();
            }
        } catch (Exception e) {}
    }

    public void setTeam(Team team) {
        this.team = team;
        this.isCaptain = team.getCaptain() != null && team.getCaptain().getBilkentEmail().equals(currentUser.getBilkentEmail());
        
        teamNameLabel.setText(team.getTeamName() + " Members");

        if (isCaptain) {
            addPlayerContainer.setVisible(true);
            addPlayerContainer.setManaged(true);
        } else {
            addPlayerContainer.setVisible(false);
            addPlayerContainer.setManaged(false);
        }

        loadMembers();
    }

    private void loadMembers() {
        membersContainer.getChildren().clear();

        List<Student> members = new ArrayList<>();
        if (USE_MOCK_DATA) {
            members.add(team.getCaptain());
            if (!team.getCaptain().getBilkentEmail().equals("test@bilkent.edu.tr")) {
                members.add(new Student("Ahmet Yılmaz", "test@bilkent.edu.tr", "22001122"));
            }
        } else {
            members = team.getMembers();
        }

        for (Student member : members) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #FFFFFF;");
            row.setPadding(new Insets(10, 15, 10, 15));

            String role = member.getBilkentEmail().equals(team.getCaptain().getBilkentEmail()) ? " (Captain)" : "";
            Label nameLabel = new Label(member.getFullName() + role);
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14.0));
            nameLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(nameLabel, spacer);

            if (isCaptain && !member.getBilkentEmail().equals(currentUser.getBilkentEmail())) {
                Button kickBtn = new Button("Kick");
                kickBtn.setStyle("-fx-background-color: #EE5D50; -fx-text-fill: white; -fx-background-radius: 5;");
                kickBtn.setOnAction(e -> handleKickPlayer(member));
                row.getChildren().add(kickBtn);
            }

            membersContainer.getChildren().add(row);
        }
    }

    @FXML
    public void handleAddPlayer(ActionEvent event) {
        String email = newPlayerEmailField.getText();
        if (email == null || email.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Email field cannot be empty.");
            return;
        }

        if (USE_MOCK_DATA) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "MOCK: Invite sent to " + email);
            newPlayerEmailField.clear();
            return;
        }

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = Database.getInstance().sendTeamInvite("MOCK_TOUR_ID", currentUser.getBilkentEmail(), email);
            } catch (Exception ex) {}

            final DbStatus finalStatus = status;
            Platform.runLater(() -> {
                if (finalStatus == DbStatus.SUCCESS) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Invite sent successfully.");
                    newPlayerEmailField.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not send invite.");
                }
            });
        }).start();
    }

    private void handleKickPlayer(Student member) {
        if (USE_MOCK_DATA) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "MOCK: " + member.getFullName() + " kicked from team.");
            loadMembers();
            return;
        }

        new Thread(() -> {
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                status = Database.getInstance().rejectTeamInvite(team.getTeamId(), member.getBilkentEmail());
            } catch (Exception ex) {}

            final DbStatus finalStatus = status;
            Platform.runLater(() -> {
                if (finalStatus == DbStatus.SUCCESS) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Player kicked successfully.");
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