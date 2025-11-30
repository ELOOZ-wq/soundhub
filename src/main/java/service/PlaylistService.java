package service;

import dao.PlaylistDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Playlist;
import model.Track;
import model.User;
import utils.ValidationUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service pour la gestion des playlists, utilisant PlaylistDAO pour la persistance.
 */
public class PlaylistService {

    // Cache pour stocker les playlists par utilisateur pour une utilisation rapide par JavaFX
    private final Map<Integer, ObservableList<Playlist>> playlistsByUserCache = new ConcurrentHashMap<>();
    private final PlaylistDAO playlistDAO;

    // Le service dépend désormais uniquement du DAO
    public PlaylistService(PlaylistDAO playlistDAO) {
        this.playlistDAO = playlistDAO;
    }

    /**
     * Retourne les playlists d'un utilisateur. Charge depuis la DB si non cachées.
     */
    public ObservableList<Playlist> getPlaylists(User user) {
        Objects.requireNonNull(user, "Utilisateur requis");

        // Utilise computeIfAbsent pour charger de la DB seulement si le cache est vide pour cet utilisateur
        ObservableList<Playlist> cache = playlistsByUserCache.computeIfAbsent(user.getId(), id -> {
            // 1. Charger les playlists (métadonnées) depuis la DB
            List<Playlist> dbPlaylists = playlistDAO.findByUser(user);

            // 2. Pour chaque playlist, charger les tracks associés et les ajouter à l'ObservableList interne
            for (Playlist p : dbPlaylists) {
                List<Track> tracks = playlistDAO.getPlaylistTracks(p.getId());
                p.getTracks().addAll(tracks);
            }

            return FXCollections.observableArrayList(dbPlaylists);
        });

        return cache;
    }

    public Playlist createPlaylist(User owner, String name) {
        ValidationUtils.require(ValidationUtils.isNotBlank(name), "Nom de playlist requis.");

        // 1. Persistance en DB (le DAO gère l'insertion et l'obtention de l'ID)
        Playlist newPlaylistMetadata = new Playlist(0, name, owner);
        Playlist persistedPlaylist = playlistDAO.save(newPlaylistMetadata);

        // 2. Mise à jour du cache JavaFX
        getPlaylists(owner).add(persistedPlaylist);

        return persistedPlaylist;
    }

    public void deletePlaylist(User owner, Playlist playlist) {
        // 1. Suppression en DB (le DAO gère la suppression dans la table de jointure et la playlist)
        playlistDAO.delete(playlist.getId());

        // 2. Mise à jour du cache JavaFX
        getPlaylists(owner).remove(playlist);

        // 3. Nettoyage du cache si l'utilisateur n'a plus de playlist
        if (getPlaylists(owner).isEmpty()) {
            playlistsByUserCache.remove(owner.getId());
        }
    }

    public void renamePlaylist(Playlist playlist, String newName) {
        ValidationUtils.require(ValidationUtils.isNotBlank(newName), "Le nom ne peut être vide.");

        // 1. Mise à jour du modèle
        playlist.setName(newName);

        // 2. Persistance en DB
        playlistDAO.save(playlist);

        // 3. Le cache est mis à jour automatiquement car l'objet Playlist est partagé
    }

    public void addTrack(Playlist playlist, Track track) {
        // 1. Persistance en DB
        playlistDAO.addTrackToPlaylist(playlist.getId(), track.getId());

        // 2. Mise à jour du cache JavaFX
        playlist.addTrack(track);
    }

    public void removeTrack(Playlist playlist, Track track) {
        // 1. Suppression en DB
        playlistDAO.removeTrackFromPlaylist(playlist.getId(), track.getId());

        // 2. Mise à jour du cache JavaFX
        playlist.removeTrack(track);
    }
}