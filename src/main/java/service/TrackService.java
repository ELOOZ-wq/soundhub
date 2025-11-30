package service;

import dao.TrackDAO;
import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.*;
import utils.ValidationUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TrackService {

    private final TrackDAO trackDAO;
    private final ObservableList<Track> tracks = FXCollections.observableArrayList();

    public TrackService(UserService userService) {
        UserDAO userDAO = new UserDAO();
        this.trackDAO = new TrackDAO(userDAO);
        loadTracks();
        seedIfEmpty(userService);
    }

    private void loadTracks() {
        tracks.clear();
        tracks.addAll(trackDAO.findAll());
    }

    private void seedIfEmpty(UserService userService) {
        if (tracks.isEmpty()) {
            List<User> artists = userService.getUsers().stream()
                    .filter(User::isActive)
                    .filter(user -> user.getRole() != Role.SUPER_ADMIN)
                    .collect(Collectors.toList());
            if (artists.isEmpty()) {
                return;
            }
            User firstArtist = artists.get(0);
            User secondArtist = artists.size() > 1 ? artists.get(1) : firstArtist;

            Track track1 = new Track(0, "City Lights", firstArtist,
                    "Neon Dreams", Duration.ofSeconds(222), "media/city_lights.mp3",
                    TrackStatus.APPROVED, LocalDateTime.now().minusDays(2));
            Track track2 = new Track(0, "Orbit", secondArtist,
                    "Space Walk", Duration.ofSeconds(187), "media/orbit.mp3",
                    TrackStatus.APPROVED, LocalDateTime.now().minusDays(1));
            Track track3 = new Track(0, "Slow Dive", firstArtist,
                    "After Hours", Duration.ofSeconds(250), "media/slow_dive.mp3",
                    TrackStatus.PENDING, LocalDateTime.now().minusHours(6));

            tracks.add(trackDAO.save(track1));
            tracks.add(trackDAO.save(track2));
            tracks.add(trackDAO.save(track3));
        }
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
                0,
                title,
                artist,
                album,
                duration,
                filePath,
                TrackStatus.PENDING,
                LocalDateTime.now()
        );
        Track savedTrack = trackDAO.save(track);
        tracks.add(savedTrack);
        return savedTrack;
    }

    public void changeStatus(Track track, TrackStatus status) {
        track.setStatus(status);
        trackDAO.save(track);
        loadTracks(); // Recharger la liste
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
    public List<Album> getPopularAlbums() {
        // 1. Filtrer uniquement les tracks APPROUVÉS
        List<Track> approvedTracks = tracks.stream()
                .filter(t -> t.getStatus() == TrackStatus.APPROVED)
                .toList();

        // 2. Regrouper les tracks par Nom d'Album et Artiste.
        // La clé de la Map sera un regroupement logique de l'album (Artiste + Nom d'Album)
        Map<String, List<Track>> groupedTracks = approvedTracks.stream()
                .collect(Collectors.groupingBy(track -> {
                    String albumName = track.getAlbum() != null && !track.getAlbum().isBlank()
                            ? track.getAlbum()
                            : "Singles de " + track.getArtistName(); // Traite les singles
                    return albumName + "|||" + track.getArtist().getId(); // Utilise l'ID de l'artiste pour l'unicité
                }));

        // 3. Convertir la Map en liste d'objets Album
        return groupedTracks.values().stream()
                .map(trackList -> {
                    // Récupère les métadonnées de l'album à partir de la première piste
                    Track representativeTrack = trackList.getFirst();
                    String albumTitle = representativeTrack.getAlbum() != null && !representativeTrack.getAlbum().isBlank()
                            ? representativeTrack.getAlbum()
                            : "Singles de " + representativeTrack.getArtistName();

                    return new Album(albumTitle, representativeTrack.getArtist(), trackList);
                })
                .sorted((a1, a2) -> Integer.compare(a2.getTracks().size(), a1.getTracks().size())) // Trie par popularité (nombre de titres)
                .limit(10) // Limite l'affichage aux 10 premiers (pour le carrousel)
                .toList();
    }
}
