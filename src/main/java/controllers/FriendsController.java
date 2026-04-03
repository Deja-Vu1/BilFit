package controllers;

import java.util.Optional;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.stage.StageStyle;
import managers.SessionManager;
import models.Student;

public class FriendsController {

    @FXML private TextField searchField;
    
    @FXML private TableView<Student> friendsTable;
    @FXML private TableColumn<Student, String> colFriendName;
    @FXML private TableColumn<Student, String> colFriendStatus;
    @FXML private TableColumn<Student, String> colFriendELO;
    @FXML private TableColumn<Student, Void> colFriendActions;

    @FXML private TableView<Student> incomingTable;
    @FXML private TableColumn<Student, String> colIncomingName;
    @FXML private TableColumn<Student, String> colIncomingStatus;
    @FXML private TableColumn<Student, String> colIncomingELO;
    @FXML private TableColumn<Student, Void> colIncomingActions;

    @FXML private TableView<Student> outgoingTable;
    @FXML private TableColumn<Student, String> colOutgoingName;
    @FXML private TableColumn<Student, String> colOutgoingStatus;
    @FXML private TableColumn<Student, String> colOutgoingELO;
    @FXML private TableColumn<Student, Void> colOutgoingActions;

    private ObservableList<Student> friendsList = FXCollections.observableArrayList();
    private ObservableList<Student> incomingList = FXCollections.observableArrayList();
    private ObservableList<Student> outgoingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns(colFriendName, colFriendStatus, colFriendELO);
        setupColumns(colIncomingName, colIncomingStatus, colIncomingELO);
        setupColumns(colOutgoingName, colOutgoingStatus, colOutgoingELO);
        
        setupActionColumns();
        setupSearchFilter();
        loadAllData();
    }

    private void setupColumns(TableColumn<Student, String> name, TableColumn<Student, String> status, TableColumn<Student, String> elo) {
        name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReliabilityScore() + "%"));
        elo.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getEloPoint())));
    }

    private void setupActionColumns() {
        colFriendActions.setCellFactory(param -> new TableCell<Student, Void>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.getStyleClass().add("btn-danger");
                removeBtn.setOnAction(e -> handleNetworkAction(getTableView().getItems().get(getIndex()), "remove"));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        colIncomingActions.setCellFactory(param -> new TableCell<Student, Void>() {
            private final Button acceptBtn = new Button("Accept");
            private final Button declineBtn = new Button("Decline");
            {
                acceptBtn.getStyleClass().add("btn-success");
                declineBtn.getStyleClass().add("btn-danger");
                acceptBtn.setOnAction(e -> handleNetworkAction(getTableView().getItems().get(getIndex()), "accept"));
                declineBtn.setOnAction(e -> handleNetworkAction(getTableView().getItems().get(getIndex()), "decline"));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    HBox box = new HBox(10, acceptBtn, declineBtn);
                    setGraphic(box);
                }
            }
        });

        colOutgoingActions.setCellFactory(param -> new TableCell<Student, Void>() {
            private final Button cancelBtn = new Button("Cancel");
            {
                cancelBtn.getStyleClass().add("btn-danger");
                cancelBtn.setOnAction(e -> handleNetworkAction(getTableView().getItems().get(getIndex()), "cancel"));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : cancelBtn);
            }
        });
    }

    private void loadAllData() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        new Thread(() -> {
            Database db = Database.getInstance();
            Student fStudent = db.fillFriendsByEmail(currentUser);
            Student iStudent = db.fillIncomingFriendRequests(currentUser);
            Student oStudent = db.fillOutgoingFriendRequests(currentUser);
            
            Platform.runLater(() -> {
                if (fStudent != null && fStudent.getFriends() != null) {
                    SessionManager.getInstance().setCurrentFriends(fStudent.getFriends());
                    friendsList.setAll(fStudent.getFriends());
                }
                if (iStudent != null && iStudent.getIncomingFriendRequests() != null) {
                    SessionManager.getInstance().setIncomingFriendRequests(iStudent.getIncomingFriendRequests());
                    incomingList.setAll(iStudent.getIncomingFriendRequests());
                }
                if (oStudent != null && oStudent.getOutgoingFriendRequests() != null) {
                    SessionManager.getInstance().setOutgoingFriendRequests(oStudent.getOutgoingFriendRequests());
                    outgoingList.setAll(oStudent.getOutgoingFriendRequests());
                }
                
                friendsTable.setItems(friendsList);
                incomingTable.setItems(incomingList);
                outgoingTable.setItems(outgoingList);
            });
        }).start();
    }

    private void setupSearchFilter() {
        FilteredList<Student> filteredFriends = new FilteredList<>(friendsList, b -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredFriends.setPredicate(friend -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lower = newValue.toLowerCase();
                return friend.getFullName().toLowerCase().contains(lower) || friend.getStudentId().toLowerCase().contains(lower);
            });
        });
        friendsTable.setItems(filteredFriends);
    }

    @FXML
    public void handleRefresh() {
        loadAllData();
    }

    @FXML
    public void handleAddFriend() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText("Send Friend Request");
        dialog.setContentText("Enter Friend's Bilkent Email:");
        try { dialog.initStyle(StageStyle.UNDECORATED); } catch (Exception ignored) {}
        try { dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception ignored) {}

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
            if (currentUser != null && !email.trim().isEmpty()) {
                new Thread(() -> {
                    DbStatus status = Database.getInstance().insertFriendRequest(currentUser.getBilkentEmail(), email.trim());
                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Friend request sent!");
                            loadAllData();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Failed", "Could not send request.");
                        }
                    });
                }).start();
            }
        });
    }

    private void handleNetworkAction(Student targetStudent, String actionType) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || targetStudent == null) return;

        new Thread(() -> {
            Database db = Database.getInstance();
            DbStatus status;

            if (actionType.equals("accept")) {
                status = db.acceptFriendRequest(targetStudent.getBilkentEmail(), currentUser.getBilkentEmail());
            } else {
                status = db.deleteFriend(currentUser.getBilkentEmail(), targetStudent.getBilkentEmail());
            }

            Platform.runLater(() -> {
                if (status == DbStatus.SUCCESS) {
                    loadAllData();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Operation failed.");
                }
            });
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try { alert.initStyle(StageStyle.UNDECORATED); } catch (Exception ignored) {}
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception ignored) {}
        alert.showAndWait();
    }
}