package managers;

import database.Database;
import database.DbStatus;
import models.Duello;
import models.Match;
import models.SportType;
import models.Student;
import models.Team;
import java.time.LocalDateTime;
import java.util.UUID;

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

        DbStatus status = db.insertDuello(duello.getReservationId(), creator.getBilkentEmail());
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
        if (duello == null || creator == null || duello.isMatched()) return DbStatus.QUERY_ERROR;
        
        if(duello.getAttendees().isEmpty() || !duello.getAttendees().get(0).getBilkentEmail().equals(creator.getBilkentEmail())) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.deleteDuello(duello.getReservationId());
        if(status == DbStatus.SUCCESS) {
            duello.setCancelled(true);
            duello.getAttendees().clear();
        }
        return status;
    }

    public DbStatus joinDuelloWithCode(Duello duello, Student student, String code) {
        if (duello == null || student == null || code == null) return DbStatus.QUERY_ERROR;
        if (duello.isCancelled() || !student.isCanAttend() || student.isBanned() || !code.equals(duello.getAccessCode()) || duello.isMatched() || duello.getEmptySlots() <= 0 || duello.getAttendees().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }

        Student creator = duello.getAttendees().get(0);
        if (!creator.isCanAttend() || creator.isBanned()) {
            return DbStatus.QUERY_ERROR;
        }

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

    private void finalizeDuelloMatch(Duello duello) {
        if (duello.getAttendees().size() == 2) {
            Student p1 = duello.getAttendees().get(0);
            Student p2 = duello.getAttendees().get(1);

            if (!p1.isCanAttend() || p1.isBanned() || !p2.isCanAttend() || p2.isBanned()) {
                duello.setEmptySlots(1);
                duello.getAttendees().remove(p2);
                return;
            }

            SportType sport = duello.getFacility().getSportType();
            String matchId = UUID.randomUUID().toString();
            
            DbStatus matchStatus = db.insertMatch(matchId, p1.getBilkentEmail(), p2.getBilkentEmail(), sport.name());
            
            if (matchStatus == DbStatus.SUCCESS) {
                Team t1 = new Team(p1.getStudentId() + "_T", p1.getNickname(), "SOLO", 1, false, p1);
                Team t2 = new Team(p2.getStudentId() + "_T", p2.getNickname(), "SOLO", 1, false, p2);
                Match m = new Match(matchId, LocalDateTime.now(), sport, t1, t2);
                duello.setScheduledMatch(m);
                duello.setMatched(true);

                notifManager.sendToUser(p1, "Duello Matched", "An opponent has been found for your duello!");
                notifManager.sendToUser(p2, "Duello Matched", "Your duello match is ready!");
            } else {
                duello.setEmptySlots(1);
                duello.getAttendees().remove(p2);
            }
        }
    }
}