package controllers;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import managers.SessionManager;
import managers.TournamentManager;
import models.SportType;
import models.Student;
import models.Team;
import models.Tournament;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TournamentsController {

    @FXML private Button applyCodeButton;
    @FXML private VBox myTournamentsCard; 
    @FXML private VBox upcomingTournamentsContainer;
    @FXML private VBox myTournamentsContainer;
    
    // TAB BUTTONS
    @FXML private Button btnIncoming;
    @FXML private Button btnOutgoing;
    @FXML private Button btnMyTeams;
    
    // CONTAINERS
    @FXML private VBox incomingRequestsContainer;
    @FXML private VBox outgoingRequestsContainer;
    @FXML private VBox myTeamsContainer;

    private TournamentManager tournamentManager;
    private boolean isProcessing = false;

    private boolean USE_MOCK_DATA = true; // MOCK VERİ AYARI

    @FXML
    public void initialize() {
        try {
            if (USE_MOCK_DATA) {
                System.out.println("===============================================================");
                System.out.println("====== DİKKAT: BU VERİLER SAHTEDİR (MOCK DATA) ======");
                System.out.println("====== DB'YE YAZILMAZ VE DB'DEN OKUNMAZ!       ======");
                System.out.println("===============================================================");
            } else {
                tournamentManager = new TournamentManager(Database.getInstance());
            }
            
            boolean isApplied = false;
            boolean isJoined = false;

            if (!USE_MOCK_DATA) {
                try {
                    isApplied = SessionManager.getInstance().isTournamentApplied();
                    isJoined = SessionManager.getInstance().isTournamentJoinedWithCode();
                } catch (Exception e) {}
            }

            if (isApplied || isJoined || USE_MOCK_DATA) {
                if (myTournamentsCard != null) {
                    myTournamentsCard.setVisible(true);
                    myTournamentsCard.setManaged(true);
                }
            }
            
            if (isJoined && applyCodeButton != null) {
                applyCodeButton.setText("Joined");
                applyCodeButton.setDisable(true);
            }

            // İlk başta sadece Incoming sekmesini göster
            showIncoming();

            loadUpcomingTournaments();
            loadTeamManagementData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- TAB MANAGEMENT (SEKME YÖNETİMİ) ---

    @FXML
    public void showIncoming() {
        if(incomingRequestsContainer != null) {
            incomingRequestsContainer.setVisible(true); incomingRequestsContainer.setManaged(true);
            outgoingRequestsContainer.setVisible(false); outgoingRequestsContainer.setManaged(false);
            myTeamsContainer.setVisible(false); myTeamsContainer.setManaged(false);
            updateTabStyles(btnIncoming);
        }
    }

    @FXML
    public void showOutgoing() {
        if(outgoingRequestsContainer != null) {
            incomingRequestsContainer.setVisible(false); incomingRequestsContainer.setManaged(false);
            outgoingRequestsContainer.setVisible(true); outgoingRequestsContainer.setManaged(true);
            myTeamsContainer.setVisible(false); myTeamsContainer.setManaged(false);
            updateTabStyles(btnOutgoing);
        }
    }

    @FXML
    public void showMyTeams() {
        if(myTeamsContainer != null) {
            incomingRequestsContainer.setVisible(false); incomingRequestsContainer.setManaged(false);
            outgoingRequestsContainer.setVisible(false); outgoingRequestsContainer.setManaged(false);
            myTeamsContainer.setVisible(true); myTeamsContainer.setManaged(true);
            updateTabStyles(btnMyTeams);
        }
    }

    private void updateTabStyles(Button activeBtn) {
        String inactiveStyle = "-fx-background-color: #E2E8F0; -fx-text-fill: #2b3674; -fx-background-radius: 8; -fx-font-weight: normal;";
        String activeStyle = "-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;";
        
        if(btnIncoming != null) btnIncoming.setStyle(inactiveStyle);
        if(btnOutgoing != null) btnOutgoing.setStyle(inactiveStyle);
        if(btnMyTeams != null) btnMyTeams.setStyle(inactiveStyle);
        
        if(activeBtn != null) activeBtn.setStyle(activeStyle);
    }

    // --- DATA LOADING ---

    private void loadUpcomingTournaments() {
        if (upcomingTournamentsContainer != null) {
            upcomingTournamentsContainer.getChildren().clear();
        }

        new Thread(() -> {
            List<Tournament> tournaments = null;
            
            if (USE_MOCK_DATA) {
                tournaments = getMockTournaments();
            } else {
                try {
                    tournaments = Database.getInstance().getAllActiveTournaments();
                } catch (Exception e) {}
            }

            if (tournaments != null && upcomingTournamentsContainer != null) {
                tournaments.sort(Comparator.comparing(Tournament::getStartDate));
                
                final List<Tournament> sortedTournaments = tournaments;
                Platform.runLater(() -> {
                    for (Tournament t : sortedTournaments) {
                        HBox row = createTournamentRow(t);
                        upcomingTournamentsContainer.getChildren().add(row);
                    }
                });
            }
        }).start();
    }

    private void loadTeamManagementData() {
        new Thread(() -> {
            try {
                List<Team> incoming;
                List<Team> outgoing;
                List<Team> myTeams;

                if (USE_MOCK_DATA) {
                    incoming = getMockTeams("incoming");
                    outgoing = getMockTeams("outgoing");
                    myTeams = getMockTeams("myteam");
                } else {
                    Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                    incoming = Database.getInstance().getIncomingTeamRequests(currentUser.getBilkentEmail());
                    outgoing = Database.getInstance().getOutgoingTeamRequests(currentUser.getBilkentEmail());
                    myTeams = Database.getInstance().getMyTeams(currentUser.getBilkentEmail());
                }

                Platform.runLater(() -> {
                    populateTeamList(incomingRequestsContainer, incoming, "incoming");
                    populateTeamList(outgoingRequestsContainer, outgoing, "outgoing");
                    populateTeamList(myTeamsContainer, myTeams, "myteam");
                    populateMyTournamentsList(myTournamentsContainer, myTeams);
                });

            } catch (Exception e) {}
        }).start();
    }

    private void populateMyTournamentsList(VBox container, List<Team> teams) {
        if (container == null) return;
        container.getChildren().clear();

        if (teams == null || teams.isEmpty()) {
            container.getChildren().add(new Label("No active tournaments."));
            return;
        }

        for (Team t : teams) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
            row.setPadding(new Insets(10, 20, 10, 20));

            VBox infoBox = new VBox(5);
            Label nameLabel = new Label("Tournament Match | Team: " + t.getTeamName());
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13.0));
            nameLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));
            
            Label subLabel = new Label("Status: Active | Players: " + t.getMaxCapacity());
            subLabel.setStyle("-fx-text-fill: #a0aec0;");
            infoBox.getChildren().addAll(nameLabel, subLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button scheduleBtn = new Button("Schedule");
            scheduleBtn.setPrefHeight(35.0);
            scheduleBtn.setPrefWidth(100.0);
            scheduleBtn.getStyleClass().add("btn-secondary");

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setPrefHeight(35.0);
            cancelBtn.setPrefWidth(100.0);
            cancelBtn.getStyleClass().add("btn-danger");
            HBox.setMargin(cancelBtn, new Insets(0, 0, 0, 10));

            row.getChildren().addAll(infoBox, spacer, scheduleBtn, cancelBtn);
            container.getChildren().add(row);
        }
    }

    private void populateTeamList(VBox container, List<Team> teams, String type) {
        if (container == null) return;
        container.getChildren().clear();

        if (teams == null || teams.isEmpty()) {
            container.getChildren().add(new Label("No records found."));
            return;
        }

        for (Team t : teams) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: #FFFFFF;");
            row.setPadding(new Insets(10));

            Label nameLabel = new Label(t.getTeamName());
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12.0));
            nameLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (type.equals("incoming")) {
                Button acceptBtn = new Button("Accept");
                acceptBtn.setStyle("-fx-background-color: #05CD99; -fx-text-fill: white; -fx-background-radius: 5;");
                acceptBtn.setOnAction(e -> handleTeamAction(t, "accept"));

                Button rejectBtn = new Button("Reject");
                rejectBtn.setStyle("-fx-background-color: #EE5D50; -fx-text-fill: white; -fx-background-radius: 5;");
                rejectBtn.setOnAction(e -> handleTeamAction(t, "reject"));
                
                HBox btnBox = new HBox(5, acceptBtn, rejectBtn);
                row.getChildren().addAll(nameLabel, spacer, btnBox);
            } else if (type.equals("outgoing")) {
                Label status = new Label("Pending");
                status.setTextFill(javafx.scene.paint.Color.web("#FFB547"));
                row.getChildren().addAll(nameLabel, spacer, status);
            } else {
                Label status = new Label("Active");
                status.setTextFill(javafx.scene.paint.Color.web("#05CD99"));
                row.getChildren().addAll(nameLabel, spacer, status);
            }

            container.getChildren().add(row);
        }
    }

    private void handleTeamAction(Team t, String action) {
        if (USE_MOCK_DATA) {
            showAlert(Alert.AlertType.INFORMATION, "Mock Data", "Aksiyon MOCK modunda alındı: " + action);
            return;
        }

        new Thread(() -> {
            Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
            DbStatus status = DbStatus.QUERY_ERROR;
            try {
                if (action.equals("accept")) {
                    status = tournamentManager.acceptTeamInvite(t, currentUser);
                } else {
                    status = tournamentManager.rejectTeamInvite(t, currentUser);
                }
            } catch (Exception e) {}

            final DbStatus finalStatus = status;
            Platform.runLater(() -> {
                if (finalStatus == DbStatus.SUCCESS) {
                    loadTeamManagementData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Action could not be completed.");
                }
            });
        }).start();
    }

    private HBox createTournamentRow(Tournament tournament) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 20));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String dateStr = tournament.getStartDate().format(dtf) + " - " + tournament.getEndDate().format(dtf);
        String infoText = tournament.getTournamentName() + "   |   " + dateStr + "   |   Max " + tournament.getMaxPlayersPerTeam() + " player";

        Label infoLabel = new Label(infoText);
        infoLabel.setTextFill(javafx.scene.paint.Color.web("#2b3674"));
        infoLabel.setFont(Font.font("System", FontWeight.BOLD, 13.0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button applyButton = new Button("Apply");
        applyButton.setPrefHeight(35.0);
        applyButton.setPrefWidth(100.0);
        applyButton.getStyleClass().add("btn-secondary");

        applyButton.setOnAction(e -> openTournamentDetails(tournament));

        row.getChildren().addAll(infoLabel, spacer, applyButton);
        return row;
    }

    private void openTournamentDetails(Tournament tournament) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard/TournamentDetailsView.fxml"));
            Parent root = loader.load();

            TournamentDetailsController controller = loader.getController();
            controller.setMockDataMode(USE_MOCK_DATA);
            controller.setTournament(tournament);

            Stage stage = new Stage();
            stage.setTitle(tournament.getTournamentName() + " Details");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 900, 700));
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Hata", "Sayfa acilamadi.");
        }
    }

    @FXML
    public void handleApplyWithCode(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Özel Turnuvaya Katıl");
        dialog.setHeaderText("Erişim kodunu giriniz");
        dialog.setContentText("Kod:");

        if (applyCodeButton != null && applyCodeButton.getScene() != null) {
            dialog.initOwner(applyCodeButton.getScene().getWindow());
        }

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Kod alani bos birakilamaz.");
                return;
            }
            
            if (USE_MOCK_DATA) {
                if (myTournamentsCard != null) {
                    myTournamentsCard.setVisible(true);
                    myTournamentsCard.setManaged(true);
                }
                if (applyCodeButton != null) {
                    applyCodeButton.setText("Joined");
                    applyCodeButton.setDisable(true);
                }
                showAlert(Alert.AlertType.INFORMATION, "Başarılı", "MOCK: Turnuvaya katildiniz.");
                return;
            }

            new Thread(() -> {
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                    Tournament targetTournament = new Tournament("PRIVATE_TOUR_1", "Private Tournament", SportType.TENNIS, LocalDate.now(), LocalDate.now(), 2, false, code, "Main Campus");
                    status = tournamentManager.applyWithCode(targetTournament, currentUser, code);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
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
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Turnuvaya katildiniz.");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Başarısız", "Gecersiz islem.");
                    }
                });
            }).start();
        });
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

    private List<Tournament> getMockTournaments() {
        List<Tournament> list = new ArrayList<>();
        list.add(new Tournament("T1", "Bilkent Spring Football", SportType.FOOTBALL, LocalDate.now().plusDays(2), LocalDate.now().plusDays(10), 11, false, "0000", "Main Campus"));
        list.add(new Tournament("T2", "Rector's Cup Tennis", SportType.TENNIS, LocalDate.now().plusDays(5), LocalDate.now().plusDays(15), 2, false, "0000", "East Campus"));
        return list;
    }

    private List<Team> getMockTeams(String type) {
        List<Team> list = new ArrayList<>();
        Student s1 = new Student("Ahmet Yılmaz", "ahmet@ug.bilkent.edu.tr", "22000001");
        Student s2 = new Student("Ayşe Demir", "ayse@ug.bilkent.edu.tr", "22000002");
        
        if (type.equals("incoming")) {
            list.add(new Team("TM1", "Ahmet's Squad (MOCK)", "INCOMING", 2, false, s1));
        } else if (type.equals("outgoing")) {
            list.add(new Team("TM2", "Ayşe's Squad (MOCK)", "OUTGOING", 2, false, s2));
        } else if (type.equals("myteam")) {
            list.add(new Team("TM3", "CS Masters (MOCK)", "MYTEAM", 2, false, new Student("Onur Arda Özçimen", "onur@ug.bilkent.edu.tr", "22200000")));
        }
        return list;
    }
}