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

    public DuelloManager(Database db) {
        this.db = db;
    }

    public DbStatus createDuello(Duello duello, Student creator) {
        if (!creator.isCanAttend()) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.insertDuello(duello.getReservationId(), creator.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            duello.setMatched(false);
            if (!duello.getAttendees().contains(creator)) {
                duello.getAttendees().add(creator);
            }
        }
        
        return status;
    }

    public DbStatus requestToJoinDuello(Duello duello, Student student) {
        if (duello.isCancelled() || !student.isCanAttend() || duello.isMatched() || duello.getEmptySlots() <= 0 || duello.getAttendees().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }
        return db.insertDuelloRequest(duello.getReservationId(), student.getStudentId());
    }

    public DbStatus acceptDuelloRequest(Duello duello, Student student) {
        if (duello.isCancelled() || duello.isMatched() || duello.getEmptySlots() <= 0 || duello.getAttendees().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.updateDuelloParticipant(duello.getReservationId(), student.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            duello.getAttendees().add(student);
            duello.setEmptySlots(duello.getEmptySlots() - 1);
            
            if (duello.getEmptySlots() <= 0) {
                duello.setMatched(true);
                finalizeDuelloMatch(duello);
            }
        }
        
        return status;
    }

    public DbStatus joinDuelloWithCode(Duello duello, Student student, String code) {
        if (duello.isCancelled() || !student.isCanAttend() || !code.equals(duello.getAccessCode()) || duello.isMatched() || duello.getEmptySlots() <= 0 || duello.getAttendees().contains(student)) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.verifyAndJoinDuello(duello.getReservationId(), student.getStudentId(), code);
        
        if (status == DbStatus.SUCCESS) {
            duello.getAttendees().add(student);
            duello.setEmptySlots(duello.getEmptySlots() - 1);
            
            if (duello.getEmptySlots() <= 0) {
                duello.setMatched(true);
                finalizeDuelloMatch(duello);
            }
        }
        
        return status;
    }

    private void finalizeDuelloMatch(Duello duello) {
        if (duello.getAttendees().size() == 2) {
            Student p1 = duello.getAttendees().get(0);
            Student p2 = duello.getAttendees().get(1);
            SportType sport = duello.getFacility().getSportType();
            String matchId = UUID.randomUUID().toString();
            
            DbStatus matchStatus = db.insertMatch(matchId, p1.getStudentId(), p2.getStudentId(), sport.name());
            
            if (matchStatus == DbStatus.SUCCESS) {
                Team t1 = new Team(p1.getStudentId() + "_T", p1.getNickname(), "SOLO", 1, false, p1);
                Team t2 = new Team(p2.getStudentId() + "_T", p2.getNickname(), "SOLO", 1, false, p2);
                Match m = new Match(matchId, LocalDateTime.now(), sport, t1, t2);
                duello.setScheduledMatch(m);
            }
        }
    }
}