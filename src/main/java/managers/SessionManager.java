package managers;

import models.Reservation;
import models.User;

public class SessionManager {

    private static final SessionManager instance = new SessionManager();
    
    private User currentUser;
    private Reservation currentReservation;

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

    public Reservation getCurrentReservation() {
        return currentReservation;
    }

    public void setCurrentReservation(Reservation reservation) {
        this.currentReservation = reservation;
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
        this.currentReservation = null;
        this.isDuelloRequested = false;
        this.isJoinedWithCode = false;
        this.isTournamentApplied = false;
        this.isTournamentJoinedWithCode = false;
    }
}