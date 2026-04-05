package managers;

import java.util.ArrayList;
import java.util.List;

import database.Database;
import database.DbStatus;
import models.SportType;
import models.Student;

public class StudentManager {

    private Database db;
    private NotificationManager notifManager;

    public StudentManager(Database db) {
        this.db = db;
        this.notifManager = new NotificationManager(db);
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

    public List<SportType> getUserInterests(Student student) {
        if (student == null) {
            return new ArrayList<>();
        }
        return student.getInterests();
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

    public DbStatus rateOpponent(Student student, Student target, double score) {
        if (student == null || target == null || score < 0 || score > 100) return DbStatus.QUERY_ERROR;
        
        boolean hasPlayed = db.hasPlayedMatchTogether(student.getBilkentEmail(), target.getBilkentEmail());
        if (!hasPlayed) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.insertStudentRating(target.getBilkentEmail(), score);
        if (status == DbStatus.SUCCESS) {
            int count = target.getRatingCount() + 1;
            double newScore = ((target.getReliabilityScore() * (count - 1)) + score) / count;
            
            target.setReliabilityScore(newScore);
            target.setRatingCount(count);
        }
        return status;
    }

    public DbStatus sendFriendRequest(Student sender, Student target) {
        if (sender == null || target == null || sender.getBilkentEmail().equals(target.getBilkentEmail())) return DbStatus.QUERY_ERROR;
        
        if (target.getFriendRequests().contains(sender) || target.getFriends().contains(sender)) {
            return DbStatus.QUERY_ERROR;
        }

        DbStatus status = db.insertFriendRequest(sender.getBilkentEmail(), target.getBilkentEmail());
        if (status == DbStatus.SUCCESS) {
            target.getFriendRequests().add(sender);
            notifManager.sendToUser(target, "Friend Request", sender.getNickname() + " sent you a friend request.");
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
            
            notifManager.sendToUser(requester, "Friend Request Accepted", receiver.getNickname() + " accepted your friend request.");
        }
        return status;
    }

    public DbStatus declineFriendRequest(Student receiver, Student requester) {
        if (receiver == null || requester == null) return DbStatus.QUERY_ERROR;

        DbStatus status = db.deleteFriendRequest(requester.getBilkentEmail(), receiver.getBilkentEmail());
        if (status == DbStatus.SUCCESS || status == DbStatus.DATA_NOT_FOUND) {
             receiver.getFriendRequests().remove(requester);
             return DbStatus.SUCCESS;
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
        if (student == null || newNickname == null || newNickname.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        // Veritabanındaki full_name'i günceller
        DbStatus status = db.updateUserNickname(student.getBilkentEmail(), newNickname);
        if (status == DbStatus.SUCCESS) {
            // İŞTE ÇÖZÜM BURADA: Hafızadaki (Session) aktif kullanıcının adını da anında güncelliyoruz!
            student.setFullName(newNickname);
        }
        return status;
    }

    public DbStatus updatePassword(Student student, String newPassword) {
        if (student == null || newPassword == null || newPassword.trim().isEmpty()) return DbStatus.QUERY_ERROR;

        DbStatus status = db.updatePassword(student.getBilkentEmail(), newPassword);
        if (status == DbStatus.SUCCESS) {
            student.setPassword(newPassword);
        }
        return status;
    }
    public DbStatus updateProfilePicture(Student student, java.io.File file) {
        if (student == null || file == null) return DbStatus.QUERY_ERROR;
        DbStatus status = db.updateProfilePicture(student.getBilkentEmail(), file);
        if (status == DbStatus.SUCCESS) {
            // Yükleme başarılıysa URL'i veritabanından tazeleyip Session'a yansıt
            db.fillStudentDataByEmail(student, student.getBilkentEmail());
        }
        return status;
    }
}