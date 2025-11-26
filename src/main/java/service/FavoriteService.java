package service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Favorite;
import model.Track;
import model.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FavoriteService {

    private final Map<Integer, ObservableList<Favorite>> favoritesByUser = new HashMap<>();

    public FavoriteService(TrackService trackService, UserService userService) {
        seed(trackService, userService);
    }

    private void seed(TrackService trackService, UserService userService) {
        userService.getUsers().stream()
                .filter(User::isActive)
                .findFirst()
                .ifPresent(user -> trackService.getTracks().stream()
                        .limit(1)
                        .forEach(track -> toggleFavorite(user, track)));
    }

    public ObservableList<Favorite> getFavorites(User user) {
        Objects.requireNonNull(user, "Utilisateur requis");
        return favoritesByUser.computeIfAbsent(user.getId(), id -> FXCollections.observableArrayList());
    }

    public void toggleFavorite(User user, Track track) {
        ObservableList<Favorite> favorites = getFavorites(user);
        favorites.stream()
                .filter(favorite -> favorite.getTrack().equals(track))
                .findFirst()
                .ifPresentOrElse(favorites::remove,
                        () -> favorites.add(new Favorite(user, track, LocalDateTime.now())));
    }

    public boolean isFavorite(User user, Track track) {
        return getFavorites(user).stream().anyMatch(favorite -> favorite.getTrack().equals(track));
    }
}
