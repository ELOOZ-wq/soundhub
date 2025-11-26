package ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import model.User;
import ui.SoundHubController;

import java.util.Optional;
import java.util.function.Consumer;

public class LoginView extends BorderPane {

    private final SoundHubController controller;
    private final Consumer<User> onLoginSuccess;
    private final Label feedbackLabel = new Label();

    public LoginView(SoundHubController controller, Consumer<User> onLoginSuccess) {
        this.controller = controller;
        this.onLoginSuccess = onLoginSuccess;
        setPadding(new Insets(40));
        getStyleClass().add("login-view");
        buildLayout();
    }

    private void buildLayout() {
        Label title = new Label("SoundHub");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Connecte-toi pour piloter ta musique");
        subtitle.getStyleClass().add("subtitle");

        TextField loginField = new TextField();
        loginField.setPromptText("Email ou nom d'utilisateur");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        Button loginButton = new Button("Connexion");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> attemptLogin(loginField.getText(), passwordField.getText()));

        Hyperlink registerLink = new Hyperlink("Créer un compte utilisateur");
        registerLink.setOnAction(event -> openRegisterDialog());

        feedbackLabel.getStyleClass().add("feedback");

        VBox form = new VBox(12, title, subtitle, loginField, passwordField, loginButton, registerLink, feedbackLabel);
        form.setPadding(new Insets(30));
        form.setMaxWidth(420);
        form.getStyleClass().add("login-card");

        BorderPane.setAlignment(form, Pos.CENTER);
        setCenter(form);
    }

    private void attemptLogin(String login, String password) {
        SoundHubController.LoginResult result = controller.login(login, password);
        feedbackLabel.getStyleClass().remove("error");
        feedbackLabel.setText(result.message());
        if (result.success() && result.user() != null) {
            onLoginSuccess.accept(result.user());
        } else {
            feedbackLabel.getStyleClass().add("error");
        }
    }

    private void openRegisterDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Créer un compte");
        dialog.setHeaderText("Compte utilisateur SoundHub");

        ButtonType createButtonType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe (6+ caractères)");

        VBox content = new VBox(10,
                new Label("Nom d'utilisateur"), usernameField,
                new Label("Email"), emailField,
                new Label("Mot de passe"), passwordField
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                return usernameField.getText() + "|" + emailField.getText() + "|" + passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] parts = data.split("\\|");
            if (parts.length != 3) {
                return;
            }
            SoundHubController.LoginResult creation = controller.register(parts[0], parts[1], parts[2]);
            Alert alert = new Alert(creation.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            alert.setTitle("Création de compte");
            alert.setHeaderText(null);
            alert.setContentText(creation.message());
            alert.showAndWait();
        });
    }
}

