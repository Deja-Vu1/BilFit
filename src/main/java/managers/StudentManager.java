package managers;

import database.Database;
import database.DbStatus;
import models.SportType;
import models.Student;

public class StudentManager {

    private Database db;

    public StudentManager(Database db) {
        this.db =  db;
    }

    public DbStatus addInterest(Student student, SportType sport) {
        DbStatus status = db.insertStudentInterest(student.getBilkentEmail(), sport.name());
        
        if (status == DbStatus.SUCCESS && !student.getInterests().contains(sport)) {
            student.getInterests().add(sport);
        }
        return status;
    }

    public DbStatus removeInterest(Student student, SportType sport) {
        DbStatus status = db.deleteStudentInterest(student.getBilkentEmail(), sport.name());
        
        if (status == DbStatus.SUCCESS) {
            student.getInterests().remove(sport);
        }
        return status;
    }

    public DbStatus toggleEloMatching(Student student, boolean enabled) {
        DbStatus status = db.updateEloMatchingStatus(student.getBilkentEmail(), enabled);
        
        if (status == DbStatus.SUCCESS) {
            student.setEloMatchingEnabled(enabled);
        }
        return status;
    }

    public DbStatus updateProfileVisibility(Student student, boolean isPublic) {
        DbStatus status = db.updateStudentProfileVisibility(student.getStudentId(), isPublic);
        
        if (status == DbStatus.SUCCESS) {
            student.setPublicProfile(isPublic);
        }
        return status;
    }

    public DbStatus rateOpponent(Student target, double score) {
        return db.insertStudentRating(target.getStudentId(), score);
    }

    public DbStatus sendFriendRequest(Student sender, Student target) {
        DbStatus status = db.insertFriendRequest(sender.getStudentId(), target.getStudentId());
        
        if (status == DbStatus.SUCCESS && !target.getFriendRequests().contains(sender)) {
            target.getFriendRequests().add(sender);
        }
        return status;
    }

    public DbStatus acceptFriendRequest(Student receiver, Student requester) {
        DbStatus status = db.acceptFriendRequest(receiver.getStudentId(), requester.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            receiver.getFriends().add(requester);
            receiver.getFriendRequests().remove(requester);
            requester.getFriends().add(receiver);
        }
        return status;
    }

    public DbStatus removeFriend(Student student, Student target) {
        DbStatus status = db.deleteFriend(student.getStudentId(), target.getStudentId());
        
        if (status == DbStatus.SUCCESS) {
            student.getFriends().remove(target);
            target.getFriends().remove(student);
        }
        return status;
    }

    public DbStatus updateNickname(Student student, String newNickname) {
        DbStatus status = db.updateUserNickname(student.getBilkentEmail(), newNickname);
        
        if (status == DbStatus.SUCCESS) {
            student.setNickname(newNickname);
        }
        
        return status;
    }

    public DbStatus updatePassword(Student student, String newPassword) {
        DbStatus status = db.updateUserPassword(student.getBilkentEmail(), newPassword);
        
        if (status == DbStatus.SUCCESS) {
            student.setPassword(newPassword);
        }
        
        return status;
    }
}