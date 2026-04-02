package managers;

import database.Database;
import database.DbStatus;
import models.Notification;
import models.User;

public class NotificationManager {

    private Database db;

    public NotificationManager() {
        this.db = Database.getInstance();
    }

    public DbStatus sendToUser(User user, Notification notification) {
        return db.insertNotification(user.getBilkentEmail(), notification.getTitle(), notification.getMessage());
    }

    public DbStatus broadcastToAll(Notification notification) {
        return db.insertNotification("BROADCAST", notification.getTitle(), notification.getMessage());
    }
}