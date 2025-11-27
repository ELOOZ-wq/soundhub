package service;

import dao.FavoriteDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Favorite;
import model.Track;
import model.User;

import java.time.LocalDateTime;
import java.util.List;

public class FavoriteService {

    private final FavoriteDAO favoriteDAO;

    // Cache en mémoire pour affichage instantané
    private final ObservableList<Favorite> favoritesCache = FXCollections.observableArrayList();

    public FavoriteService(FavoriteDAO favoriteDAO) {
        this.favoriteDAO = favoriteDAO;
    }

    // Charge les favoris depuis la base dans le cache (au démarrage)
    public void loadFavorites(User user) {
        List<Favorite> favoritesFromDB = favoriteDAO.findByUser(user);
        favoritesCache.setAll(favoritesFromDB);
    }

    // Retourne le cache pour lier à la ListView
    public ObservableList<Favorite> getFavoritesCache() {
        return favoritesCache;
    }

    // Toggle favori : met à jour le cache et la base
    public void toggleFavorite(User user, Track track) {
        boolean exists = favoritesCache.stream()
                .anyMatch(fav -> fav.getTrack().equals(track));

        if (exists) {
            // Supprime du cache et de la base
            favoritesCache.removeIf(fav -> fav.getTrack().equals(track));
        } else {
            // Ajoute au cache
            favoritesCache.add(new Favorite(user, track, LocalDateTime.now()));
        }

        // Mise à jour en base
        favoriteDAO.toggleFavorite(user, track);
    }

    // Vérifie si un track est favori
    public boolean isFavorite(User user, Track track) {
        return favoritesCache.stream()
                .anyMatch(fav -> fav.getTrack().equals(track));
    }
}
