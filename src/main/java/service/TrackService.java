package service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Role;
import model.Track;
import model.TrackStatus;
import model.User;
import utils.ValidationUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TrackService {

    private final ObservableList<Track> tracks = FXCollections.observableArrayList();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    public TrackService(UserService userService) {
        seed(userService);
    }

    private void seed(UserService userService) {
        List<User> artists = userService.getUsers().stream()
                .filter(User::isActive)
                .filter(user -> user.getRole() != Role.SUPER_ADMIN)
                .collect(Collectors.toList());
        if (artists.isEmpty()) {
            return;
        }
        User firstArtist = artists.get(0);
        User secondArtist = artists.get(artists.size() > 1 ? 1 : 0);

        tracks.add(new Track(idSequence.getAndIncrement(), "City Lights", firstArtist,
                "Neon Dreams", Duration.ofSeconds(222), "media/city_lights.mp3",
                TrackStatus.APPROVED, LocalDateTime.now().minusDays(2)));
        tracks.add(new Track(idSequence.getAndIncrement(), "Orbit", secondArtist,
                "Space Walk", Duration.ofSeconds(187), "media/orbit.mp3",
                TrackStatus.APPROVED, LocalDateTime.now().minusDays(1)));
        tracks.add(new Track(idSequence.getAndIncrement(), "Slow Dive", firstArtist,
                "After Hours", Duration.ofSeconds(250), "media/slow_dive.mp3",
                TrackStatus.PENDING, LocalDateTime.now().minusHours(6)));
    }

    public ObservableList<Track> getTracks() {
        return FXCollections.unmodifiableObservableList(tracks);
    }

    public Track submitTrack(User artist, String title, String album, Duration duration, String filePath) {
        Objects.requireNonNull(artist, "Artiste requis");
        ValidationUtils.require(ValidationUtils.isNotBlank(title), "Le titre est requis.");
        ValidationUtils.require(duration != null && !duration.isZero(), "Durée invalide.");
        ValidationUtils.require(ValidationUtils.isNotBlank(filePath), "Chemin du fichier requis.");

        Track track = new Track(
                idSequence.getAndIncrement(),
                title,
                artist,
                album,
                duration,
                filePath,
                TrackStatus.PENDING,
                LocalDateTime.now()
        );
        tracks.add(track);
        return track;
    }

    public void changeStatus(Track track, TrackStatus status) {
        track.setStatus(status);
    }

    public List<Track> getPendingTracks() {
        return tracks.stream()
                .filter(t -> t.getStatus() == TrackStatus.PENDING)
                .collect(Collectors.toList());
    }

    public List<Track> getTracksByStatus(TrackStatus status) {
        return tracks.stream()
                .filter(t -> status == null || t.getStatus() == status)
                .collect(Collectors.toList());
    }
}
