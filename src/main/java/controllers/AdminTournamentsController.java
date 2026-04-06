package controllers;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.Database;
import database.DbStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import managers.TournamentManager;
import models.Match;
import models.SportType;
import models.Team;
import models.Tournament;

public class AdminTournamentsController {

    @FXML private VBox tournamentsContainer;
    
    private Map<CheckBox, Tournament> selectionMap = new HashMap<>();
    private Database db = Database.getInstance();
    private TournamentManager tManager = new TournamentManager(db);
    private boolean isProcessing = false;

    @FXML
    public void initialize() {
        loadTournaments();
    }

    private void loadTournaments() {
        if (tournamentsContainer == null) return;
        
        tournamentsContainer.getChildren().clear();
        selectionMap.clear();

        Label loadingLabel = new Label("Turnuvalar yükleniyor...");
        loadingLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
        tournamentsContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            List<Tournament> tournaments = tManager.getAllActiveTournaments(); 
            
            Platform.runLater(() -> {
                tournamentsContainer.getChildren().clear();
                
                if (tournaments == null || tournaments.isEmpty()) {
                    Label emptyLabel = new Label("Sistemde aktif turnuva bulunmuyor.");
                    emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                    tournamentsContainer.getChildren().add(emptyLabel);
                } else {
                    for (Tournament t : tournaments) {
                        tournamentsContainer.getChildren().add(createTournamentCard(t));
                    }
                }
            });
        }).start();
    }

    private HBox createTournamentCard(Tournament t) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 15; -fx-background-radius: 15; -fx-background-color: #FFFFFF;");
        card.setPadding(new Insets(15, 20, 15, 20));

        CheckBox selectBox = new CheckBox();
        selectBox.setStyle("-fx-cursor: hand;");
        selectionMap.put(selectBox, t);

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(t.getTournamentName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2B3674;");
        
        Label detailsLabel = new Label("Spor: " + t.getSportType().name() + " | Takım Başı Kapasite: " + t.getMaxPlayersPerTeam() + " | Kampüs: " + t.getCampusLocation());
        detailsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #A3AED0;");
        
        Label dateLabel = new Label(t.getStartDate() + " -> " + t.getEndDate());
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4318FF; -fx-font-weight: bold;");
        
        infoBox.getChildren().addAll(nameLabel, detailsLabel, dateLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("Düzenle");
        editBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 80;");
        editBtn.setOnAction(e -> handleEditTournament(t));

        Button scheduleBtn = new Button("Fikstür / Sonuç");
        scheduleBtn.setStyle("-fx-background-color: #FFF4E5; -fx-text-fill: #FF9120; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 120;");
        scheduleBtn.setOnAction(e -> handleSchedule(t));

        Button fixtureBtn = new Button("Eşleşme Oluştur");
        fixtureBtn.setStyle("-fx-background-color: #E6F4EA; -fx-text-fill: #1E8E3E; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 120;");
        fixtureBtn.setOnAction(e -> handleCreateFixture(t));

        card.getChildren().addAll(selectBox, infoBox, spacer, editBtn, scheduleBtn, fixtureBtn);
        return card;
    }

    @FXML
    public void handleCreateTournament(ActionEvent event) {
        Stage dialog = createDialogStage("Yeni Turnuva Oluştur");
        VBox layout = (VBox) dialog.getScene().getRoot();

        TextField nameField = new TextField(); 
        nameField.setPromptText("Turnuva Başlığı (Örn: Bahar Kupası)");
        nameField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        ComboBox<String> sportCombo = new ComboBox<>();
        for (SportType s : SportType.values()) {
            sportCombo.getItems().add(s.name());
        }
        sportCombo.setPromptText("Spor Türü Seçin");
        sportCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        ComboBox<String> campusCombo = new ComboBox<>();
        campusCombo.getItems().addAll("Main Campus", "East Campus");
        campusCombo.setPromptText("Kampüs Seçin");
        campusCombo.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-pref-height: 40; -fx-pref-width: 300;");

        DatePicker startDate = new DatePicker(); 
        startDate.setPromptText("Başlangıç Tarihi");
        startDate.setStyle("-fx-font-size: 14px; -fx-pref-width: 300;");

        DatePicker endDate = new DatePicker(); 
        endDate.setPromptText("Bitiş Tarihi");
        endDate.setStyle("-fx-font-size: 14px; -fx-pref-width: 300;");

        TextField maxPlayers = new TextField(); 
        maxPlayers.setPromptText("Takım Başı Maks. Oyuncu (Örn: 5)");
        maxPlayers.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        CheckBox ge250Check = new CheckBox("GE250 Puanı Verilsin mi?");
        ge250Check.setStyle("-fx-font-size: 14px; -fx-text-fill: #2B3674;");

        HBox btnBox = new HBox(15); 
        btnBox.setAlignment(Pos.CENTER);
        
        Button cancelBtn = new Button("İptal");
        cancelBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Oluştur");
        saveBtn.setStyle("-fx-background-color: #1E8E3E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        
        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || sportCombo.getValue() == null || campusCombo.getValue() == null || 
                startDate.getValue() == null || endDate.getValue() == null || maxPlayers.getText().isEmpty()) {
                showCustomAlert("Eksik Bilgi", "Lütfen tüm alanları doldurun.");
                return;
            }

            try {
                int players = Integer.parseInt(maxPlayers.getText().trim());
                
                Tournament t = new Tournament(
                    "", 
                    nameField.getText().trim(), 
                    SportType.valueOf(sportCombo.getValue()), 
                    startDate.getValue(), 
                    endDate.getValue(), 
                    players, 
                    ge250Check.isSelected(), 
                    "", 
                    campusCombo.getValue()
                );
                
                saveBtn.setText("İşleniyor...");
                saveBtn.setDisable(true);

                new Thread(() -> {
                    DbStatus status = tManager.createTournament(t);

                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) {
                            dialog.close(); 
                            loadTournaments(); 
                            showCustomAlert("Başarılı", "Turnuva başarıyla oluşturuldu.");
                        } else {
                            saveBtn.setText("Oluştur");
                            saveBtn.setDisable(false);
                            showCustomAlert("Hata", "Turnuva oluşturulurken bir hata meydana geldi.");
                        }
                    });
                }).start();
                
            } catch (NumberFormatException ex) {
                showCustomAlert("Hatalı Format", "Oyuncu sayısı sadece rakamlardan oluşmalıdır.");
            }
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        layout.getChildren().addAll(nameField, sportCombo, campusCombo, startDate, endDate, maxPlayers, ge250Check, btnBox);
        dialog.showAndWait();
    }

    private void handleEditTournament(Tournament t) {
        Stage dialog = createDialogStage("Turnuvayı Düzenle: " + t.getTournamentName());
        VBox layout = (VBox) dialog.getScene().getRoot();

        TextField nameField = new TextField(t.getTournamentName());
        nameField.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");
        
        TextField maxPlayers = new TextField(String.valueOf(t.getMaxPlayersPerTeam()));
        maxPlayers.setStyle("-fx-background-color: #F4F7FE; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-height: 40;");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("İptal");
        cancelBtn.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #2B3674; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Kaydet");
        saveBtn.setStyle("-fx-background-color: #1E8E3E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 35; -fx-cursor: hand;");
        
        saveBtn.setOnAction(e -> {
            try {
                int newPlayers = Integer.parseInt(maxPlayers.getText().trim());
                String newName = nameField.getText().trim();
                
                saveBtn.setText("Kaydediliyor...");
                saveBtn.setDisable(true);

                new Thread(() -> {
                    DbStatus status = tManager.editDetails(t, newName, newPlayers);
                    
                    Platform.runLater(() -> {
                        if (status == DbStatus.SUCCESS) { 
                            dialog.close(); 
                            loadTournaments();
                            showCustomAlert("Başarılı", "Turnuva başarıyla güncellendi.");
                        } else {
                            saveBtn.setText("Kaydet");
                            saveBtn.setDisable(false);
                            showCustomAlert("Hata", "Güncelleme sırasında hata oluştu.");
                        }
                    });
                }).start();
            } catch (NumberFormatException ex) {
                showCustomAlert("Hatalı Format", "Oyuncu sayısı sayısal bir değer olmalıdır.");
            }
        });
        
        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        layout.getChildren().addAll(new Label("Yeni Turnuva Başlığı:"), nameField, new Label("Takım Başı Max Katılımcı:"), maxPlayers, btnBox);
        dialog.showAndWait();
    }

    private void handleCreateFixture(Tournament t) {
        if (isProcessing) return;
        isProcessing = true;
        
        new Thread(() -> {
            List<Team> teams = tManager.getTournamentTeams(t.getTournamentId());
            
            if (teams == null || teams.size() < 2) {
                Platform.runLater(() -> { 
                    isProcessing = false; 
                    showCustomAlert("Hata", "Fikstür oluşturmak için turnuvada en az 2 onaylı takım bulunmalıdır."); 
                });
                return;
            }

            Collections.shuffle(teams);
            
            OffsetDateTime matchTime = OffsetDateTime.of(t.getStartDate(), LocalTime.of(1, 0), ZoneOffset.UTC);

            int matchCount = 0;
            for (int i = 0; i < teams.size(); i += 2) {
                Team t1 = teams.get(i);
                if (i + 1 < teams.size()) {
                    Team t2 = teams.get(i + 1);
                    db.insertMatch(t, t1, t2, matchTime, 10); 
                    matchCount++;
                } else {
                    db.insertMatch(t, t1, t1, matchTime, 0); 
                }
            }

            final int finalMatches = matchCount;
            Platform.runLater(() -> {
                isProcessing = false;
                showCustomAlert("Başarılı", "Kura çekildi! " + finalMatches + " adet eşleşme başarıyla oluşturuldu.");
            });
        }).start();
    }

    private void handleSchedule(Tournament t) {
        Stage dialog = createDialogStage("Fikstür ve Sonuçlar: " + t.getTournamentName());
        VBox layout = (VBox) dialog.getScene().getRoot();
        
        ScrollPane scrollPane = new ScrollPane(); 
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(400);

        VBox matchesBox = new VBox(10);
        matchesBox.setPadding(new Insets(10));

        new Thread(() -> {
            List<Match> matches = db.getAllTournamentMatches(t.getTournamentId());
            
            Platform.runLater(() -> {
                if (matches == null || matches.isEmpty()) {
                    Label emptyLabel = new Label("Henüz fikstür oluşturulmamış veya eşleşme bulunamadı.");
                    emptyLabel.setStyle("-fx-text-fill: #a3aed0; -fx-font-weight: bold;");
                    matchesBox.getChildren().add(emptyLabel);
                } else {
                    for (Match m : matches) {
                        HBox matchRow = new HBox(15); 
                        matchRow.setAlignment(Pos.CENTER_LEFT);
                        matchRow.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-padding: 10; -fx-background-color: #F8FAFC;");

                        String team1Name = (m.getTeam1() != null) ? m.getTeam1().getTeamName() : "Bilinmiyor";
                        String team2Name = (m.getTeam2() != null && !m.getTeam1().getTeamId().equals(m.getTeam2().getTeamId())) ? m.getTeam2().getTeamName() : "BAY GEÇTİ";
                        
                        Label mLabel = new Label(team1Name + " VS " + team2Name);
                        mLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B3674;");
                        
                        Region rSpacer = new Region();
                        HBox.setHgrow(rSpacer, Priority.ALWAYS);
                        
                        if (team2Name.equals("BAY GEÇTİ")) {
                            Label bayLabel = new Label("Otomatik Üst Tur");
                            bayLabel.setStyle("-fx-text-fill: #1E8E3E; -fx-font-weight: bold;");
                            matchRow.getChildren().addAll(mLabel, rSpacer, bayLabel);
                        } else {
                            ComboBox<String> winnerCombo = new ComboBox<>();
                            winnerCombo.getItems().addAll(team1Name, team2Name, "Beraberlik");
                            winnerCombo.setPromptText("Kazananı Seç");
                            
                            Button updateBtn = new Button("Onayla");
                            updateBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                            
                            updateBtn.setOnAction(e -> {
                                if (winnerCombo.getValue() == null) return;
                                
                                Team winner = null;
                                if (winnerCombo.getValue().equals(team1Name)) {
                                    winner = m.getTeam1();
                                } else if (winnerCombo.getValue().equals(team2Name)) {
                                    winner = m.getTeam2();
                                }
                                
                                final Team finalWinner = winner;
                                updateBtn.setText("...");
                                updateBtn.setDisable(true);
                                
                                new Thread(() -> {
                                    db.updateMatchWinner(m.getMatchId(), finalWinner);
                                    db.updateMatchStatus(m.getMatchId(), true);
                                    
                                    Platform.runLater(() -> {
                                        updateBtn.setText("Onaylandı");
                                        showCustomAlert("Başarılı", "Maç sonucu başarıyla kaydedildi ve takım statüsü ayarlandı.");
                                    });
                                }).start();
                            });
                            
                            matchRow.getChildren().addAll(mLabel, rSpacer, winnerCombo, updateBtn);
                        }
                        
                        matchesBox.getChildren().add(matchRow);
                    }
                }
            });
        }).start();

        scrollPane.setContent(matchesBox);
        
        Button closeBtn = new Button("Kapat");
        closeBtn.setStyle("-fx-background-color: #D93025; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 35; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());
        
        layout.getChildren().addAll(scrollPane, closeBtn);
        dialog.showAndWait();
    }

    @FXML
    public void handleBulkDelete(ActionEvent event) {
        List<Tournament> toDelete = new ArrayList<>();
        for (Map.Entry<CheckBox, Tournament> entry : selectionMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                toDelete.add(entry.getValue());
            }
        }

        if (toDelete.isEmpty()) {
            showCustomAlert("Uyarı", "Lütfen silmek istediğiniz turnuvaları seçin.");
            return;
        }

        isProcessing = true;
        new Thread(() -> {
            int count = 0;
            for (Tournament t : toDelete) {
                DbStatus status = db.deleteTournament(t.getTournamentId());
                if (status == DbStatus.SUCCESS) {
                    count++;
                }
            }
            
            final int finalCount = count;
            Platform.runLater(() -> {
                isProcessing = false;
                loadTournaments();
                showCustomAlert("Bilgi", finalCount + " adet turnuva kalıcı olarak silindi.");
            });
        }).start();
    }

    private Stage createDialogStage(String titleText) {
        Stage dialogStage = new Stage(); 
        dialogStage.initModality(Modality.APPLICATION_MODAL); 
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        
        VBox layout = new VBox(20); 
        layout.setAlignment(Pos.CENTER); 
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");
        
        DropShadow shadow = new DropShadow(); 
        shadow.setRadius(20); 
        shadow.setColor(Color.rgb(0, 0, 0, 0.15)); 
        layout.setEffect(shadow);
        
        Label titleLabel = new Label(titleText); 
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3674;");
        layout.getChildren().add(titleLabel);
        
        Scene scene = new Scene(layout); 
        scene.setFill(Color.TRANSPARENT); 
        dialogStage.setScene(scene); 
        dialogStage.centerOnScreen();
        
        return dialogStage;
    }

    private void showCustomAlert(String title, String message) {
        Stage dialog = createDialogStage(title);
        VBox layout = (VBox) dialog.getScene().getRoot();
        
        Label msgLabel = new Label(message); 
        msgLabel.setWrapText(true); 
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a3aed0; -fx-text-alignment: center;");
        
        Button okBtn = new Button("Tamam"); 
        okBtn.setStyle("-fx-background-color: #4318FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-pref-width: 120; -fx-pref-height: 40; -fx-cursor: hand;");
        okBtn.setOnAction(e -> dialog.close());
        
        layout.getChildren().addAll(msgLabel, okBtn); 
        dialog.showAndWait();
    }
}