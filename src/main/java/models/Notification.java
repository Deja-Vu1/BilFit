package models;

import java.time.LocalDateTime;

public class Notification {
    private String notificationId;
    private String title;
    private String message;
    private LocalDateTime date;

    public Notification(String notificationId, String title, String message) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.date = LocalDateTime.now(); 
    }

    public void sendToUser(User user) {
        System.out.println("To: " + user.getNickname() + " | Subject: " + this.title + " | Message: " + this.message);
    }

    public void broadcastToAll(String message) {
        System.out.println("SYSTEM BROADCAST: " + message);
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getDate() { return date; }
}