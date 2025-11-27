package ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import model.Track;

import java.io.File;

public class MiniPlayer extends VBox {

    private final Label nowPlaying = new Label("Sélectionne un morceau");
    private final Slider progressSlider = new Slider(0, 1, 0);
    private final Slider volumeSlider = new Slider(0, 100, 70);
    private final Label elapsedLabel = new Label("00:00");
    private final Label totalLabel = new Label("00:00");
    private Timeline timeline;
    private Track currentTrack;
    private double trackDurationSeconds = 0d;
    private MediaPlayer mediaPlayer;


    public MiniPlayer() {
        setPadding(new Insets(16));
        setSpacing(12);
        getStyleClass().add("mini-player");
        build();
    }

    private void build() {
        nowPlaying.getStyleClass().add("now-playing");

        progressSlider.setDisable(true);
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                elapsedLabel.setText(formatSeconds(newVal.doubleValue())));

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue() / 100.0);


            }
        });


        Button playButton = new Button("Lecture");
        playButton.setOnAction(event -> play());
        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(event -> pause());
        Button stopButton = new Button("Stop");
        stopButton.setOnAction(event -> stop());

        HBox actions = new HBox(10, playButton, pauseButton, stopButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        HBox progressRow = new HBox(10, elapsedLabel, progressSlider, totalLabel);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        HBox volumeRow = new HBox(10, new Label("Volume"), volumeSlider);
        volumeRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(nowPlaying, progressRow, actions, volumeRow);
    }

    public void loadTrack(Track track) {
        stop(); // stop timeline ou mediaPlayer précédent
        currentTrack = track;
        if (track == null) {
            nowPlaying.setText("Sélectionne un morceau");
            progressSlider.setDisable(true);
            totalLabel.setText("00:00");
            return;
        }

        Media media = new Media(new File(track.getFilePath()).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            trackDurationSeconds = media.getDuration().toSeconds();
            progressSlider.setDisable(false);
            progressSlider.setValue(0);
            progressSlider.setMax(trackDurationSeconds);
            totalLabel.setText(track.formattedDuration());
        });

        progressSlider.setDisable(false);
        progressSlider.setValue(0);
        progressSlider.setMax(trackDurationSeconds);

        nowPlaying.setText(track.getTitle() + " • " + track.getArtistName());
        totalLabel.setText(track.formattedDuration());
        elapsedLabel.setText("00:00");

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            progressSlider.setValue(newTime.toSeconds());
            elapsedLabel.setText(formatSeconds(newTime.toSeconds()));
        });
    }


    public void play() {
        if (currentTrack == null || mediaPlayer == null) {
            nowPlaying.setText("Choisis un morceau pour lancer la lecture");
            return;
        }
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
            mediaPlayer = null;
        }
        if (progressSlider != null) {
            progressSlider.setValue(0);
        }
        elapsedLabel.setText("00:00");
    }

    private void startTimeline() {
        if (trackDurationSeconds <= 0) {
            return;
        }
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressSlider.valueProperty(), 0)),
                new KeyFrame(Duration.seconds(trackDurationSeconds),
                        new KeyValue(progressSlider.valueProperty(), trackDurationSeconds))
        );
        timeline.setOnFinished(event -> stop());
        timeline.play();
    }

    private String formatSeconds(double seconds) {
        long totalSeconds = Math.round(seconds);
        long minutes = totalSeconds / 60;
        long remain = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remain);
    }
}

