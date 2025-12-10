package ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import model.Track;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;

public class MiniPlayer extends VBox {

    private final Label nowPlaying = new Label("Sélectionne un morceau");
    private final Slider progressSlider = new Slider(0, 1, 0);
    private final Slider volumeSlider = new Slider(0, 100, 70);
    private final Label elapsedLabel = new Label("00:00");
    private final Label totalLabel = new Label("00:00");

    private Track currentTrack;
    private double trackDurationSeconds = 0d;
    private MediaPlayer mediaPlayer;

    // Gestion de la file d'attente (Queue)
    private ObservableList<Track> queue = FXCollections.observableArrayList();
    private int currentTrackIndex = -1;


    public MiniPlayer() {
        setPadding(new Insets(8));
        setSpacing(6);
        getStyleClass().add("mini-player");
        build();
    }

    private void build() {
        nowPlaying.getStyleClass().add("now-playing");

        // Gère l'affichage du temps écoulé
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                elapsedLabel.setText(formatSeconds(newVal.doubleValue())));

        // Gère l'avance/retour rapide (Seeking)
        progressSlider.setOnMousePressed(event -> {
            if (mediaPlayer != null) {
                mediaPlayer.pause(); // Pause pendant le drag
            }
        });

        progressSlider.setOnMouseReleased(event -> {
            if (mediaPlayer != null) {
                double newTime = progressSlider.getValue();
                mediaPlayer.seek(Duration.seconds(newTime));
                mediaPlayer.play(); // Reprendre la lecture
            }
        });

        // Gère le volume
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
            }
        });


        // Boutons de contrôle
        Button prevButton = new Button("⏮");
        prevButton.getStyleClass().add("player-control-button");
        prevButton.getStyleClass().add("prev-button");
        prevButton.setOnAction(event -> playPrevious());
        
        Button playButton = new Button("▶");
        playButton.getStyleClass().add("player-control-button");
        playButton.getStyleClass().add("play-button");
        playButton.setOnAction(event -> play());
        
        Button pauseButton = new Button("⏸");
        pauseButton.getStyleClass().add("player-control-button");
        pauseButton.getStyleClass().add("pause-button");
        pauseButton.setOnAction(event -> pause());
        
        Button nextButton = new Button("⏭");
        nextButton.getStyleClass().add("player-control-button");
        nextButton.getStyleClass().add("next-button");
        nextButton.setOnAction(event -> playNext());
        
        Button stopButton = new Button("⏹");
        stopButton.getStyleClass().add("player-control-button");
        stopButton.getStyleClass().add("stop-button");
        stopButton.setOnAction(event -> stop());

        // HBox des contrôles avec volume à côté
        elapsedLabel.getStyleClass().add("time-label");
        totalLabel.getStyleClass().add("time-label");
        progressSlider.getStyleClass().add("progress-slider");
        progressSlider.setPrefWidth(150);
        progressSlider.setMaxWidth(150);
        progressSlider.setMinWidth(150);
        HBox.setHgrow(progressSlider, Priority.NEVER);
        
        Label progressLabel = new Label("▶ Lecture");
        progressLabel.getStyleClass().add("bar-label");
        progressLabel.getStyleClass().add("progress-bar-label");
        VBox progressContainer = new VBox(4);
        progressContainer.getChildren().add(progressLabel);
        HBox progressBarRow = new HBox(6, elapsedLabel, progressSlider, totalLabel);
        progressBarRow.setAlignment(Pos.CENTER_LEFT);
        progressContainer.getChildren().add(progressBarRow);
        progressContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label volumeLabel = new Label("🔊 Volume");
        volumeLabel.getStyleClass().add("bar-label");
        volumeLabel.getStyleClass().add("volume-bar-label");
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setPrefWidth(100);
        volumeSlider.setMaxWidth(100);
        volumeSlider.setMinWidth(100);
        HBox.setHgrow(volumeSlider, Priority.NEVER);
        
        VBox volumeContainer = new VBox(4);
        volumeContainer.getChildren().add(volumeLabel);
        HBox volumeBarRow = new HBox(6, volumeSlider);
        volumeBarRow.setAlignment(Pos.CENTER_RIGHT);
        volumeContainer.getChildren().add(volumeBarRow);
        volumeContainer.setAlignment(Pos.CENTER_RIGHT);
        
        HBox barsRow = new HBox(20, progressContainer, new Region(), volumeContainer);
        barsRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(barsRow.getChildren().get(1), Priority.ALWAYS);
        
        HBox actions = new HBox(6, prevButton, playButton, pauseButton, nextButton, stopButton);
        actions.setAlignment(Pos.CENTER);
        
        // Centrer le titre aussi
        nowPlaying.setAlignment(Pos.CENTER);

        getChildren().addAll(nowPlaying, barsRow, actions);
    }


    public void loadTrack(Track track) {
        loadQueue(FXCollections.observableArrayList(track), 0);
    }



    public void loadQueue(ObservableList<Track> tracks, int startIndex) {
        // Arrête proprement l'ancien MediaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        this.queue = FXCollections.observableArrayList(tracks);
        this.currentTrackIndex = startIndex;

        if (queue.isEmpty()) return;

        currentTrack = queue.get(currentTrackIndex);
        nowPlaying.setText(currentTrack.getTitle() + " • " + currentTrack.getArtistName());

        File file = new File(currentTrack.getFilePath());
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

        mediaPlayer.setOnReady(() -> {
            double duration = media.getDuration().toSeconds();
            progressSlider.setMax(duration);
            totalLabel.setText(formatSeconds(duration));
            mediaPlayer.play();
        });

        mediaPlayer.setOnEndOfMedia(this::playNext);

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            progressSlider.setValue(newTime.toSeconds());
            elapsedLabel.setText(formatSeconds(newTime.toSeconds()));
        });
    }

    // Méthode 'play' simplifiée et corrigée
    public void play() {
        if (mediaPlayer == null) {
            nowPlaying.setText("Choisis un morceau pour lancer la lecture");
            return;
        }
        // Si le MediaPlayer est chargé, on joue. Le statut UNKNOWN passera à READY puis PLAYING.
        mediaPlayer.play();
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        progressSlider.setValue(0);
        elapsedLabel.setText("00:00");

        currentTrackIndex = -1;
    }



    public void playNext() {
        if (currentTrackIndex < queue.size() - 1) {
            currentTrackIndex++;
            loadQueue(queue, currentTrackIndex);
        } else {
            stop();
        }
    }



    public void playPrevious() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--;
            loadQueue(queue, currentTrackIndex);
        } else if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.ZERO); // redémarrer la piste actuelle
        }
    }


    private String formatSeconds(double seconds) {
        long totalSeconds = Math.round(seconds);
        long minutes = totalSeconds / 60;
        long remain = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remain);
    }
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}