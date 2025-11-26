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
import javafx.util.Duration;
import model.Track;

public class MiniPlayer extends VBox {

    private final Label nowPlaying = new Label("Sélectionne un morceau");
    private final Slider progressSlider = new Slider(0, 1, 0);
    private final Slider volumeSlider = new Slider(0, 100, 70);
    private final Label elapsedLabel = new Label("00:00");
    private final Label totalLabel = new Label("00:00");
    private Timeline timeline;
    private Track currentTrack;
    private double trackDurationSeconds = 0d;

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

        volumeSlider.setPrefWidth(150);

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
        stop();
        currentTrack = track;
        if (track == null) {
            nowPlaying.setText("Sélectionne un morceau");
            progressSlider.setDisable(true);
            totalLabel.setText("00:00");
            return;
        }
        trackDurationSeconds = track.getDuration().toSeconds();
        progressSlider.setDisable(false);
        progressSlider.setValue(0);
        progressSlider.setMax(trackDurationSeconds);
        nowPlaying.setText(track.getTitle() + " • " + track.getArtistName());
        totalLabel.setText(track.formattedDuration());
        elapsedLabel.setText("00:00");
    }

    public void play() {
        if (currentTrack == null) {
            nowPlaying.setText("Choisis un morceau pour lancer la lecture");
            return;
        }
        startTimeline();
    }

    public void pause() {
        if (timeline != null) {
            timeline.pause();
        }
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
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

