package service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Playlist;
import model.Track;
import model.TrackStatus;
import model.User;
import utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class PlaylistService {

    private final Map<Integer, ObservableList<Playlist>> playlistsByUser = new HashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    public PlaylistService(TrackService trackService, UserService userService) {
        seed(trackService, userService);
    }

    private void seed(TrackService trackService, UserService userService) {
        userService.getUsers().stream()
                .filter(User::isActive)
                .findFirst()
                .ifPresent(user -> {
                    Playlist playlist = new Playlist(idSequence.getAndIncrement(), "Chill Vibes", user);
                    trackService.getTracks().stream()
                            .filter(track -> track.getStatus() == TrackStatus.APPROVED)
                            .limit(2)
                            .forEach(playlist::addTrack);
                    getPlaylists(user).add(playlist);
                });
    }

    public ObservableList<Playlist> getPlaylists(User user) {
        Objects.requireNonNull(user, "Utilisateur requis");
        return playlistsByUser.computeIfAbsent(user.getId(), id -> FXCollections.observableArrayList());
    }

    public Playlist createPlaylist(User owner, String name) {
        ValidationUtils.require(ValidationUtils.isNotBlank(name), "Nom de playlist requis.");
        Playlist playlist = new Playlist(idSequence.getAndIncrement(), name, owner);
        getPlaylists(owner).add(playlist);
        return playlist;
    }

    public void deletePlaylist(User owner, Playlist playlist) {
        getPlaylists(owner).remove(playlist);
    }

    public void renamePlaylist(Playlist playlist, String newName) {
        ValidationUtils.require(ValidationUtils.isNotBlank(newName), "Le nom ne peut être vide.");
        playlist.setName(newName);
    }

    public void addTrack(Playlist playlist, Track track) {
        playlist.addTrack(track);
    }

    public void removeTrack(Playlist playlist, Track track) {
        playlist.removeTrack(track);
    }
}
