package managers;

import java.util.ArrayList;
import java.util.List;

import database.Database;
import models.SportType;
import models.Student;

public class RecommendationManager {

    private static final double DEFAULT_THRESHOLD = 0.3;
    private Database db;

    public RecommendationManager(Database db) {
        this.db = db;
    }

    public List<Student> getRecommendations(Student targetStudent) {
        return getRecommendations(targetStudent, DEFAULT_THRESHOLD);
    }

    public List<Student> getRecommendations(Student targetStudent, double threshold) {
        List<Student> recommendedFriends = new ArrayList<>();
        if (targetStudent == null || threshold < 0.0 || threshold > 1.0) return recommendedFriends;

        List<Student> allPublicStudents = db.getAllPublicStudents();
        if (allPublicStudents == null || allPublicStudents.isEmpty()) {
            return recommendedFriends;
        }

        for (Student other : allPublicStudents) {
            if (other == null || targetStudent.getBilkentEmail().equals(other.getBilkentEmail())) {
                continue;
            }
            
            boolean isAlreadyConnected = false;
            
            // 1. Zaten arkadaş olup olmadıklarını kontrol et
            if (targetStudent.getFriends() != null) {
                for (Student friend : targetStudent.getFriends()) {
                    if(friend.getBilkentEmail().equals(other.getBilkentEmail())) {
                        isAlreadyConnected = true;
                        break;
                    }
                }
            }
            
            // 2. Gelen isteklerde var mı kontrol et (Eski hatalı kısım burasıydı, düzeltildi)
            if(!isAlreadyConnected && targetStudent.getIncomingFriendRequests() != null) {
                for (Student request : targetStudent.getIncomingFriendRequests()) {
                    if(request.getBilkentEmail().equals(other.getBilkentEmail())) {
                        isAlreadyConnected = true;
                        break;
                    }
                }
            }

            // 3. Giden isteklerde var mı kontrol et
            if(!isAlreadyConnected && targetStudent.getOutgoingFriendRequests() != null) {
                for (Student request : targetStudent.getOutgoingFriendRequests()) {
                    if(request.getBilkentEmail().equals(other.getBilkentEmail())) {
                        isAlreadyConnected = true;
                        break;
                    }
                }
            }
            
            // Eğer zaten bir bağ varsa (arkadaş, gelen veya giden istek) önerme
            if(isAlreadyConnected) {
                continue;
            }

            double similarityScore = calculateJaccardSimilarity(targetStudent, other);

            if (similarityScore >= threshold) {
                recommendedFriends.add(other);
            }
        }
        return recommendedFriends;
    }

    private double calculateJaccardSimilarity(Student s1, Student s2) {
        List<SportType> interests1 = s1.getInterests();
        List<SportType> interests2 = s2.getInterests();

        if (interests1 == null || interests2 == null || (interests1.isEmpty() && interests2.isEmpty())) {
            return 0.0;
        }

        int intersectionScore = 0;
        for (SportType st : interests1) {
            if (interests2.contains(st)) {
                intersectionScore++;
            }
        }

        int unionScore = interests1.size() + interests2.size() - intersectionScore;
        return (double) intersectionScore / unionScore;
    }
}