package dao;

import model.Playlist;
import model.Track;
import model.User;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaylistDAO {

    private final UserDAO userDAO;
    private final TrackDAO trackDAO;

    public PlaylistDAO(UserDAO userDAO, TrackDAO trackDAO) {
        this.userDAO = userDAO;
        this.trackDAO = trackDAO;
    }

    public List<Playlist> findAll() {
        List<Playlist> playlists = new ArrayList<>();
        String sql = "SELECT * FROM playlist";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                playlists.add(mapResultSetToPlaylist(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des playlists", e);
        }
        
        return playlists;
    }

    public Optional<Playlist> findById(int id) {
        String sql = "SELECT * FROM playlist WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToPlaylist(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de la playlist", e);
        }
        
        return Optional.empty();
    }

    public List<Playlist> findByUser(User user) {
        String sql = "SELECT * FROM playlist WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            
            List<Playlist> playlists = new ArrayList<>();
            while (rs.next()) {
                playlists.add(mapResultSetToPlaylist(rs));
            }
            return playlists;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des playlists par utilisateur", e);
        }
    }

    public Playlist save(Playlist playlist) {
        if (playlist.getId() == 0) {
            return insert(playlist);
        } else {
            return update(playlist);
        }
    }

    private Playlist insert(Playlist playlist) {
        String sql = "INSERT INTO playlist (name, user_id) VALUES (?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, playlist.getName());
            stmt.setInt(2, playlist.getOwner().getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Échec de la création de la playlist");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return new Playlist(id, playlist.getName(), playlist.getOwner());
                } else {
                    throw new SQLException("Échec de la création de la playlist, aucun ID généré");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de la playlist", e);
        }
    }

    private Playlist update(Playlist playlist) {
        String sql = "UPDATE playlist SET name = ? WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playlist.getName());
            stmt.setInt(2, playlist.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Playlist non trouvée pour la mise à jour");
            }
            
            return playlist;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la playlist", e);
        }
    }

    public void delete(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            // Supprimer d'abord les associations playlist-track
            String deleteTracksSQL = "DELETE FROM playlisttrack WHERE playlist_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteTracksSQL)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            // Puis supprimer la playlist
            String deletePlaylistSQL = "DELETE FROM playlist WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deletePlaylistSQL)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la playlist", e);
        }
    }

    public void addTrackToPlaylist(int playlistId, int trackId) {
        String sql = "INSERT IGNORE INTO playlisttrack (playlist_id, track_id) VALUES (?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, playlistId);
            stmt.setInt(2, trackId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du track à la playlist", e);
        }
    }

    public void removeTrackFromPlaylist(int playlistId, int trackId) {
        String sql = "DELETE FROM playlisttrack WHERE playlist_id = ? AND track_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, playlistId);
            stmt.setInt(2, trackId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du track de la playlist", e);
        }
    }

    public List<Track> getPlaylistTracks(int playlistId) {
        String sql = "SELECT t.* FROM track t " +
                    "INNER JOIN playlisttrack pt ON t.id = pt.track_id " +
                    "WHERE pt.playlist_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, playlistId);
            ResultSet rs = stmt.executeQuery();
            
            List<Track> tracks = new ArrayList<>();
            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
            return tracks;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tracks de la playlist", e);
        }
    }

    private Playlist mapResultSetToPlaylist(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        User user = userDAO.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé pour la playlist"));
        
        return new Playlist(
            rs.getInt("id"),
            rs.getString("name"),
            user
        );
    }

    private Track mapResultSetToTrack(ResultSet rs) throws SQLException {
        int artistId = rs.getInt("artist_id");
        User artist = userDAO.findById(artistId)
            .orElseThrow(() -> new RuntimeException("Artiste non trouvé pour le track"));
        
        return new Track(
            rs.getInt("id"),
            rs.getString("title"),
            artist,
            rs.getString("album"),
            java.time.Duration.ofSeconds(rs.getLong("duration")),
            rs.getString("file_path"),
            model.TrackStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("upload_date").toLocalDateTime()
        );
    }
}
