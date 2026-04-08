package controllers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import managers.SessionManager;
import models.Notification;
import models.Student;
import models.Tournament;
import models.User;

public class HomeController {

    @FXML private VBox tournamentsContainer;
    @FXML private VBox notificationsContainer;

    private Database db = Database.getInstance();

    @FXML
    public void initialize() {
        loadHomeData();
    }

    private void loadHomeData() {
        new Thread(() -> {
            try {
                User sessionUser = SessionManager.getInstance().getCurrentUser();
                if (!(sessionUser instanceof Student)) return;
                
                Student currentUser = (Student) sessionUser;

                List<Notification> myNotifications = db.getNotificationsByStudent(currentUser);
                List<Tournament> allTournaments = db.getAllActiveTournaments();
                
                
                LocalDate currentDate = LocalDate.now();
                List<Tournament> upcomingTournaments = new ArrayList<>();
                
                if (allTournaments != null) {
                    upcomingTournaments = allTournaments.stream()
                        .filter(t -> t.getStartDate() != null && t.getStartDate().isAfter(currentDate))
                        .sorted(Comparator.comparing(Tournament::getStartDate))
                        .collect(Collectors.toList());
                }

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

                final List<Tournament> finalTournaments = upcomingTournaments;

                Platform.runLater(() -> {
                    if (tournamentsContainer != null) {
                        tournamentsContainer.getChildren().clear();

                        if (finalTournaments.isEmpty()) {
                            Label emptyTournamentsLabel = new Label("There are no upcoming tournaments at the moment.");
                            emptyTournamentsLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                            tournamentsContainer.getChildren().add(emptyTournamentsLabel);
                        } else {
                            for (int i = 0; i < finalTournaments.size(); i++) {
                                Tournament t = finalTournaments.get(i);
                                
                                if (i == 0) {
                                    HBox newEventTag = new HBox();
                                    newEventTag.setAlignment(Pos.CENTER_LEFT);
                                    newEventTag.setPadding(new Insets(0, 0, 0, 20));
                                    Label tagLabel = new Label("⭐ NEXT EVENT");
                                    tagLabel.getStyleClass().add("tag-orange");
                                    newEventTag.getChildren().add(tagLabel);
                                    tournamentsContainer.getChildren().add(newEventTag);
                                }

                                tournamentsContainer.getChildren().add(createTournamentRow(t, dateFormatter, currentUser));
                            }
                        }
                    }

                    if (notificationsContainer != null) {
                        notificationsContainer.getChildren().clear();

                        if (myNotifications == null || myNotifications.isEmpty()) {
                            Label emptyLabel = new Label("You have no new notifications at the moment.");
                            emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                            notificationsContainer.getChildren().add(emptyLabel);
                        } else {
                            for (Notification notif : myNotifications) {
                                notificationsContainer.getChildren().add(createNotificationRow(notif, dateTimeFormatter));
                            }
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private HBox createTournamentRow(Tournament t, DateTimeFormatter formatter, Student currentUser) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 20));
        row.setMinHeight(55.0); 

        String tName = (t.getTournamentName() != null && !t.getTournamentName().trim().isEmpty()) ? t.getTournamentName() : "Unnamed Tournament";
        String ge250Status = t.isHasGe250() ? "Ge250-251" : "No Ge250";
        String startDateStr = t.getStartDate() != null ? t.getStartDate().format(formatter) : "Unknown Date";
        String endDateStr = t.getEndDate() != null ? t.getEndDate().format(formatter) : "Unknown Date";

        String labelText = tName + " | " + startDateStr + " - " + endDateStr + " | Max " + t.getMaxPlayersPerTeam() + " players | " + ge250Status;

        Label tLabel = new Label(labelText);
        tLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");
        tLabel.setWrapText(false);
        tLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tLabel, Priority.ALWAYS);

        Button applyBtn = new Button("Apply");
        applyBtn.setPrefHeight(35.0);
        applyBtn.setMinWidth(100.0); 
        applyBtn.getStyleClass().add("btn-secondary");

        applyBtn.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Tournament Application");
            dialog.setHeaderText("Create a Team for " + tName);
            dialog.setContentText("Please enter your team name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(teamName -> {
                if (teamName.trim().isEmpty()) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Team name cannot be empty!");
                    err.showAndWait();
                    return;
                }
                
                new Thread(() -> {
                    DbStatus status = db.insertTeam(currentUser, t, teamName.trim());
                    
                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) {
                            Alert success = new Alert(Alert.AlertType.INFORMATION, "Successfully applied to the tournament! Your team '" + teamName + "' has been created.");
                            success.showAndWait();
                        } else {
                            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to apply. You might already be in a team for this tournament.");
                            err.showAndWait();
                        }
                    });
                }).start();
            });
        });

        row.getChildren().addAll(tLabel, applyBtn);
        return row;
    }

    private HBox createNotificationRow(Notification notif, DateTimeFormatter formatter) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F4F7FE; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #E2E8F0;");
        row.setPadding(new Insets(10, 15, 10, 15));
        
        Label starIndicator = new Label("★");
        starIndicator.setStyle("-fx-text-fill: #FF9120; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        VBox textContainer = new VBox(5);
        
        String dateStr = (notif.getDate() != null) ? notif.getDate().format(formatter) : "";
        Label titleLabel = new Label(notif.getTitle() + "  |  " + dateStr);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B3674; -fx-font-size: 13px;");
        
        Label msgLabel = new Label(notif.getMessage());
        msgLabel.setWrapText(true); 
        msgLabel.setStyle("-fx-text-fill: #A3AED0; -fx-font-size: 12px;");
        
        textContainer.getChildren().addAll(titleLabel, msgLabel);
        
        row.getChildren().addAll(starIndicator, textContainer);
        return row;
    }
}