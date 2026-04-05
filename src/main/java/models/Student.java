package models;

import java.util.ArrayList;
import java.util.List;

public class Student extends User {
    public static final int MAX_PENALTY_LIMIT = 3;
    public static final double MIN_RELIABILITY_SCORE = 50.0;
    
    private String studentId;
    private String nickname; // EKLENEN KISIM: Nickname değişkeni
    private int eloPoint;
    private int penaltyPoints;
    private double reliabilityScore;
    private int ratingCount;
    private List<SportType> interests;
    private List<String> badges;
    private int matchesPlayed;
    private int matchesWon;
    private double winRate;
    private boolean isPublicProfile;
    private boolean isEloMatchingEnabled;
    private boolean isBanned;
    private List<Match> matchHistory;
    
    // Arkadaşlık sistemi listeleri
    private List<Student> friends; // Kabul edilmiş arkadaşlar
    private List<Student> incomingFriendRequests; // Bana gelen ve kabul etmemi bekleyen istekler
    private List<Student> outgoingFriendRequests; // Benim attığım ve karşı tarafın kabul etmesini beklediğim istekler

    public Student(String fullName, String bilkentEmail, String studentId) {
        super(fullName, bilkentEmail);
        this.studentId = studentId;

        // Başlangıçta nickname boş kalmasın diye tam adı atıyoruz. (İstersen boş da bırakabilirsin)
        this.nickname = fullName; 
        this.eloPoint = 1000;
        this.penaltyPoints = 0;
        this.reliabilityScore = 100.0;
        this.ratingCount = 0;
        this.matchesPlayed = 0;
        this.matchesWon = 0;
        this.winRate = 0.0;
        this.isPublicProfile = true;
        this.isEloMatchingEnabled = true;
        this.isBanned = false;
        
        this.interests = new ArrayList<>();
        this.badges = new ArrayList<>();
        this.matchHistory = new ArrayList<>();
        
        // Listelerin başlatılması
        this.friends = new ArrayList<>();
        this.incomingFriendRequests = new ArrayList<>();
        this.outgoingFriendRequests = new ArrayList<>();
    }
    

    // Getter ve Setter metotları
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public int getEloPoint() { return eloPoint; }
    public void setEloPoint(int eloPoint) { this.eloPoint = eloPoint; }
    public int getPenaltyPoints() { return penaltyPoints; }
    public void setPenaltyPoints(int penaltyPoints) { this.penaltyPoints = penaltyPoints; }
    public double getReliabilityScore() { return reliabilityScore; }
    public void setReliabilityScore(double reliabilityScore) { this.reliabilityScore = reliabilityScore; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public List<SportType> getInterests() { return interests; }
    public void setInterests(List<SportType> interests) { this.interests = interests; }
    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }
    public int getMatchesWon() { return matchesWon; }
    public void setMatchesWon(int matchesWon) { this.matchesWon = matchesWon; }
    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    public boolean isPublicProfile() { return isPublicProfile; }
    public void setPublicProfile(boolean isPublicProfile) { this.isPublicProfile = isPublicProfile; }
    public boolean isEloMatchingEnabled() { return isEloMatchingEnabled; }
    public void setEloMatchingEnabled(boolean isEloMatchingEnabled) { this.isEloMatchingEnabled = isEloMatchingEnabled; }
    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean isBanned) { this.isBanned = isBanned; }
    
    public boolean isCanAttend() { 
        return this.penaltyPoints < MAX_PENALTY_LIMIT && this.reliabilityScore >= MIN_RELIABILITY_SCORE && !this.isBanned; 
    }
    
    public List<Match> getMatchHistory() { return matchHistory; }
    public void setMatchHistory(List<Match> matchHistory) { this.matchHistory = matchHistory; }
    
    public List<Student> getFriends() { return friends; }
    public void setFriends(List<Student> friends) { this.friends = friends; }
    
    public List<Student> getIncomingFriendRequests() { return incomingFriendRequests; }
    public void setIncomingFriendRequests(List<Student> incomingFriendRequests) { this.incomingFriendRequests = incomingFriendRequests; }
    
    public List<Student> getOutgoingFriendRequests() { return outgoingFriendRequests; }
    public void setOutgoingFriendRequests(List<Student> outgoingFriendRequests) { this.outgoingFriendRequests = outgoingFriendRequests; }

    public String toString() {
        return "Student{" +
                "fullName='" + getFullName() + '\'' +
                ", bilkentEmail='" + getBilkentEmail() + '\'' +
                ", studentId='" + studentId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", eloPoint=" + eloPoint +
                ", penaltyPoints=" + penaltyPoints +
                ", reliabilityScore=" + reliabilityScore +
                ", ratingCount=" + ratingCount +
                ", interests=" + interests +
                ", badges=" + badges +
                ", matchesPlayed=" + matchesPlayed +
                ", matchesWon=" + matchesWon +
                ", winRate=" + winRate +
                ", isPublicProfile=" + isPublicProfile +
                ", isEloMatchingEnabled=" + isEloMatchingEnabled +
                ", isBanned=" + isBanned +
                '}';
    }
}