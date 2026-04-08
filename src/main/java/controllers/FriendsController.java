package controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import managers.RecommendationManager;
import managers.SessionManager;
import models.Student;

public class FriendsController {

    @FXML private TextField searchField;
    
    @FXML private TableView<Student> friendsTable;
    @FXML private TableColumn<Student, Student> colFriendPic;
    @FXML private TableColumn<Student, String> colFriendName;
    @FXML private TableColumn<Student, String> colFriendStatus;
    @FXML private TableColumn<Student, String> colFriendELO;
    @FXML private TableColumn<Student, String> colFriendLastSeen;
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
        
        setupSpecificFriendColumns();
        setupActionColumns();
        setupSearchFilter();
        loadAllData();
    }

    private void setupColumns(TableColumn<Student, String> name, TableColumn<Student, String> status, TableColumn<Student, String> elo) {
        name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReliabilityScore() + "%"));
        elo.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getEloPoint())));
    }

    private void setupSpecificFriendColumns() {
        
        colFriendLastSeen.setCellValueFactory(data -> {
            LocalDateTime lastSeen = data.getValue().getLastSeen();
            if (lastSeen == null) {
                return new SimpleStringProperty("Offline / Hidden");
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            return new SimpleStringProperty(lastSeen.format(formatter));
        });

        colFriendPic.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        colFriendPic.setCellFactory(param -> new TableCell<Student, Student>() {
            private final Circle circle = new Circle(18); 
            
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);
                if (empty || student == null) {
                    setGraphic(null);
                } else {
                    String url = student.getProfilePictureUrl();
                    circle.setStroke(Color.web("#4318FF"));
                    circle.setStrokeWidth(1.5);
                    
                    if (url != null && !url.trim().isEmpty()) {
                        String noCacheUrl = url + (url.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
                        Image image = new Image(noCacheUrl, true);
                        circle.setFill(Color.web("#E2E8F0")); 
                        
                        image.progressProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal.doubleValue() == 1.0 && !image.isError()) {
                                Platform.runLater(() -> circle.setFill(new ImagePattern(image)));
                            }
                        });
                        
                        image.errorProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal) {
                                Platform.runLater(() -> circle.setFill(Color.web("#E2E8F0")));
                            }
                        });

                    } else {
                        circle.setFill(Color.web("#E2E8F0")); 
                    }
                    
                    setGraphic(circle);
                    setAlignment(Pos.CENTER);
                }
            }
        });
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
            });
        }).start();
    }

    private void setupSearchFilter() {
        FilteredList<Student> filteredFriends = new FilteredList<>(friendsList, b -> true);
        FilteredList<Student> filteredIncoming = new FilteredList<>(incomingList, b -> true);
        FilteredList<Student> filteredOutgoing = new FilteredList<>(outgoingList, b -> true);

        friendsTable.setItems(filteredFriends);
        incomingTable.setItems(filteredIncoming);
        outgoingTable.setItems(filteredOutgoing);

        searchField.setOnAction(event -> {
            String filterText = searchField.getText();
            
            Predicate<Student> searchPredicate = friend -> {
                if (filterText == null || filterText.isEmpty()) return true;
                
                java.util.Locale trLocale = new java.util.Locale("tr", "TR");
                String lowerCaseFilter = filterText.toLowerCase(trLocale);
                
                String friendName = friend.getFullName() != null ? friend.getFullName().toLowerCase(trLocale) : "";
                String friendId = friend.getStudentId() != null ? friend.getStudentId().toLowerCase(trLocale) : "";
                
                return friendName.contains(lowerCaseFilter) || friendId.contains(lowerCaseFilter);
            };

            filteredFriends.setPredicate(searchPredicate);
            filteredIncoming.setPredicate(searchPredicate);
            filteredOutgoing.setPredicate(searchPredicate);
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                Predicate<Student> clearPredicate = b -> true;
                filteredFriends.setPredicate(clearPredicate);
                filteredIncoming.setPredicate(clearPredicate);
                filteredOutgoing.setPredicate(clearPredicate);
            }
        });
    }

    @FXML
    public void handleRefresh() {
        loadAllData();
        showAlert(Alert.AlertType.INFORMATION, "Refreshed", "Friend lists have been refreshed successfully.");
    }

    @FXML
    public void handleAddFriend() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
        layout.setPrefWidth(450);
        layout.setPrefHeight(550);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        layout.setEffect(shadow);

        Label titleLabel = new Label("Add Friend");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");

        HBox manualAddBox = new HBox(10);
        manualAddBox.setAlignment(Pos.CENTER);
        TextField emailField = new TextField();
        emailField.setPromptText("Enter Bilkent Email");
        emailField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8; -fx-pref-width: 250;");
        
        Button sendBtn = new Button("Send Request");
        sendBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 15 8 15;");
        sendBtn.setOnAction(e -> {
            String targetEmail = emailField.getText().trim();
            if (!targetEmail.isEmpty()) {
                sendRequestAction(targetEmail, sendBtn);
            }
        });
        manualAddBox.getChildren().addAll(emailField, sendBtn);

        Label recLabel = new Label("Recommended Friends");
        recLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");
        VBox.setMargin(recLabel, new Insets(10, 0, 0, 0));

        VBox recListContainer = new VBox(10);
        recListContainer.setAlignment(Pos.TOP_CENTER);
        Label loadingLabel = new Label("Loading recommendations...");
        loadingLabel.setStyle("-fx-text-fill: #A0AEC0;");
        recListContainer.getChildren().add(loadingLabel);

        ScrollPane scrollPane = new ScrollPane(recListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 100; -fx-padding: 8;");
        closeBtn.setOnAction(e -> dialogStage.close());

        layout.getChildren().addAll(titleLabel, manualAddBox, recLabel, scrollPane, closeBtn);
        
        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);
        dialogStage.centerOnScreen();

        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            new Thread(() -> {
                RecommendationManager recManager = new RecommendationManager(Database.getInstance());
                List<Student> recommendations = recManager.getRecommendations(currentUser);
                
                Platform.runLater(() -> {
                    recListContainer.getChildren().clear();
                    if (recommendations == null || recommendations.isEmpty()) {
                        Label emptyLabel = new Label("No recommendations found at the moment.");
                        emptyLabel.setStyle("-fx-text-fill: #A0AEC0;");
                        recListContainer.getChildren().add(emptyLabel);
                    } else {
                        for (Student recStudent : recommendations) {
                            HBox row = new HBox(10);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-padding: 10;");
                            
                            VBox info = new VBox(3);
                            Label nameL = new Label(recStudent.getFullName());
                            nameL.setStyle("-fx-font-weight: bold; -fx-text-fill: #2b3674; -fx-font-size: 14px;");
                            Label detailL = new Label(recStudent.getBilkentEmail() + " | ELO: " + recStudent.getEloPoint());
                            detailL.setStyle("-fx-text-fill: #A0AEC0; -fx-font-size: 12px;");
                            info.getChildren().addAll(nameL, detailL);
                            
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            
                            Button addBtn = new Button("Add");
                            addBtn.setStyle("-fx-background-color: #05CD99; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 5 15 5 15;");
                            addBtn.setOnAction(e -> sendRequestAction(recStudent.getBilkentEmail(), addBtn));
                            
                            row.getChildren().addAll(info, spacer, addBtn);
                            recListContainer.getChildren().add(row);
                        }
                    }
                });
            }).start();
        }

        dialogStage.showAndWait();
    }

    private void sendRequestAction(String targetEmail, Button actionBtn) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String origText = actionBtn.getText();
        actionBtn.setDisable(true);
        actionBtn.setText("...");

        new Thread(() -> {
            DbStatus status = Database.getInstance().insertFriendRequest(currentUser.getBilkentEmail(), targetEmail);
            Platform.runLater(() -> {
                if (status == DbStatus.SUCCESS) {
                    actionBtn.setText("Sent");
                    actionBtn.setStyle("-fx-background-color: #A0AEC0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
                    loadAllData();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Friend request sent to " + targetEmail);
                } else {
                    actionBtn.setDisable(false);
                    actionBtn.setText(origText);
                    showAlert(Alert.AlertType.ERROR, "Failed", "Could not send request. Check if user exists or request is already pending.");
                }
            });
        }).start();
    }

    private void handleNetworkAction(Student targetStudent, String actionType) {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || targetStudent == null) return;

        boolean isConfirmed = showConfirmation("Confirm Action", "Are you sure you want to " + actionType + " " + targetStudent.getFullName() + "?");
        if (!isConfirmed) {
            return;
        }

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
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Action completed successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Operation failed.");
                }
            });
        }).start();
    }

    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try { alert.initStyle(StageStyle.UNDECORATED); } catch (Exception ignored) {}
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception ignored) {}
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
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