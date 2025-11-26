package model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Favorite {
    private final User user;
    private final Track track;
    private final LocalDateTime addedAt;

    public Favorite(User user, Track track, LocalDateTime addedAt) {
        this.user = user;
        this.track = track;
        this.addedAt = addedAt;
    }

    public User getUser() {
        return user;
    }

    public Track getTrack() {
        return track;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Favorite favorite)) return false;
        return Objects.equals(user, favorite.user) && Objects.equals(track, favorite.track);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, track);
    }

    @Override
    public String toString() {
        return track.toString();
    }
}
