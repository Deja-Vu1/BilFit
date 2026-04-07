package managers;

import database.Database;
import database.DbStatus;
import models.Duello;
import models.Student;
import java.util.ArrayList;

public class DuelloManager {

    private Database db;
    private NotificationManager notifManager;

    public DuelloManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
    }

    public DbStatus createDuello(Duello duello, Student creator) {
        if (duello == null || creator == null || !creator.isCanAttend() || creator.isBanned()) {
            return DbStatus.QUERY_ERROR;
        }

        duello.setEmptySlots(1);

        DbStatus status = db.insertDuello(
            duello.getReservationId(), 
            creator.getBilkentEmail(), 
            duello.getRequiredSkillLevel(), 
            duello.getEmptySlots()
        );
        
        if (status == DbStatus.SUCCESS) {
            duello.setMatched(false);
            if (!duello.getAttendees().contains(creator)) {
                duello.getAttendees().add(creator);
            }
        }
        return status;
    }

    public DbStatus requestToJoinDuello(Duello duello, Student student) {
        if (duello == null || student == null || duello.isCancelled() || !student.isCanAttend() || student.isBanned() || duello.isMatched() || duello.getEmptySlots() <= 0) {
            return DbStatus.QUERY_ERROR;
        }
        
        if (duello.getAttendees().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }
        
        Student creator = duello.getAttendees().get(0);
        if (creator.isEloMatchingEnabled() && student.isEloMatchingEnabled()) {
            if (Math.abs(creator.getEloPoint() - student.getEloPoint()) > 400) {
                return DbStatus.QUERY_ERROR;
            }
        }

        DbStatus status = db.insertDuelloRequest(duello.getReservationId(), student.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            notifManager.sendToUser(creator, "New Duello Request", student.getNickname() + " wants to join your duello.");
        }
        return status;
    }

    public DbStatus acceptDuelloRequest(Duello duello, Student student) {
        if (duello == null || student == null || duello.isCancelled() || duello.isMatched() || duello.getEmptySlots() <= 0 || duello.getAttendees().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }

        if (!student.isCanAttend() || student.isBanned()) {
            return DbStatus.QUERY_ERROR;
        }

        Student creator = duello.getAttendees().get(0);
        if (!creator.isCanAttend() || creator.isBanned()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateDuelloParticipant(duello.getReservationId(), student.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            duello.getAttendees().add(student);
            duello.setEmptySlots(duello.getEmptySlots() - 1);
            
            if (duello.getEmptySlots() <= 0) {
                finalizeDuelloMatch(duello);
            }
        }
        return status;
    }

    public DbStatus declineDuelloRequest(Duello duello, Student student) {
        if (duello == null || student == null) return DbStatus.QUERY_ERROR;
        return db.deleteDuelloRequest(duello.getReservationId(), student.getBilkentEmail());
    }

    public DbStatus cancelDuello(Duello duello, Student creator) {
        if (duello == null || creator == null) return DbStatus.QUERY_ERROR;
        DbStatus status = db.deleteDuello(duello.getReservationId(), creator.getBilkentEmail());
        if(status == DbStatus.SUCCESS) {
            db.insertNotification(creator.getBilkentEmail(), 
                            "Your Duello match deleted", 
                             creator.getFullName() + " has deleted the duello. The match has been cancelled.");
            duello.setCancelled(true);
            duello.getAttendees().clear();
        }
        return status;
    }

    public DbStatus leaveDuello(Duello duello, Student student) {
        if (duello == null || student == null) return DbStatus.QUERY_ERROR;
        DbStatus status = db.removeDuelloParticipant(duello.getReservationId(), student.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            db.insertNotification(duello.getAttendees().get(0).getBilkentEmail(), 
                            "Your Duello match deleted", 
                             student.getFullName() + " has left the duello. The match has been cancelled.");
            duello.setMatched(false);
            duello.setEmptySlots(duello.getEmptySlots() + 1);
            duello.getAttendees().remove(student);
        }
        return status;
    }

    public DbStatus joinDuelloWithCode(String code, Student student) {
        if (code == null || student == null || code.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        Duello duello = db.getDuelloByCode(code);
        
        if (duello == null) return DbStatus.DATA_NOT_FOUND;
        if (duello.getAttendees().contains(student)) return DbStatus.QUERY_ERROR;

        if (!student.isCanAttend() || student.isBanned()) return DbStatus.QUERY_ERROR;

        Student creator = duello.getAttendees().get(0);
        if (!creator.isCanAttend() || creator.isBanned()) return DbStatus.QUERY_ERROR;

        if (creator.isEloMatchingEnabled() && student.isEloMatchingEnabled()) {
            if (Math.abs(creator.getEloPoint() - student.getEloPoint()) > 400) {
                return DbStatus.QUERY_ERROR;
            }
        }

        DbStatus status = db.verifyAndJoinDuello(duello.getReservationId(), student.getBilkentEmail(), code);
        if (status == DbStatus.SUCCESS) {
            duello.getAttendees().add(student);
            duello.setEmptySlots(duello.getEmptySlots() - 1);
            
            if (duello.getEmptySlots() <= 0) {
                finalizeDuelloMatch(duello);
            }
        }
        return status;
    }

    public ArrayList<Duello> findOpponentForMatch(Student currentStudent, String sportName) {
        if (currentStudent == null || sportName == null || sportName.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return db.findOpponentForMatch(currentStudent, sportName);
    }
    
    public ArrayList<Duello> getUserDuellos(Student currentStudent) {
        if (currentStudent == null) return new ArrayList<>();
        return db.getUserDuellos(currentStudent);
    }

    public ArrayList<Student> getPendingRequestsForDuello(String reservationId) {
        if (reservationId == null || reservationId.trim().isEmpty()) return new ArrayList<>();
        return db.getPendingRequestsForDuello(reservationId);
    }

    public DbStatus updateMatchWinner(String matchId, Boolean isCreatorWin) {
        if (matchId == null || matchId.trim().isEmpty()) {
            return DbStatus.QUERY_ERROR;
        }
        return db.updateMatchWinner(matchId, isCreatorWin);
    }

    private void finalizeDuelloMatch(Duello duello) {
        if (duello.getAttendees().size() == 2) {
            Student p1 = duello.getAttendees().get(0);
            Student p2 = duello.getAttendees().get(1);

            if (!p1.isCanAttend() || p1.isBanned() || !p2.isCanAttend() || p2.isBanned()) {
                duello.setEmptySlots(1);
                duello.getAttendees().remove(p2);
                return;
            }

            duello.setMatched(true);
            notifManager.sendToUser(p1, "Duello Matched", "An opponent has been found for your duello!");
            notifManager.sendToUser(p2, "Duello Matched", "Your duello match is ready!");
        }
    }
}