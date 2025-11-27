package dao;

import model.Favorite;
import model.Track;
import model.User;
import utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDAO {

    private final UserDAO userDAO;
    private final TrackDAO trackDAO;

    public FavoriteDAO(UserDAO userDAO, TrackDAO trackDAO) {
        this.userDAO = userDAO;
        this.trackDAO = trackDAO;
    }

    public List<Favorite> findByUser(User user) {
        String sql = "SELECT f.*, t.* FROM favorite f " +
                    "INNER JOIN track t ON f.track_id = t.id " +
                    "WHERE f.user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            
            List<Favorite> favorites = new ArrayList<>();
            while (rs.next()) {
                favorites.add(mapResultSetToFavorite(rs, user));
            }
            return favorites;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des favoris", e);
        }
    }

    public boolean isFavorite(User user, Track track) {
        String sql = "SELECT COUNT(*) FROM favorite WHERE user_id = ? AND track_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, user.getId());
            stmt.setInt(2, track.getId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la vérification du favori", e);
        }
    }

    public void addFavorite(User user, Track track) {
        String sql = "INSERT IGNORE INTO favorite (user_id, track_id) VALUES (?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, user.getId());
            stmt.setInt(2, track.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du favori", e);
        }
    }

    public void removeFavorite(User user, Track track) {
        String sql = "DELETE FROM favorite WHERE user_id = ? AND track_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, user.getId());
            stmt.setInt(2, track.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du favori", e);
        }
    }

    public void toggleFavorite(User user, Track track) {
        if (isFavorite(user, track)) {
            removeFavorite(user, track);
        } else {
            addFavorite(user, track);
        }
    }

    private Favorite mapResultSetToFavorite(ResultSet rs, User user) throws SQLException {
        // Récupérer les données du track depuis le ResultSet
        int artistId = rs.getInt("artist_id");
        User artist = userDAO.findById(artistId)
            .orElseThrow(() -> new RuntimeException("Artiste non trouvé pour le track"));
        
        Track track = new Track(
            rs.getInt("id"),
            rs.getString("title"),
            artist,
            rs.getString("album"),
            java.time.Duration.ofSeconds(rs.getLong("duration")),
            rs.getString("file_path"),
            model.TrackStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("upload_date").toLocalDateTime()
        );
        
        return new Favorite(user, track, LocalDateTime.now());
    }
}
