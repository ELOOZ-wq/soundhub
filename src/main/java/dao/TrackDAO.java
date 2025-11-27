package dao;

import model.Track;
import model.TrackStatus;
import model.User;
import utils.DBConnection;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrackDAO {

    private final UserDAO userDAO;

    public TrackDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public List<Track> findAll() {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT * FROM track";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tracks", e);
        }
        
        return tracks;
    }

    public Optional<Track> findById(int id) {
        String sql = "SELECT * FROM track WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToTrack(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du track", e);
        }
        
        return Optional.empty();
    }

    public List<Track> findByStatus(TrackStatus status) {
        String sql = "SELECT * FROM track WHERE status = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            
            List<Track> tracks = new ArrayList<>();
            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
            return tracks;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tracks par statut", e);
        }
    }

    public List<Track> findByArtist(User artist) {
        String sql = "SELECT * FROM track WHERE artist_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, artist.getId());
            ResultSet rs = stmt.executeQuery();
            
            List<Track> tracks = new ArrayList<>();
            while (rs.next()) {
                tracks.add(mapResultSetToTrack(rs));
            }
            return tracks;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tracks par artiste", e);
        }
    }

    public Track save(Track track) {
        if (track.getId() == 0) {
            return insert(track);
        } else {
            return update(track);
        }
    }

    private Track insert(Track track) {
        String sql = "INSERT INTO track (title, artist_id, album, duration, file_path, status, upload_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, track.getTitle());
            stmt.setInt(2, track.getArtist().getId());
            stmt.setString(3, track.getAlbum());
            stmt.setLong(4, track.getDuration().toSeconds());
            stmt.setString(5, track.getFilePath());
            stmt.setString(6, track.getStatus().name());
            stmt.setTimestamp(7, Timestamp.valueOf(track.getUploadDate()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Échec de la création du track");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return new Track(id, track.getTitle(), track.getArtist(), track.getAlbum(),
                                   track.getDuration(), track.getFilePath(), track.getStatus(), track.getUploadDate());
                } else {
                    throw new SQLException("Échec de la création du track, aucun ID généré");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du track", e);
        }
    }

    private Track update(Track track) {
        String sql = "UPDATE track SET title = ?, album = ?, status = ? WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, track.getTitle());
            stmt.setString(2, track.getAlbum());
            stmt.setString(3, track.getStatus().name());
            stmt.setInt(4, track.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Track non trouvé pour la mise à jour");
            }
            
            return track;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du track", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM track WHERE id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du track", e);
        }
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
            Duration.ofSeconds(rs.getLong("duration")),
            rs.getString("file_path"),
            TrackStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("upload_date").toLocalDateTime()
        );
    }
}
