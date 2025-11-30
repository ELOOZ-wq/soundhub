package model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.Objects;

public class Album {
    private final String title;
    private final User artist;
    private final ObservableList<Track> tracks;

    public Album(String title, User artist, List<Track> tracks) {
        this.title = title;
        this.artist = artist;
        this.tracks = FXCollections.observableArrayList(tracks);
    }

    public String getTitle() {
        return title;
    }

    public User getArtist() {
        return artist;
    }

    public String getArtistName() {
        // Assure l'accès au nom de l'artiste depuis l'objet User
        return artist.getUsername();
    }

    public ObservableList<Track> getTracks() {
        return tracks;
    }

    // Pour l'affichage si vous en avez besoin
    @Override
    public String toString() {
        return title + " (" + tracks.size() + " titres)";
    }

    /**
     * Permet de considérer deux albums avec le même titre et artiste comme identiques
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vérifie si l'objet est une instance d'Album (et gère le cas null)
        if (!(o instanceof Album album)) return false;

        // Retourne true si le titre et l'artiste correspondent
        return Objects.equals(title, album.title) && Objects.equals(artist, album.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist);
    }
}