package models;

public enum SportType {
    BASKETBALL(40),
    FOOTBALL(90),
    BASEBALL(180),
    TENNIS(120),
    GYM(60),
    VOLLEYBALL(90),
    SWIMMING(60),
    US_FOOTBALL(180);

    private final int durationMinutes;

    SportType(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }
}