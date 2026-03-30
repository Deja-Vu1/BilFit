package managers;

import database.Database;
import database.DbStatus;
import models.SportType;
import models.Student;

public class StudentManager {

    private Database db;

    public StudentManager(Database db) {
        this.db = db;
    }

    public DbStatus addInterest(Student student, SportType sport) {
        DbStatus status = db.insertStudentInterest(student.getStudentId(), sport.name());
        
        if (status == DbStatus.SUCCESS) {
            student.addInterest(sport);
        }
        return status;
    }

    public DbStatus removeInterest(Student student, SportType sport) {
        DbStatus status = db.deleteStudentInterest(student.getStudentId(), sport.name());
        
        if (status == DbStatus.SUCCESS) {
            student.removeInterest(sport);
        }
        return status;
    }

    public DbStatus toggleEloMatching(Student student, boolean enabled) {
        DbStatus status = db.updateEloMatchingStatus(student.getStudentId(), enabled);
        
        if (status == DbStatus.SUCCESS) {
            student.toggleEloMatching(enabled);
        }
        return status;
    }

    public DbStatus updateProfileVisibility(Student student, boolean isPublic) {
        DbStatus status = db.updateStudentProfileVisibility(student.getStudentId(), isPublic);
        
        if (status == DbStatus.SUCCESS) {
            student.updateProfileVisibility(isPublic);
        }
        return status;
    }

    public DbStatus rateOpponent(Student target, double score) {
        return db.insertStudentRating(target.getStudentId(), score);
    }

    public DbStatus sendFriendRequest(Student sender, Student target) {
        return db.insertFriendRequest(sender.getStudentId(), target.getStudentId());
    }

    public DbStatus acceptFriendRequest(Student receiver, Student requester) {
        DbStatus status = db.acceptFriendRequest(receiver.getStudentId(), requester.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            receiver.acceptFriendRequest(requester);
        }
        return status;
    }

    public DbStatus removeFriend(Student student, Student target) {
        DbStatus status = db.deleteFriend(student.getStudentId(), target.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            student.removeFriend(target);
        }
        return status;
    }
}