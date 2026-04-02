package managers;

import database.Database;
import database.DbStatus;
import models.Notification;
import models.User;
import java.util.UUID;

public class NotificationManager {

    private Database db;

    public NotificationManager(Database db) {
        this.db = db;
    }

    public Notification sendToUser(User user, String title, String message) {
        if (user == null || title == null || message == null || title.isEmpty()) return null;

        String notifId = UUID.randomUUID().toString();
        Notification notification = new Notification(notifId, title, message);
        
        DbStatus status = db.insertNotification(user.getBilkentEmail(), notification.getTitle(), notification.getMessage());
        return status == DbStatus.SUCCESS ? notification : null;
    }

    public Notification broadcastToAll(String title, String message) {
        if (title == null || message == null || title.isEmpty()) return null;

        String notifId = UUID.randomUUID().toString();
        Notification notification = new Notification(notifId, title, message);
        
        DbStatus status = db.insertNotification("BROADCAST", notification.getTitle(), notification.getMessage());
        return status == DbStatus.SUCCESS ? notification : null;
    }
}