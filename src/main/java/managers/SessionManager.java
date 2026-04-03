package managers;

import models.Reservation;
import models.Student;
import models.User;
import java.util.ArrayList;

import database.Database;

public class SessionManager {

    private static final SessionManager instance = new SessionManager();
    
    private User currentUser;
    // TEK OBJE YERİNE ARRAYLIST OLDU
    private ArrayList<Reservation> currentReservations = new ArrayList<>();

    private boolean isDuelloRequested = false;
    private boolean isJoinedWithCode = false;
    private boolean isTournamentApplied = false;
    private boolean isTournamentJoinedWithCode = false;

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

    // YENİ ARRAYLIST GETTER / SETTER
    public ArrayList<Reservation> getCurrentReservations() {
        return currentReservations;
    }

    public void setCurrentReservations(ArrayList<Reservation> reservations) {
        this.currentReservations = reservations != null ? reservations : new ArrayList<>();
    }
    
    public void addReservation(Reservation res) {
        if (res != null) this.currentReservations.add(res);
    }

    public boolean isDuelloRequested() {
        return isDuelloRequested;
    }

    public void setDuelloRequested(boolean requested) {
        this.isDuelloRequested = requested;
    }

    public boolean isJoinedWithCode() {
        return isJoinedWithCode;
    }

    public void setJoinedWithCode(boolean joined) {
        this.isJoinedWithCode = joined;
    }

    public boolean isTournamentApplied() {
        return isTournamentApplied;
    }

    public void setTournamentApplied(boolean applied) {
        this.isTournamentApplied = applied;
    }

    public boolean isTournamentJoinedWithCode() {
        return isTournamentJoinedWithCode;
    }

    public void setTournamentJoinedWithCode(boolean joined) {
        this.isTournamentJoinedWithCode = joined;
    }

    public void logout() {
        this.currentUser = null;
        this.currentReservations.clear(); // Liste temizlendi
        this.isDuelloRequested = false;
        this.isJoinedWithCode = false;
        this.isTournamentApplied = false;
        this.isTournamentJoinedWithCode = false;
    }
    
}