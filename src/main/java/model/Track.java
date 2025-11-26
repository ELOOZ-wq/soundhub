package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Track {
    private final int id;
    private final String title;
    private final User artist;
    private final String album;
    private final Duration duration;
    private final String filePath;
    private TrackStatus status;
    private final LocalDateTime uploadDate;

    public Track(int id,
                 String title,
                 User artist,
                 String album,
                 Duration duration,
                 String filePath,
                 TrackStatus status,
                 LocalDateTime uploadDate) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.filePath = filePath;
        this.status = status;
        this.uploadDate = uploadDate;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public User getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public TrackStatus getStatus() {
        return status;
    }

    public void setStatus(TrackStatus status) {
        this.status = status;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public String getArtistName() {
        return artist.getUsername();
    }

    public String formattedDuration() {
        long seconds = duration.toSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public String formattedUploadDate() {
        return uploadDate.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track track)) return false;
        return id == track.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " - " + getArtistName();
    }
}
