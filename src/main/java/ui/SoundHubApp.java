package ui;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import model.User;
import ui.views.DashboardView;
import ui.views.LoginView;

import java.net.URL;

public class SoundHubApp extends Application {

    private final SoundHubController controller = new SoundHubController();
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("SoundHub");
        stage.setResizable(true);
        showLoginView();
        stage.centerOnScreen();
        stage.show();
    }

    private void showLoginView() {
        LoginView view = new LoginView(controller, this::showDashboardView);
        
        // Calculer la taille adaptée à l'écran
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1100, screenBounds.getWidth() * 0.8);
        double height = Math.min(700, screenBounds.getHeight() * 0.8);
        
        Scene scene = new Scene(view, width, height);
        applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
    }

    private void showDashboardView(User user) {
        DashboardView dashboardView = new DashboardView(controller, user, this::showLoginView);
        
        // Calculer la taille adaptée à l'écran
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1380, screenBounds.getWidth() * 0.9);
        double height = Math.min(860, screenBounds.getHeight() * 0.9);
        
        Scene scene = new Scene(dashboardView, width, height);
        applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.centerOnScreen();
    }

    private void applyTheme(Scene scene) {
        URL css = getClass().getResource("/ui/styles.css");
        if (css != null) {
            String stylesheet = css.toExternalForm();
            if (!scene.getStylesheets().contains(stylesheet)) {
                scene.getStylesheets().add(stylesheet);
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}

