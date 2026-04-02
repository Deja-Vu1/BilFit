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
        if (student == null || sport == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.insertStudentInterest(student.getBilkentEmail(), sport.name());
        if (status == DbStatus.SUCCESS && !student.getInterests().contains(sport)) {
            student.getInterests().add(sport);
        }
        return status;
    }

    public DbStatus removeInterest(Student student, SportType sport) {
        if (student == null || sport == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.deleteStudentInterest(student.getBilkentEmail(), sport.name());
        if (status == DbStatus.SUCCESS) {
            student.getInterests().remove(sport);
        }
        return status;
    }

    public DbStatus toggleEloMatching(Student student, boolean enabled) {
        if (student == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateEloMatchingStatus(student.getBilkentEmail(), enabled);
        if (status == DbStatus.SUCCESS) {
            student.setEloMatchingEnabled(enabled);
        }
        return status;
    }

    public DbStatus updateProfileVisibility(Student student, boolean isPublic) {
        if (student == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateStudentProfileVisibility(student.getBilkentEmail(), isPublic);
        if (status == DbStatus.SUCCESS) {
            student.setPublicProfile(isPublic);
        }
        return status;
    }

    public DbStatus rateOpponent(Student target, double score) {
        if (target == null || score < 0) return DbStatus.QUERY_ERROR;

        DbStatus status = db.insertStudentRating(target.getBilkentEmail(), score);
        if (status == DbStatus.SUCCESS) {
            // Ağırlıklı ortalama hesabı
            int count = target.getRatingCount() + 1;
            double newScore = ((target.getReliabilityScore() * (count - 1)) + score) / count;
            
            target.setReliabilityScore(newScore);
            target.setRatingCount(count);
        }
        return status;
    }

    public DbStatus sendFriendRequest(Student sender, Student target) {
        if (sender == null || target == null || sender.getBilkentEmail().equals(target.getBilkentEmail())) return DbStatus.QUERY_ERROR;

        DbStatus status = db.insertFriendRequest(sender.getBilkentEmail(), target.getBilkentEmail());
        if (status == DbStatus.SUCCESS && !target.getFriendRequests().contains(sender)) {
            target.getFriendRequests().add(sender);
        }
        return status;
    }

    public DbStatus acceptFriendRequest(Student receiver, Student requester) {
        if (receiver == null || requester == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.acceptFriendRequest(requester.getBilkentEmail(), receiver.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            if (!receiver.getFriends().contains(requester)) receiver.getFriends().add(requester);
            receiver.getFriendRequests().remove(requester);
            if (!requester.getFriends().contains(receiver)) requester.getFriends().add(receiver);
        }
        return status;
    }

    public DbStatus removeFriend(Student student, Student target) {
        if (student == null || target == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.deleteFriend(student.getBilkentEmail(), target.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            student.getFriends().remove(target);
            target.getFriends().remove(student);
        }
        return status;
    }

    public DbStatus updateNickname(Student student, String newNickname) {
        if (student == null || newNickname == null || newNickname.isEmpty()) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updateUserNickname(student.getBilkentEmail(), newNickname);
        if (status == DbStatus.SUCCESS) {
            student.setNickname(newNickname);
        }
        return status;
    }

    public DbStatus updatePassword(Student student, String newPassword) {
        if (student == null || newPassword == null || newPassword.isEmpty()) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updatePassword(student.getBilkentEmail(), newPassword);
        if (status == DbStatus.SUCCESS) {
            student.setPassword(newPassword);
        }
        return status;
    }
}