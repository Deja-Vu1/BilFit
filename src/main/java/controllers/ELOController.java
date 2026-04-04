package controllers;

import java.util.ArrayList;
import java.util.Optional;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import managers.DuelloManager;
import managers.ReservationManager;
import managers.SessionManager;
import models.Duello;
import models.Reservation;
import models.Student;

public class ELOController {

    @FXML private VBox reservationsContainer;
    @FXML private VBox myDuellosContainer;
    @FXML private VBox duellosContainer;
    @FXML private VBox incomingRequestsContainer;
    @FXML private ComboBox<String> sportTypeComboBox;

    private DuelloManager duelloManager = new DuelloManager(Database.getInstance());
    private ReservationManager resManager = new ReservationManager(Database.getInstance());
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        if (sportTypeComboBox != null) {
            sportTypeComboBox.getItems().addAll("BASKETBALL", "TENNIS", "TABLE TENNIS", "SQUASH", "VOLLEYBALL", "FOOTBALL");
            sportTypeComboBox.getSelectionModel().selectFirst();
        }
        loadEloAndDuelloData();
    }

    private void loadEloAndDuelloData() {
        Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String selectedSport = sportTypeComboBox != null && sportTypeComboBox.getValue() != null ? sportTypeComboBox.getValue() : "BASKETBALL";

        new Thread(() -> {
            try {
                ArrayList<Reservation> dbReservations = resManager.getUserReservations(currentUser);
                ArrayList<Duello> myDuellos = duelloManager.getUserDuellos(currentUser);
                ArrayList<Duello> suitableDuellos = duelloManager.findOpponentForMatch(currentUser, selectedSport);

                ArrayList<Reservation> validReservations = new ArrayList<>();
                if (dbReservations != null) {
                    for (Reservation res : dbReservations) {
                        if (!res.isCancelled()) {
                            boolean isAlreadyDuello = false;
                            for (Duello d : myDuellos) {
                                if (d.getReservationId().equals(res.getReservationId())) {
                                    isAlreadyDuello = true;
                                    break;
                                }
                            }
                            if (!isAlreadyDuello) {
                                validReservations.add(res);
                            }
                        }
                    }
                }

                ArrayList<HBox> requestRows = new ArrayList<>();
                if (myDuellos != null) {
                    for (Duello d : myDuellos) {
                        if (!d.isMatched() && !d.isCancelled()) {
                            ArrayList<Student> requesters = duelloManager.getPendingRequestsForDuello(d.getReservationId());
                            if (requesters != null) {
                                for (Student requester : requesters) {
                                    requestRows.add(createIncomingRequestRow(d, requester));
                                }
                            }
                        }
                    }
                }

                Platform.runLater(() -> {
                    if (reservationsContainer != null) {
                        reservationsContainer.getChildren().clear();
                        if (!validReservations.isEmpty()) {
                            for (Reservation res : validReservations) {
                                reservationsContainer.getChildren().add(createReservationRow(res));
                            }
                        } else {
                            Label emptyLabel = new Label("Aktif normal rezervasyonunuz bulunmamaktadır.");
                            emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            reservationsContainer.getChildren().add(emptyLabel);
                        }
                    }

                    if (myDuellosContainer != null) {
                        myDuellosContainer.getChildren().clear();
                        if (myDuellos != null && !myDuellos.isEmpty()) {
                            for (Duello d : myDuellos) {
                                myDuellosContainer.getChildren().add(createMyDuelloRow(d));
                            }
                        } else {
                            Label emptyMyDuellosLabel = new Label("Henüz oluşturduğunuz veya katıldığınız bir düello yok.");
                            emptyMyDuellosLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            myDuellosContainer.getChildren().add(emptyMyDuellosLabel);
                        }
                    }

                    if (incomingRequestsContainer != null) {
                        incomingRequestsContainer.getChildren().clear();
                        if (!requestRows.isEmpty()) {
                            incomingRequestsContainer.getChildren().addAll(requestRows);
                        } else {
                            Label emptyReqLabel = new Label("Şu an bekleyen gelen isteğiniz yok.");
                            emptyReqLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            incomingRequestsContainer.getChildren().add(emptyReqLabel);
                        }
                    }

                    if (duellosContainer != null) {
                        duellosContainer.getChildren().clear();
                        if (suitableDuellos != null && !suitableDuellos.isEmpty()) {
                            for (Duello d : suitableDuellos) {
                                duellosContainer.getChildren().add(createAvailableDuelloRow(d));
                            }
                        } else {
                            Label emptyDuelloLabel = new Label("Şu anda seviyenize uygun açık bir düello bulunmamaktadır.");
                            emptyDuelloLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold; -fx-font-size: 13px;");
                            duellosContainer.getChildren().add(emptyDuelloLabel);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleFindDuello(ActionEvent event) {
        String selectedSport = sportTypeComboBox.getValue();
        if (selectedSport != null) {
            loadEloAndDuelloData();
        }
    }

    private HBox createReservationRow(Reservation res) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        row.setPadding(new Insets(10, 20, 10, 10));

        String facilityName = res.getFacility() != null ? res.getFacility().getName() : "Saha";
        String resText = res.getFacility().getCampusLocation() + "   |   " + facilityName + "   |   " + res.getDate() + "   |   " + res.getTimeSlot();

        Label infoLabel = new Label(resText);
        infoLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button createBtn = new Button("Create A Duello");
        createBtn.setPrefHeight(35.0);
        createBtn.setPrefWidth(130.0);
        createBtn.getStyleClass().add("btn-success");
        createBtn.setOnAction(e -> handleCreateSpecificDuello(res, createBtn));

        row.getChildren().addAll(infoLabel, spacer, createBtn);
        return row;
    }

    private HBox createMyDuelloRow(Duello duello) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #4318FF; -fx-border-radius: 15; -fx-background-radius: 15;");
        row.setPadding(new Insets(10, 20, 10, 20));

        VBox infoBox = new VBox(5);
        
        String facilityName = duello.getFacility() != null ? duello.getFacility().getName() : "Saha";
        String loc = duello.getFacility() != null ? duello.getFacility().getCampusLocation() : "Kampüs";
        String mainText = loc + "   |   " + facilityName + "   |   " + duello.getDate() + " " + duello.getTimeSlot();
        String subText = "Access Code: " + duello.getAccessCode() + "   |   Status: " + (duello.isMatched() ? "Match Ready" : "Waiting for Opponent");

        Label mainLabel = new Label(mainText);
        mainLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(mainLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button actionBtn = new Button();
        actionBtn.setPrefHeight(35.0);
        actionBtn.setPrefWidth(110.0);

        if (duello.isMatched()) {
            actionBtn.setText("Matched");
            actionBtn.setStyle("-fx-background-color: #05CD99; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10;");
            actionBtn.setDisable(true);
        } else {
            actionBtn.setText("Cancel Duello");
            actionBtn.setStyle("-fx-background-color: #E2ECF6; -fx-text-fill: #4318FF; -fx-font-weight: bold; -fx-background-radius: 10;");
            actionBtn.setOnAction(e -> handleCancelSpecificDuello(duello, actionBtn));
        }

        row.getChildren().addAll(infoBox, spacer, actionBtn);
        return row;
    }

    private HBox createIncomingRequestRow(Duello duello, Student requester) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FEF4F4; -fx-border-color: #FFB547; -fx-border-radius: 15; -fx-background-radius: 15;");
        row.setPadding(new Insets(10, 20, 10, 20));

        VBox infoBox = new VBox(5);
        String mainText = "Requester: " + requester.getFullName() + " (ELO: " + requester.getEloPoint() + ")";
        String facilityName = duello.getFacility() != null ? duello.getFacility().getName() : "Saha";
        String subText = "Match: " + facilityName + " | " + duello.getDate() + " " + duello.getTimeSlot();

        Label mainLabel = new Label(mainText);
        mainLabel.setStyle("-fx-text-fill: #2b3674; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(mainLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button acceptBtn = new Button("Accept");
        acceptBtn.setPrefHeight(30.0);
        acceptBtn.setPrefWidth(80.0);
        acceptBtn.setStyle("-fx-background-color: #05CD99; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        acceptBtn.setOnAction(e -> handleAcceptRequest(duello, requester, acceptBtn));

        Button rejectBtn = new Button("Reject");
        rejectBtn.setPrefHeight(30.0);
        rejectBtn.setPrefWidth(80.0);
        rejectBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> handleRejectRequest(duello, requester, rejectBtn));

        HBox btnBox = new HBox(10, acceptBtn, rejectBtn);
        row.getChildren().addAll(infoBox, spacer, btnBox);
        return row;
    }

    private HBox createAvailableDuelloRow(Duello duello) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FCE8E8; -fx-border-color: #D93025; -fx-border-radius: 15; -fx-background-radius: 15;");
        row.setPadding(new Insets(10, 20, 10, 20));

        VBox infoBox = new VBox(5);
        
        String facilityName = duello.getFacility() != null ? duello.getFacility().getName() : "Saha";
        String loc = duello.getFacility() != null ? duello.getFacility().getCampusLocation() : "Kampüs";
        String mainText = loc + "   |   " + facilityName + "   |   Max " + (duello.getFacility() != null ? duello.getFacility().getCapacity() : "0") + " player   |   " + duello.getDate();
        String subText = "Empty Slots: " + duello.getEmptySlots() + "   |   Skill: " + duello.getRequiredSkillLevel();

        Label mainLabel = new Label(mainText);
        mainLabel.setStyle("-fx-text-fill: #D93025; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label subLabel = new Label(subText);
        subLabel.setStyle("-fx-text-fill: #D93025; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(mainLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label iconLabel = new Label("🔥");
        iconLabel.setStyle("-fx-font-size: 24;");
        HBox.setMargin(iconLabel, new Insets(0, 15, 0, 0));

        Button requestBtn = new Button("Request");
        requestBtn.setPrefHeight(35.0);
        requestBtn.setPrefWidth(100.0);
        requestBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 10;");
        requestBtn.setOnAction(e -> handleRequestSpecificDuello(duello, requestBtn));

        row.getChildren().addAll(infoBox, spacer, iconLabel, requestBtn);
        return row;
    }

    private void handleAcceptRequest(Duello duello, Student requester, Button btn) {
        if (isProcessing) return;
        isProcessing = true;
        String originalText = btn.getText();
        btn.setDisable(true);
        btn.setText("...");

        new Thread(() -> {
            DbStatus status = duelloManager.acceptDuelloRequest(duello, requester);
            Platform.runLater(() -> {
                isProcessing = false;
                if (status == DbStatus.SUCCESS) {
                    showAlert(Alert.AlertType.INFORMATION, "Başarılı", "İsteği kabul ettiniz. Eşleşme tamamlandı!");
                    loadEloAndDuelloData();
                } else {
                    btn.setDisable(false);
                    btn.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Hata", "İstek kabul edilemedi.");
                }
            });
        }).start();
    }

    private void handleRejectRequest(Duello duello, Student requester, Button btn) {
        if (isProcessing) return;
        isProcessing = true;
        String originalText = btn.getText();
        btn.setDisable(true);
        btn.setText("...");

        new Thread(() -> {
            DbStatus status = duelloManager.declineDuelloRequest(duello, requester);
            Platform.runLater(() -> {
                isProcessing = false;
                if (status == DbStatus.SUCCESS) {
                    loadEloAndDuelloData();
                } else {
                    btn.setDisable(false);
                    btn.setText(originalText);
                    showAlert(Alert.AlertType.ERROR, "Hata", "İstek reddedilemedi.");
                }
            });
        }).start();
    }

    private void handleCreateSpecificDuello(Reservation targetRes, Button clickedButton) {
        if (isProcessing) return;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Creating...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                Duello newDuello = new Duello(targetRes.getReservationId(), targetRes.getFacility(), targetRes.getDate(), targetRes.getTimeSlot(), "CODE123", "Mid-Level", 1);
                
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    status = duelloManager.createDuello(newDuello, currentUser);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (finalStatus == DbStatus.SUCCESS) {
                        showAlert(Alert.AlertType.INFORMATION, "Başarılı", "Rezervasyonunuz başarıyla bir Düello'ya dönüştürüldü!");
                        loadEloAndDuelloData();
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Hata", "Düello oluşturulamadı. Lütfen tekrar deneyin.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    private void handleCancelSpecificDuello(Duello duello, Button clickedButton) {
        if (isProcessing) return;
        
        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Canceling...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                DbStatus status = DbStatus.QUERY_ERROR;
                
                try {
                    status = duelloManager.cancelDuello(duello, currentUser);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (finalStatus == DbStatus.SUCCESS) {
                        showAlert(Alert.AlertType.INFORMATION, "İptal Edildi", "Düello başarıyla iptal edildi ve normal rezervasyona dönüştürüldü.");
                        loadEloAndDuelloData();
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Hata", "Düello iptal edilemedi veya yetkiniz yok.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    private void handleRequestSpecificDuello(Duello targetDuello, Button clickedButton) {
        if (isProcessing) return;

        isProcessing = true;
        String originalText = clickedButton.getText();
        clickedButton.setDisable(true);
        clickedButton.setText("Sending...");

        new Thread(() -> {
            try {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                
                DbStatus status = DbStatus.QUERY_ERROR;
                try {
                    status = duelloManager.requestToJoinDuello(targetDuello, currentUser);
                } catch (Exception ex) {}

                final DbStatus finalStatus = status;

                Platform.runLater(() -> {
                    isProcessing = false;
                    if (finalStatus == DbStatus.SUCCESS) { 
                        clickedButton.setText("Requested");
                        showAlert(Alert.AlertType.INFORMATION, "İstek Gönderildi", "Bu maça katılma isteğiniz başarıyla kurucuya iletildi.");
                    } else {
                        clickedButton.setDisable(false);
                        clickedButton.setText(originalText);
                        showAlert(Alert.AlertType.ERROR, "Hata", "İstek gönderilemedi.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing = false;
                    clickedButton.setDisable(false);
                    clickedButton.setText(originalText);
                });
            }
        }).start();
    }

    @FXML
    public void handleApplyWithCode(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Özel Düelloya Katıl");
        dialog.setHeaderText("Düello Kodunu Giriniz");
        dialog.setContentText("Kod:");
        try { dialog.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception e) {}

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(code -> {
            if (code.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Uyarı", "Kod alanı boş bırakılamaz.");
                return;
            }
            new Thread(() -> {
                Student currentUser = (Student) SessionManager.getInstance().getCurrentUser();
                DbStatus status = DbStatus.QUERY_ERROR;
                
                try { 
                    status = duelloManager.joinDuelloWithCode(code.trim(), currentUser); 
                } catch (Exception ex) { ex.printStackTrace(); }
                
                final DbStatus finalStatus = status;
                Platform.runLater(() -> {
                    if (finalStatus == DbStatus.SUCCESS) { 
                         showAlert(Alert.AlertType.INFORMATION, "İşlem Başarılı", "Koda sahip düelloya başarıyla katıldınız!");
                         loadEloAndDuelloData();
                    } else if (finalStatus == DbStatus.DATA_NOT_FOUND) {
                         showAlert(Alert.AlertType.ERROR, "Hata", "Girdiğiniz koda ait açık bir düello bulunamadı.");
                    } else {
                         showAlert(Alert.AlertType.ERROR, "Hata", "Geçersiz kod, ELO uyumsuzluğu veya kendi açtığınız maça girmeye çalışıyorsunuz.");
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
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("/views/dashboard/bilfit-exact.css").toExternalForm()); } catch (Exception e) {}
        if (reservationsContainer != null && reservationsContainer.getScene() != null) {
            alert.initOwner(reservationsContainer.getScene().getWindow());
        }
        alert.showAndWait();
    }
}