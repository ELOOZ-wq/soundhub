package ui;

import javafx.application.Application;
import javafx.scene.Scene;
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
        showLoginView();
        stage.show();
    }

    private void showLoginView() {
        LoginView view = new LoginView(controller, this::showDashboardView);
        Scene scene = new Scene(view, 1100, 700);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    private void showDashboardView(User user) {
        DashboardView dashboardView = new DashboardView(controller, user, this::showLoginView);
        Scene scene = new Scene(dashboardView, 1380, 860);
        applyTheme(scene);
        primaryStage.setScene(scene);
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

