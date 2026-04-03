package managers;

import models.User;

public class SessionManager {
    
    // Eager initialization - JavaFX için Thread-Safe yapı
    private static final SessionManager instance = new SessionManager();
    private User currentUser;
    
    // Bizim ELO ve Reservation arasındaki bağlantıyı sağlayan değişkenimiz
    private String currentReservation;
    private boolean isDuelloRequested = false;

    public boolean isDuelloRequested() {
        return isDuelloRequested;
    }

    public void setDuelloRequested(boolean requested) {
        this.isDuelloRequested = requested;
    }

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

    // --- BİZİM EKLENTİLERİMİZ (SİLİNMEMESİ GEREKENLER) ---
    public String getCurrentReservation() {
        return currentReservation;
    }

    public void setCurrentReservation(String reservation) {
        this.currentReservation = reservation;
    }

    public void logout() {
        this.currentUser = null;
        this.currentReservation = null; // Çıkış yapınca rezervasyon da temizlenmeli
    }
}