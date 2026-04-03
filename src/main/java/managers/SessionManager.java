package managers;

import models.User;

public class SessionManager {
    
    // Eager initialization - JavaFX için Thread-Safe yapı
    private static final SessionManager instance = new SessionManager();
    
    // 1. Sisteme giriş yapan aktif kullanıcı
    private User currentUser;
    
    // 2. Sayfalar arası (Reservation -> ELO) taşınacak rezervasyon hafızası
    private String currentReservation;

    // 3. ELO sayfasındaki "Request" butonunun hafızası (Sayfa değişse de unutmasın diye)
    private boolean isDuelloRequested = false;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return instance;
    }

    // --- KULLANICI METODLARI ---
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // --- REZERVASYON METODLARI ---
    public String getCurrentReservation() {
        return currentReservation;
    }

    public void setCurrentReservation(String reservation) {
        this.currentReservation = reservation;
    }

    // --- DÜELLO BUTON HAFIZASI METODLARI ---
    public boolean isDuelloRequested() {
        return isDuelloRequested;
    }
    private boolean isJoinedWithCode = false;

    public boolean isJoinedWithCode() {
        return isJoinedWithCode;
    }

    public void setJoinedWithCode(boolean joined) {
        this.isJoinedWithCode = joined;
    }

    public void setDuelloRequested(boolean requested) {
        this.isDuelloRequested = requested;
    }

    // --- ÇIKIŞ YAPMA (TÜM HAFIZAYI TEMİZLEME) ---
    public void logout() {
        this.currentUser = null;
        this.currentReservation = null;
        this.isDuelloRequested = false; // Çıkış yapınca buton hafızası da sıfırlanır
        this.isJoinedWithCode = false;
    }
}