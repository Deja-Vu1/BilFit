package managers;

import database.Database;
import models.Student;
import java.util.ArrayList;
import java.util.List;

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
        List<Student> allPublicStudents = db.getAllPublicStudents();
        List<Student> recommendedFriends = new ArrayList<>();

        if (allPublicStudents == null || allPublicStudents.isEmpty()) {
            return recommendedFriends;
        }

        for (Student other : allPublicStudents) {
            if (targetStudent.getStudentId().equals(other.getStudentId())) {
                continue;
            }

            double similarityScore = targetStudent.calculateJaccardSimilarity(other);

            if (similarityScore >= threshold) {
                recommendedFriends.add(other);
            }
        }

        return recommendedFriends;
    }
}