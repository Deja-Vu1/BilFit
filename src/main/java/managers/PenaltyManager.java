package managers;

import models.Reservation;
import models.Student;

public class PenaltyManager {

    private static final double SUSPENSION_THRESHOLD = 50.0;
    private static final int MAX_PENALTY_POINTS = 3;

    public void processNoShow(Student student, Reservation reservation) {
        reservation.markAttendance(false);
        student.addPenaltyPoint(1);
        checkSuspensionStatus(student);
    }

    public void processAttendance(Student student, Reservation reservation) {
        reservation.markAttendance(true);
    }

    private void checkSuspensionStatus(Student student) {
        if (student.getReliabilityScore() < SUSPENSION_THRESHOLD || student.getPenaltyPoints() >= MAX_PENALTY_POINTS) {
            student.setPublicProfile(false); // burası tekrardan düzenlenmeli !!!!!!!!
        }
    }
}