package controllers;

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

import java.util.Optional;

public class FriendsController {

    @FXML private TextField searchField;
    @FXML private TableView<Student> friendsTable;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colStatus;
    @FXML private TableColumn<Student, String> colELO;
    @FXML private TableColumn<Student, Void> colActions;

    private ObservableList<Student> friendsObservableList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadFriendsData();
        setupSearchFilter();
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getReliabilityScore() + "%"));
        colELO.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getEloPoint())));

        colActions.setCellFactory(param -> new TableCell<Student, Void>() {
            private final Button removeBtn = new Button("Remove");

            {
                removeBtn.getStyleClass().add("btn-danger");
                removeBtn.setOnAction(event -> {
                    Student friend = getTableView().getItems().get(getIndex());
                    handleRemoveFriend(friend);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actionBox = new HBox(removeBtn);
                    actionBox.setSpacing(10);
                    setGraphic(actionBox);
                }
            }
        });
    }

    private void loadFriendsData() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        new Thread(() -> {
            Student updatedStudent = Database.getInstance().fillFriendsByEmail(currentUser);
            
            Platform.runLater(() -> {
                if (updatedStudent != null && updatedStudent.getFriends() != null) {
                    SessionManager.getInstance().setCurrentFriends(updatedStudent.getFriends());
                    friendsObservableList.setAll(updatedStudent.getFriends());
                    friendsTable.setItems(friendsObservableList);
                }
            });
        }).start();
    }

    private void setupSearchFilter() {
        FilteredList<Student> filteredData = new FilteredList<>(friendsObservableList, b -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(friend -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (friend.getFullName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (friend.getStudentId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        
        friendsTable.setItems(filteredData);
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
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Friend request sent successfully!");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Failed", "Could not send request. Check email or existing requests.");
                        }
                    });
                }).start();
            }
        });
    }

    private void handleRemoveFriend(Student friend) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || friend == null) return;
        
        friendsObservableList.remove(friend);
        SessionManager.getInstance().getCurrentFriends().remove(friend);
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