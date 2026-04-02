package managers;

import models.User;

public class SessionManager {
    
    // Eager initialization - JavaFX için Thread-Safe yapı
    private static final SessionManager instance = new SessionManager();
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }
}