package models;

import java.util.ArrayList;
import java.util.List;

public class Student extends User {
    private String studentId;
    private int eloPoint;
    private int penaltyPoints;
    private double reliabilityScore;
    private List<SportType> interests;
    private List<String> badges;
    private int matchesPlayed;
    private double winRate;
    private boolean isPublicProfile;
    private boolean isEloMatchingEnabled;
    private List<Match> matchHistory;
    private List<Student> friends;
    private List<Student> friendRequests;

    public Student(String fullName, String bilkentEmail, String nickname, String password, String studentId) {
        super(fullName, bilkentEmail, nickname, password);
        this.studentId = studentId;
        this.eloPoint = 1000;
        this.penaltyPoints = 0;
        this.reliabilityScore = 100.0;
        this.matchesPlayed = 0;
        this.winRate = 0.0;
        this.isPublicProfile = true;
        this.isEloMatchingEnabled = true;
        this.interests = new ArrayList<>();
        this.badges = new ArrayList<>();
        this.matchHistory = new ArrayList<>();
        this.friends = new ArrayList<>();
        this.friendRequests = new ArrayList<>();
    }

    public void addInterest(SportType sport) {
        if (!interests.contains(sport)) {
            interests.add(sport);
        }
    }

    public void removeInterest(SportType sport) {
        interests.remove(sport);
    }

    public void updateElo(boolean matchWon, int opponentElo) {
        int kFactor = 32;
        if (matchWon) {
            this.eloPoint += kFactor;
        } else {
            this.eloPoint -= kFactor;
        }
        this.matchesPlayed++;
    }

    public void updateReliabilityScore(boolean attended) {
        if (!attended) {
            this.reliabilityScore -= 5.0;
        } else if (this.reliabilityScore < 100.0) {
            this.reliabilityScore += 1.0;
        }
    }

    public void addPenaltyPoint(int points) {
        this.penaltyPoints += points;
    }

    public double calculateJaccardSimilarity(Student other) {
        int intersection = 0;
        for (SportType sport : this.interests) {
            if (other.interests.contains(sport)) {
                intersection++;
            }
        }
        int union = this.interests.size() + other.interests.size() - intersection;
        if (union == 0) return 0.0;
        return (double) intersection / union;
    }

    public void toggleEloMatching(boolean enabled) {
        this.isEloMatchingEnabled = enabled;
    }

    public void updateProfileVisibility(boolean isPublic) {
        this.isPublicProfile = isPublic;
    }

    public void sendFriendRequest(Student target) {
        if (!target.friendRequests.contains(this)) {
            target.friendRequests.add(this);
        }
    }

    public void acceptFriendRequest(Student requester) {
        if (this.friendRequests.contains(requester)) {
            this.friends.add(requester);
            requester.friends.add(this);
            this.friendRequests.remove(requester);
        }
    }

    public void removeFriend(Student target) {
        this.friends.remove(target);
        target.friends.remove(this);
    }

    public String getProfileTranscript() {
        return "Matches: " + matchesPlayed + " | Win Rate: " + String.format("%.1f", winRate) + "% | ELO: " + eloPoint;
    }

    public String getStudentId() {
    return studentId;
}

public void setStudentId(String studentId) {
    this.studentId = studentId;
}

public int getEloPoint() {
    return eloPoint;
}

public void setEloPoint(int eloPoint) {
    this.eloPoint = eloPoint;
}

public double getPenaltyPoints() {
    return penaltyPoints;
}

public void setPenaltyPoints(int penaltyPoints) {
    this.penaltyPoints = penaltyPoints;
}

public double getReliabilityScore() {
    return reliabilityScore;
}

public void setReliabilityScore(double reliabilityScore) {
    this.reliabilityScore = reliabilityScore;
}

public List<SportType> getInterests() {
    return interests;
}

public void setInterests(List<SportType> interests) {
    this.interests = interests;
}

public List<String> getBadges() {
    return badges;
}

public void setBadges(List<String> badges) {
    this.badges = badges;
}

public int getMatchesPlayed() {
    return matchesPlayed;
}

public void setMatchesPlayed(int matchesPlayed) {
    this.matchesPlayed = matchesPlayed;
}

public double getWinRate() {
    return winRate;
}

public void setWinRate(double winRate) {
    this.winRate = winRate;
}

public boolean isPublicProfile() {
    return isPublicProfile;
}

public void setPublicProfile(boolean isPublicProfile) {
    this.isPublicProfile = isPublicProfile;
}

public boolean isEloMatchingEnabled() {
    return isEloMatchingEnabled;
}

public void setEloMatchingEnabled(boolean isEloMatchingEnabled) {
    this.isEloMatchingEnabled = isEloMatchingEnabled;
}

public List<Match> getMatchHistory() {
    return matchHistory;
}

public void setMatchHistory(List<Match> matchHistory) {
    this.matchHistory = matchHistory;
}

public List<Student> getFriends() {
    return friends;
}

public void setFriends(List<Student> friends) {
    this.friends = friends;
}

public List<Student> getFriendRequests() {
    return friendRequests;
}

public void setFriendRequests(List<Student> friendRequests) {
    this.friendRequests = friendRequests;
}

}