package model;

public enum TrackStatus {
    PENDING("En attente"),
    APPROVED("Approuvé"),
    REJECTED("Rejeté");

    private final String label;

    TrackStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

