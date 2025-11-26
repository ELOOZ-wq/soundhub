package model;

/**
 * Application roles with helper methods for permission checks.
 */
public enum Role {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Admin"),
    USER("User");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public boolean canModerateUsers() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean canManageAdmins() {
        return this == SUPER_ADMIN;
    }

    public boolean canUploadTracks() {
        return this == USER || this == ADMIN;
    }

    public boolean canModerateTracks() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    @Override
    public String toString() {
        return label;
    }
}

