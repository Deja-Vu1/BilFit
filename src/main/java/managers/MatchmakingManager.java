package managers;

import database.Database;
import database.DbStatus;
import models.Match;
import models.SportType;
import models.Student;
import models.Team;
import java.time.LocalDateTime;
import java.util.UUID;

public class MatchmakingManager {

    private Database db;
    private NotificationManager notifManager;

    public MatchmakingManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
    }
}