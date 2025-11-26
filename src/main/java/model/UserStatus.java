package model;

public enum UserStatus {
    PENDING("En attente"),
    ACTIVE("Actif"),
    BANNED("Suspendu");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

