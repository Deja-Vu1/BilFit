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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
    @FXML private VBox addPlayerContainer;
    @FXML private TextField newPlayerEmailField;
    @FXML private Button addPlayerButton;
    @FXML private VBox membersContainer;
    
    
    @FXML private VBox friendsCheckboxContainer;
    @FXML private Button addSelectedFriendsButton;

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

        
        if (isCaptain) {
            addPlayerContainer.setVisible(true);
            addPlayerContainer.setManaged(true);
            loadFriendsList(); 
        } else {
            addPlayerContainer.setVisible(false);
            addPlayerContainer.setManaged(false);
        }

        
        loadMembers();
    }

    private void loadFriendsList() {
        if (friendsCheckboxContainer == null) return;
        
        friendsCheckboxContainer.getChildren().clear();
        friendsCheckboxContainer.getChildren().add(new Label("Loading friends..."));

        new Thread(() -> {
            try {
                
                Database.getInstance().fillFriendsByEmail(currentUser);
                List<Student> friends = currentUser.getFriends();

                Platform.runLater(() -> {
                    friendsCheckboxContainer.getChildren().clear();

                    if (friends == null || friends.isEmpty()) {
                        friendsCheckboxContainer.getChildren().add(new Label("No friends found."));
                        return;
                    }

                    boolean hasAvailableFriends = false;

                    for (Student friend : friends) {
                        
                        boolean alreadyInTeam = false;
                        if (team.getMembers() != null) {
                            for (Student member : team.getMembers()) {
                                if (member.getBilkentEmail().equals(friend.getBilkentEmail())) {
                                    alreadyInTeam = true;
                                    break;
                                }
                            }
                        }

                        if (!alreadyInTeam) {
                            hasAvailableFriends = true;
                            CheckBox cb = new CheckBox(friend.getFullName() + " (" + friend.getBilkentEmail() + ")");
                            cb.setUserData(friend); 
                            cb.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-padding: 5;");
                            friendsCheckboxContainer.getChildren().add(cb);
                        }
                    }

                    if (!hasAvailableFriends) {
                        friendsCheckboxContainer.getChildren().add(new Label("All friends are already in the team."));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    friendsCheckboxContainer.getChildren().clear();
                    friendsCheckboxContainer.getChildren().add(new Label("Error loading friends."));
                });
            }
        }).start();
    }

    @FXML
    public void handleAddSelectedFriends(ActionEvent event) {
        if (friendsCheckboxContainer == null) return;

        List<Student> selectedFriends = new ArrayList<>();

        
        for (Node node : friendsCheckboxContainer.getChildren()) {
            if (node instanceof CheckBox) {
                CheckBox cb = (CheckBox) node;
                if (cb.isSelected() && cb.getUserData() != null) {
                    selectedFriends.add((Student) cb.getUserData());
                }
            }
        }

        if (selectedFriends.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select at least one friend to invite.");
            return;
        }

        if (addSelectedFriendsButton != null) {
            addSelectedFriendsButton.setText("Sending...");
            addSelectedFriendsButton.setDisable(true);
        }

        new Thread(() -> {
            boolean allSuccess = true;
            boolean someSuccess = false;

            for (Student friend : selectedFriends) {
                DbStatus status = tournamentManager.sendTeamInvite(team.getTeamId(), friend);
                if (status == DbStatus.SUCCESS) {
                    someSuccess = true;
                } else {
                    allSuccess = false;
                }
            }

            final boolean finalAllSuccess = allSuccess;
            final boolean finalSomeSuccess = someSuccess;

            Platform.runLater(() -> {
                if (addSelectedFriendsButton != null) {
                    addSelectedFriendsButton.setText("Add Selected Friends");
                    addSelectedFriendsButton.setDisable(false);
                }

                if (finalAllSuccess) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Invites sent successfully to all selected friends.");
                    loadFriendsList(); 
                } else if (finalSomeSuccess) {
                    showAlert(Alert.AlertType.WARNING, "Partial Success", "Some invites were sent. Others may already have pending invites.");
                    loadFriendsList();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not send invites. Users might already be invited or in a team.");
                }
            });
        }).start();
    }

    private void loadMembers() {
        membersContainer.getChildren().clear();
        
        Label loadingLabel = new Label("Loading members...");
        membersContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            try {
                
                List<Student> fetchedMembers = teamManager.getTeamMembers(team.getTeamId());
                
                
                team.setMembers(fetchedMembers);

                Platform.runLater(() -> {
                    membersContainer.getChildren().clear();

                    if (fetchedMembers == null || fetchedMembers.isEmpty()) {
                        membersContainer.getChildren().add(new Label("No members found."));
                        return;
                    }

                   
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
                    
                    loadMembers(); 
                    loadFriendsList();
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