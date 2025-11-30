package ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.User;
import service.FavoriteService;
import service.PlaylistService;
import service.TrackService;
import service.UserService;
import dao.*; // Import nécessaire pour les classes DAO

public class SoundHubController {

    private final UserService userService;
    private final TrackService trackService;
    private final PlaylistService playlistService;
    private final FavoriteService favoriteService;
    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();

    public SoundHubController() {
        // Initialisation des DAOs nécessaires pour les dépendances
        UserDAO userDAO = new UserDAO();
        TrackDAO trackDAO = new TrackDAO(userDAO);
        PlaylistDAO playlistDAO = new PlaylistDAO(userDAO, trackDAO); // Instanciation du PlaylistDAO
        FavoriteDAO favoriteDAO = new FavoriteDAO(userDAO, trackDAO);

        // Initialisation des Services
        this.userService = new UserService();
        this.trackService = new TrackService(userService);
        // MODIFICATION ICI: Passage de l'instance de PlaylistDAO
        this.playlistService = new PlaylistService(playlistDAO);
        this.favoriteService = new FavoriteService(favoriteDAO);
    }

    public LoginResult login(String login, String password) {
        try {
            User user = userService.authenticate(login, password);
            currentUser.set(user);

            // IMPORTANT : Charger les playlists et favoris APRES le login
            favoriteService.loadFavorites(user);
            playlistService.getPlaylists(user); // Force le chargement/mise en cache des playlists

            return new LoginResult(true, user, "Connexion réussie.");
        } catch (RuntimeException ex) {
            return new LoginResult(false, null, ex.getMessage());
        }
    }

    // ... (Reste de la classe inchangé) ...

    public LoginResult register(String username, String email, String password) {
        try {
            User user = userService.registerUser(username, email, password);
            String message = "Compte créé. Merci d'attendre la validation par un ADMIN.";
            return new LoginResult(true, user, message);
        } catch (RuntimeException ex) {
            return new LoginResult(false, null, ex.getMessage());
        }
    }

    public void logout() {
        currentUser.set(null);
    }

    public UserService getUserService() {
        return userService;
    }

    public TrackService getTrackService() {
        return trackService;
    }

    public PlaylistService getPlaylistService() {
        return playlistService;
    }

    public FavoriteService getFavoriteService() {
        return favoriteService;
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUser;
    }

    public record LoginResult(boolean success, User user, String message) {
    }
}