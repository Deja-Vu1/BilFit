package managers;

import database.Database;
import database.DbStatus;
import models.Duello;
import models.Student;

public class DuelloManager {

    private Database db;

    public DuelloManager(Database db) {
        this.db = db;
    }

    public DbStatus createDuello(Duello duello, Student creator) {
        DbStatus status = db.insertDuello(duello.getReservationId(), creator.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            duello.publishDuello();
        }
        
        return status;
    }

    public DbStatus requestToJoinDuello(Duello duello, Student student) {
        DbStatus status = db.insertDuelloRequest(duello.getReservationId(), student.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            duello.requestToJoin(student);
        }
        
        return status;
    }

    public DbStatus acceptDuelloRequest(Duello duello, Student student) {
        DbStatus status = db.updateDuelloParticipant(duello.getReservationId(), student.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            duello.acceptRequest(student);
        }
        
        return status;
    }

    public DbStatus joinDuelloWithCode(Duello duello, Student student, String code) {
        DbStatus status = db.verifyAndJoinDuello(duello.getReservationId(), student.getStudentId(), code);
        
        if (status == DbStatus.SUCCESS) {
            duello.joinWithCode(student, code);
        }
        
        return status;
    }
}