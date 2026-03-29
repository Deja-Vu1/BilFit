package managers;

import models.Student;
import java.util.ArrayList;
import java.util.List;

public class RecommendationManager {

    private static final double DEFAULT_THRESHOLD = 0.3;

    public List<Student> getRecommendations(Student targetStudent, List<Student> allStudents) {
        return getRecommendations(targetStudent, allStudents, DEFAULT_THRESHOLD);
    }

    public List<Student> getRecommendations(Student targetStudent, List<Student> allStudents, double threshold) {
        List<Student> recommendedFriends = new ArrayList<>();

        for (Student other : allStudents) {
            if (targetStudent.getStudentId().equals(other.getStudentId())) {
                continue;
            }

            if (!other.isPublicProfile()) {
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