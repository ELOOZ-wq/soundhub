package ui.views;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.*;
import service.FavoriteService;
import service.PlaylistService;
import service.TrackService;
import service.UserService;
import ui.SoundHubController;
import ui.components.MiniPlayer;
import utils.ValidationUtils;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class DashboardView extends BorderPane {

    private final SoundHubController controller;
    private final User currentUser;
    private final Runnable onLogout;

    private final MiniPlayer miniPlayer = new MiniPlayer();
    private final FilteredList<Track> filteredTracks;
    private final FilteredList<User> pendingUsers;
    private final FilteredList<User> adminUsers;
    private final FilteredList<Track> moderationTracks;

    private final FlowPane radioCarousel = createCarousel();
    private final FlowPane albumCarousel = createCarousel();
    private final ToggleGroup tagGroup = new ToggleGroup();

    private final ListView<Playlist> playlistList = new ListView<>();
    private final ListView<Track> playlistTracks = new ListView<>();
    private final ListView<Favorite> favoritesList = new ListView<>();
    // Dans ui.views.DashboardView.java (Champs de la classe)
// ...
    private final FlowPane trendingAlbumsPane = new FlowPane(16, 16); // Existant
    // Nouvelle zone dédiée aux morceaux d'album
    private final ListView<Track> selectedAlbumTracksList = new ListView<>();
    private final VBox trackDisplayContainer = new VBox(10);
    private final Label albumTracksTitle = new Label("Morceaux de l'album...");
// ...

    private String currentQuery = "";
    private TrackStatus selectedStatus = null;

    public DashboardView(SoundHubController controller, User user, Runnable onLogout) {
        this.controller = controller;
        this.currentUser = user;
        this.onLogout = onLogout;
        TrackService trackService = controller.getTrackService();
        UserService userService = controller.getUserService();
        this.filteredTracks = new FilteredList<>(trackService.getTracks());
        this.pendingUsers = new FilteredList<>(userService.getUsers(), this::isPending);
        this.adminUsers = new FilteredList<>(userService.getUsers(), this::isAdmin);
        this.moderationTracks = new FilteredList<>(trackService.getTracks(), this::isPendingTrack);

        getStyleClass().add("spotify-root");
        initLists();
        setTop(buildTopBar());
        setLeft(buildLibraryPanel());
        setCenter(buildMainScroll());
        setBottom(miniPlayer);
        applyFilters();
    }

    private void initLists() {
        PlaylistService playlistService = controller.getPlaylistService();
        playlistList.setItems(playlistService.getPlaylists(currentUser));
        playlistList.setPlaceholder(new Label("Aucune playlist"));
        playlistList.getSelectionModel().selectedItemProperty().addListener((obs, old, playlist) -> {
            if (playlist == null) {
                playlistTracks.setItems(FXCollections.observableArrayList());
            } else {
                playlistTracks.setItems(playlist.getTracks());
            }
        });
        playlistTracks.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && playlistTracks.getSelectionModel().getSelectedItem() != null) {
                Track track = playlistTracks.getSelectionModel().getSelectedItem();
                miniPlayer.loadTrack(track);
                miniPlayer.play();
            }
        });

        FavoriteService favoriteService = controller.getFavoriteService();
// Charge initialement les favoris depuis la base
        favoriteService.loadFavorites(currentUser);

// Lie la ListView au cache pour mise à jour immédiate
        favoritesList.setItems(favoriteService.getFavoritesCache());
        favoritesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Favorite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTrack().getTitle() + " • " + item.getTrack().getArtistName());
                }
            }
        });
        favoritesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && favoritesList.getSelectionModel().getSelectedItem() != null) {
                Track track = favoritesList.getSelectionModel().getSelectedItem().getTrack();
                miniPlayer.loadTrack(track);
                miniPlayer.play();
            }
        });
    }

    private Node buildTopBar() {
        HBox bar = new HBox(15);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Button homeButton = pillButton("⌂");
        Button browseButton = pillButton("⌕");

        TextField searchField = new TextField();
        searchField.setPromptText("Que souhaitez-vous écouter ou regarder ?");
        searchField.getStyleClass().add("search-pill");
        searchField.textProperty().addListener((obs, old, value) -> {
            currentQuery = value == null ? "" : value.trim().toLowerCase();
            applyFilters();
        });

        Button premiumButton = outlineButton("Découvrir Premium");
        Button appButton = outlineButton("Installer l'appli");
        Button bellButton = pillButton("\uD83D\uDD14");
        Button peopleButton = pillButton("\uD83D\uDC65");
        Button avatarButton = pillButton(currentUser.getUsername().substring(0, 1).toUpperCase());

        Button uploadButton = outlineButton("Uploader");
        uploadButton.setOnAction(event -> openUploadDialog());
        uploadButton.setDisable(!currentUser.getRole().canUploadTracks());

        Button logoutButton = outlineButton("Quitter");
        logoutButton.setOnAction(event -> {
            controller.logout();
            onLogout.run();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(homeButton, browseButton, searchField, spacer,
                premiumButton, appButton, uploadButton, bellButton, peopleButton, avatarButton, logoutButton);
        return bar;
    }

    private Node buildLibraryPanel() {
        VBox panel = new VBox(20);
        panel.getStyleClass().add("library-panel");
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(260);

        Label title = new Label("Bibliothèque");
        title.getStyleClass().add("library-title");

        VBox navButtons = new VBox(10,
                navEntry("Accueil"),
                navEntry("Explorer"),
                navEntry("Radio")
        );

        VBox playlistCard = libraryCard(
                "Créez votre première playlist",
                "C'est simple, nous allons vous aider",
                "Créer une playlist",
                () -> controller.getPlaylistService().createPlaylist(currentUser, "Nouvelle playlist"));

        VBox podcastCard = libraryCard(
                "Cherchons des podcasts",
                "Nous vous préviendrons des nouveautés",
                "Parcourir les podcasts",
                () -> {
                });

        panel.getChildren().addAll(title, navButtons, playlistCard, podcastCard);
        VBox.setVgrow(podcastCard, Priority.ALWAYS);
        return panel;
    }

    private Node buildMainScroll() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");

        VBox content = new VBox(30);
        content.setPadding(new Insets(30));
        content.getChildren().add(buildTagRow());
        content.getChildren().add(buildSection("Radio populaire", "Tout afficher", radioCarousel));
        content.getChildren().add(buildSection("Albums & singles populaires", null, buildAlbumsSectionContent()));        content.getChildren().add(buildPlaylistManagerCard());
        content.getChildren().add(buildFavoritesCard());
        Node moderation = buildModerationCard();
        if (moderation != null) {
            content.getChildren().add(moderation);
        }

        scroll.setContent(content);
        return scroll;
    }

    private Node buildTagRow() {
        HBox tags = new HBox(12);
        tags.getStyleClass().add("tag-row");

        tags.getChildren().addAll(
                tagButton("Tout", null, true),
                tagButton("Musique", TrackStatus.APPROVED, false),
                tagButton("Podcasts", TrackStatus.PENDING, false)
        );
        return tags;
    }

    private ToggleButton tagButton(String label, TrackStatus status, boolean selected) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(tagGroup);
        button.getStyleClass().add("tag-chip");
        button.setSelected(selected);
        button.setUserData(status);
        button.selectedProperty().addListener((obs, old, value) -> {
            if (value) {
                selectedStatus = status;
                applyFilters();
            }
        });
        if (selected) {
            tagGroup.selectToggle(button);
        }
        return button;
    }

    private Node buildSection(String title, String actionLabel, Node content) {
        VBox card = new VBox(18);
        card.getStyleClass().add("content-card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(label, spacer);
        if (actionLabel != null) {
            Button action = new Button(actionLabel);
            action.getStyleClass().add("ghost-button");
            header.getChildren().add(action);
        }
        card.getChildren().addAll(header, content);
        return card;
    }

    private Node buildPlaylistManagerCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("list-card");

        Label title = new Label("Votre univers SoundHub");
        title.getStyleClass().add("section-title");

        HBox lists = new HBox(20);
        lists.setPrefHeight(220);
        playlistList.setPrefWidth(220);
        playlistTracks.setPrefWidth(320);
        playlistTracks.setPlaceholder(new Label("Sélectionne une playlist"));
        lists.getChildren().addAll(playlistList, playlistTracks);
        HBox.setHgrow(playlistTracks, Priority.ALWAYS);

        Button addPlaylist = new Button("Nouvelle playlist");
        addPlaylist.setOnAction(event -> {
            Playlist playlist = controller.getPlaylistService().createPlaylist(currentUser, "Playlist " + (playlistList.getItems().size() + 1));
            playlistList.getSelectionModel().select(playlist);
        });

        Button rename = new Button("Renommer");
        rename.disableProperty().bind(playlistList.getSelectionModel().selectedItemProperty().isNull());
        rename.setOnAction(event -> {
            Playlist playlist = playlistList.getSelectionModel().getSelectedItem();
            if (playlist != null) {
                TextInputDialog dialog = new TextInputDialog(playlist.getName());
                dialog.setHeaderText("Renommer la playlist");
                dialog.showAndWait().ifPresent(name -> {
                    controller.getPlaylistService().renamePlaylist(playlist, name);
                    playlistList.refresh();
                });
            }
        });

        Button delete = new Button("Supprimer");
        delete.disableProperty().bind(playlistList.getSelectionModel().selectedItemProperty().isNull());
        delete.setOnAction(event -> {
            Playlist playlist = playlistList.getSelectionModel().getSelectedItem();
            if (playlist != null) {
                controller.getPlaylistService().deletePlaylist(currentUser, playlist);
            }
        });

        Button addTrack = new Button("Ajouter un track");
        addTrack.disableProperty().bind(playlistList.getSelectionModel().selectedItemProperty().isNull());
        addTrack.setOnAction(event -> {
            Playlist playlist = playlistList.getSelectionModel().getSelectedItem();
            if (playlist != null) {
                selectTrackForPlaylist().ifPresent(track -> controller.getPlaylistService().addTrack(playlist, track));
            }
        });

        Button removeTrack = new Button("Retirer le track");
        removeTrack.disableProperty().bind(playlistTracks.getSelectionModel().selectedItemProperty().isNull());
        removeTrack.setOnAction(event -> {
            Playlist playlist = playlistList.getSelectionModel().getSelectedItem();
            Track track = playlistTracks.getSelectionModel().getSelectedItem();
            if (playlist != null && track != null) {
                controller.getPlaylistService().removeTrack(playlist, track);
            }
        });

        HBox actions = new HBox(10, addPlaylist, rename, delete, addTrack, removeTrack);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, lists, actions);
        return card;
    }

    private ListCell<Track> createAlbumTrackCell() {
        return new ListCell<Track>() {
            @Override
            protected void updateItem(Track track, boolean empty) {
                super.updateItem(track, empty);
                if (empty || track == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label titleLabel = new Label(track.getTitle());
                    Label durationLabel = new Label(track.formattedDuration());

                    // HBox avec un espaceur pour aligner la durée à droite
                    HBox hbox = new HBox(10, titleLabel, new Region(), durationLabel);
                    HBox.setHgrow(hbox.getChildren().get(1), Priority.ALWAYS);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(hbox);
                    setText(null);
                }
            }
        };
    }
    // Dans ui.views.DashboardView.java (Nouvelle méthode)

    private Node buildAlbumsSectionContent() {
        // 1. Configuration de la liste des morceaux (à droite)
        albumTracksTitle.getStyleClass().add("section-subtitle");

        // Configuration de la ListView
        selectedAlbumTracksList.setPlaceholder(new Label("Sélectionne un album pour voir ses morceaux"));
        selectedAlbumTracksList.setCellFactory(list -> createAlbumTrackCell());
        selectedAlbumTracksList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && selectedAlbumTracksList.getSelectionModel().getSelectedItem() != null) {
                Track track = selectedAlbumTracksList.getSelectionModel().getSelectedItem();
                miniPlayer.loadTrack(track);
                miniPlayer.play();
            }
        });

        // Conteneur de la zone des morceaux (Titre + Liste)
        trackDisplayContainer.getChildren().addAll(albumTracksTitle, selectedAlbumTracksList);
        trackDisplayContainer.setMinWidth(300);
        trackDisplayContainer.setPrefWidth(300);
        trackDisplayContainer.setVisible(false); // Cacher par défaut

        // 2. Encapsuler l'albumCarousel dans un ScrollPane pour le fixer
        ScrollPane albumCardsScrollPane = new ScrollPane(albumCarousel);
        albumCardsScrollPane.setFitToWidth(true);
        albumCardsScrollPane.setPrefHeight(250);
        albumCardsScrollPane.getStyleClass().add("transparent-scrollpane");

        // 3. Conteneur horizontal (Albums à gauche, Liste des morceaux à droite)
        HBox contentRow = new HBox(20);
        HBox.setHgrow(albumCardsScrollPane, Priority.ALWAYS);
        contentRow.getChildren().addAll(albumCardsScrollPane, trackDisplayContainer);

        return contentRow;
    }

    private Node buildFavoritesCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("list-card");
        Label title = new Label("Favoris");
        title.getStyleClass().add("section-title");

        favoritesList.setPrefHeight(160);

        // Lie la ListView au cache pour mise à jour immédiate
        favoritesList.setItems(controller.getFavoriteService().getFavoritesCache());
        favoritesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Favorite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTrack().getTitle() + " • " + item.getTrack().getArtistName());
                }
            }
        });

        Button remove = new Button("Retirer des favoris");
        remove.disableProperty().bind(favoritesList.getSelectionModel().selectedItemProperty().isNull());
        remove.setOnAction(event -> {
            Favorite favorite = favoritesList.getSelectionModel().getSelectedItem();
            if (favorite != null) {
                controller.getFavoriteService().toggleFavorite(currentUser, favorite.getTrack());
            }
        });

        card.getChildren().addAll(title, favoritesList, remove);
        return card;
    }



    private Node buildModerationCard() {
        boolean canModerate = currentUser.getRole().canModerateTracks() || currentUser.getRole().canModerateUsers();
        if (!canModerate) {
            return null;
        }
        VBox card = new VBox(18);
        card.getStyleClass().add("list-card");
        Label title = new Label("Validation & rôles");
        title.getStyleClass().add("section-title");
        card.getChildren().add(title);

        if (currentUser.getRole().canModerateUsers()) {
            card.getChildren().add(buildUserModeration());
        }
        if (currentUser.getRole().canModerateTracks()) {
            card.getChildren().add(buildTrackModeration());
        }
        if (currentUser.getRole().canManageAdmins()) {
            card.getChildren().add(buildAdminManagement());
        }
        return card;
    }

    private Node buildUserModeration() {
        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<User, String> userCol = column("Utilisateur", "username");
        TableColumn<User, String> emailCol = column("Email", "email");
        TableColumn<User, String> roleCol = new TableColumn<>("Rôle");
        roleCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().getRole().toString()));
        table.getColumns().add(userCol);
        table.getColumns().add(emailCol);
        table.getColumns().add(roleCol);
        SortedList<User> sortedList = new SortedList<>(pendingUsers);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);
        table.setPlaceholder(new Label("Aucun utilisateur en attente"));

        Button approve = new Button("Activer");
        approve.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        approve.setOnAction(event -> {
            User selected = table.getSelectionModel().getSelectedItem();
            controller.getUserService().updateStatus(selected, UserStatus.ACTIVE);
            refreshUserFilters();
        });

        Button reject = new Button("Refuser");
        reject.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        reject.setOnAction(event -> {
            User selected = table.getSelectionModel().getSelectedItem();
            controller.getUserService().updateStatus(selected, UserStatus.BANNED);
            refreshUserFilters();
        });

        Label header = new Label("Utilisateurs en attente");
        header.getStyleClass().add("subsection-title");
        VBox box = new VBox(8, header, table, new HBox(10, approve, reject));
        return box;
    }

    private Node buildTrackModeration() {
        TableView<Track> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Track, String> titleCol = column("Titre", "title");
        TableColumn<Track, String> artistCol = column("Artiste", "artistName");
        TableColumn<Track, String> albumCol = column("Album", "album");
        table.getColumns().add(titleCol);
        table.getColumns().add(artistCol);
        table.getColumns().add(albumCol);
        SortedList<Track> sortedList = new SortedList<>(moderationTracks);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);
        table.setPlaceholder(new Label("Aucun track à valider"));

        Button approve = new Button("Approuver");
        approve.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        approve.setOnAction(event -> changeTrackStatus(table.getSelectionModel().getSelectedItem(), TrackStatus.APPROVED));

        Button reject = new Button("Rejeter");
        reject.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        reject.setOnAction(event -> changeTrackStatus(table.getSelectionModel().getSelectedItem(), TrackStatus.REJECTED));

        Label header = new Label("Validation des tracks");
        header.getStyleClass().add("subsection-title");
        VBox box = new VBox(8, header, table, new HBox(10, approve, reject));
        return box;
    }

    private Node buildAdminManagement() {
        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<User, String> userCol = column("Utilisateur", "username");
        TableColumn<User, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().getStatus().toString()));
        table.getColumns().add(userCol);
        table.getColumns().add(statusCol);
        SortedList<User> sortedList = new SortedList<>(adminUsers);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);
        table.setPlaceholder(new Label("Aucun admin déclaré"));

        Button downgrade = new Button("Repasser USER");
        downgrade.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        downgrade.setOnAction(event -> {
            User selected = table.getSelectionModel().getSelectedItem();
            controller.getUserService().updateRole(selected, Role.USER);
            refreshUserFilters();
        });

        Label header = new Label("Gestion des ADMIN");
        header.getStyleClass().add("subsection-title");
        VBox box = new VBox(8, header, table, downgrade);
        return box;
    }

    private void applyFilters() {
        filteredTracks.setPredicate(track -> {
            if (track == null) {
                return false;
            }
            if (!currentUser.getRole().canModerateTracks() && track.getStatus() != TrackStatus.APPROVED) {
                return false;
            }
            boolean matchesStatus = selectedStatus == null || track.getStatus() == selectedStatus;
            if (!matchesStatus) {
                return false;
            }
            if (currentQuery == null || currentQuery.isBlank()) {
                return true;
            }
            String normalized = currentQuery.toLowerCase();
            return track.getTitle().toLowerCase().contains(normalized)
                    || track.getArtistName().toLowerCase().contains(normalized);
        });
        refreshCarousels();
    }

    private void refreshUserFilters() {
        pendingUsers.setPredicate(this::isPending);
        adminUsers.setPredicate(this::isAdmin);
    }

    private void refreshTrackFilters() {
        moderationTracks.setPredicate(this::isPendingTrack);
        applyFilters();
    }

    private boolean isPending(User user) {
        return user.getStatus() == UserStatus.PENDING;
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private boolean isPendingTrack(Track track) {
        return track.getStatus() == TrackStatus.PENDING;
    }

    private void openUploadDialog() {
        Dialog<TrackFormData> dialog = new Dialog<>();
        dialog.setTitle("Uploader un track");
        dialog.setHeaderText("Partage un nouveau morceau");
        ButtonType saveButton = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Titre");
        TextField albumField = new TextField();
        albumField.setPromptText("Album (optionnel)");
        TextField durationField = new TextField();
        durationField.setPromptText("Durée (mm:ss ou minutes)");
        TextField fileField = new TextField();
        fileField.setPromptText("Aucun fichier sélectionné");
        fileField.setEditable(false);
        
        Button browseButton = new Button("Parcourir...");
        browseButton.setOnAction(e -> {
            File selectedFile = utils.FileUtils.selectAudioFile(dialog.getDialogPane().getScene().getWindow());
            if (selectedFile != null) {
                fileField.setText(selectedFile.getAbsolutePath());
                fileField.setUserData(selectedFile); // Stocker le fichier pour plus tard
                
                // Auto-remplir le titre si vide
                if (titleField.getText().isEmpty()) {
                    String fileName = selectedFile.getName();
                    String titleFromFile = fileName.substring(0, fileName.lastIndexOf('.'));
                    titleField.setText(titleFromFile);
                }
            }
        });
        
        HBox fileRow = new HBox(5, fileField, browseButton);
        HBox.setHgrow(fileField, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Titre"), titleField);
        grid.addRow(1, new Label("Album"), albumField);
        grid.addRow(2, new Label("Durée"), durationField);
        grid.addRow(3, new Label("Fichier"), fileRow);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                File selectedFile = (File) fileField.getUserData();
                String filePath = selectedFile != null ? selectedFile.getAbsolutePath() : "";
                return new TrackFormData(titleField.getText(), albumField.getText(), durationField.getText(), filePath);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            try {
                // Valider les données
                if (data.title().trim().isEmpty()) {
                    throw new IllegalArgumentException("Le titre est requis.");
                }
                if (data.filePath().trim().isEmpty()) {
                    throw new IllegalArgumentException("Veuillez sélectionner un fichier audio.");
                }
                
                Duration duration = ValidationUtils.parseDuration(data.duration());
                
                // Copier le fichier dans le dossier media
                File sourceFile = new File(data.filePath());
                String mediaPath = utils.FileUtils.copyAudioFileToMedia(sourceFile);
                
                // Soumettre le track avec le nouveau chemin
                controller.getTrackService().submitTrack(currentUser, data.title(), data.album(), duration, mediaPath);
                refreshTrackFilters();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "Track '" + data.title() + "' soumis pour validation.\nFichier copié vers: " + mediaPath);
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    private void addTrackToPlaylist(Track track) {
        if (playlistList.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Crée d'abord une playlist.");
            alert.showAndWait();
            return;
        }
        Playlist playlist = playlistList.getSelectionModel().getSelectedItem();
        if (playlist == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Sélectionne une playlist pour y ajouter le morceau.");
            alert.showAndWait();
            return;
        }
        controller.getPlaylistService().addTrack(playlist, track);
    }

    private void changeTrackStatus(Track track, TrackStatus status) {
        if (track == null) {
            return;
        }
        controller.getTrackService().changeStatus(track, status);
        refreshTrackFilters();
    }

    private Optional<Track> selectTrackForPlaylist() {
        ObservableList<Track> approvedTracks = controller.getTrackService().getTracks()
                .filtered(track -> track.getStatus() == TrackStatus.APPROVED);
        if (approvedTracks.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucun track approuvé disponible.");
            alert.showAndWait();
            return Optional.empty();
        }
        ChoiceDialog<Track> dialog = new ChoiceDialog<>(approvedTracks.get(0), approvedTracks);
        dialog.setHeaderText("Choisis un track à ajouter");
        return dialog.showAndWait();
    }

    private FlowPane createCarousel() {
        FlowPane pane = new FlowPane();
        pane.setHgap(20);
        pane.setVgap(20);
        return pane;
    }

    private Node createTrackCard(Track track) {
        VBox card = new VBox(10);
        card.getStyleClass().add("music-card");
        card.setPrefWidth(180);

        Rectangle cover = new Rectangle(180, 180);
        cover.setArcWidth(30);
        cover.setArcHeight(30);
        cover.setFill(colorForTrack(track));

        StackPane artwork = new StackPane(cover);
        artwork.getStyleClass().add("music-cover");

        Label title = new Label(track.getTitle());
        title.getStyleClass().add("music-card-title");
        title.setWrapText(true);

        Label artist = new Label(track.getArtistName());
        artist.getStyleClass().add("music-card-subtitle");
        artist.setWrapText(true);

        card.getChildren().addAll(artwork, title, artist);
        card.setOnMouseClicked(event -> handleCardClick(event, track));
        card.setOnContextMenuRequested(event -> {
            ContextMenu menu = new ContextMenu();
            MenuItem play = new MenuItem("Lire");
            play.setOnAction(e -> {
                miniPlayer.loadTrack(track);
                miniPlayer.play();
            });
            MenuItem favorite = new MenuItem(controller.getFavoriteService().isFavorite(currentUser, track)
                    ? "Retirer des favoris" : "Ajouter aux favoris");
            favorite.setOnAction(e -> controller.getFavoriteService().toggleFavorite(currentUser, track));
            // Sous-menu "Ajouter à une playlist"
            Menu addToPlaylist = new Menu("Ajouter à une playlist");

            ObservableList<Playlist> playlists =         controller.getPlaylistService().getPlaylists(currentUser);


// Crée un MenuItem pour chaque playlist
            for (Playlist p : playlists) {
                MenuItem playlistItem = new MenuItem(p.getName());
                playlistItem.setOnAction(e -> {
                    controller.getPlaylistService().addTrack(p, track);
                    System.out.println("Ajouté à la playlist : " + p.getName());
                });
                addToPlaylist.getItems().add(playlistItem);
            }

            menu.getItems().addAll(play, favorite, addToPlaylist);

            menu.show(card, event.getScreenX(), event.getScreenY());
        });
        return card;
    }

    private void handleCardClick(MouseEvent event, Track track) {
        miniPlayer.loadTrack(track);
        if (event.getClickCount() == 2) {
            miniPlayer.play();
        }
    }

    private Color colorForTrack(Track track) {
        Random random = new Random(track.getId() * 31L + track.getTitle().hashCode());
        double hue = random.nextDouble() * 360;
        return Color.hsb(hue, 0.65, 0.9);
    }

    private <S, T> TableColumn<S, T> column(String title, String property) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    // Dans DashboardView.java (Modification de la méthode refreshCarousels)

    private void refreshCarousels() {
        // 1. Tracks filtrés pour le carrousel "Radio populaire" (par titres)
        List<Track> tracks = filteredTracks.stream().toList();
        radioCarousel.getChildren().setAll(tracks.stream()
                .limit(5)
                .map(this::createTrackCard)
                .toList());

        // 2. Albums populaires pour le carrousel "Albums & singles populaires"
        List<Album> popularAlbums = controller.getTrackService().getPopularAlbums(); // NOUVEAU

        albumCarousel.getChildren().setAll(popularAlbums.stream()
                .map(this::createAlbumCard) // NOUVEAU: Utilise une méthode spécifique pour les albums
                .toList());
    }


    // Dans ui.views.DashboardView.java

    private Node createAlbumCard(Album album) {
        VBox card = new VBox(10);
        card.getStyleClass().add("music-card");
        card.setPrefWidth(180);

        // --- 1. Création de la couverture ---
        Rectangle cover = new Rectangle(180, 180);
        cover.setArcWidth(30);
        cover.setArcHeight(30);

        if (!album.getTracks().isEmpty()) {
            cover.setFill(colorForTrack(album.getTracks().getFirst()));
        } else {
            cover.setFill(Color.DARKGRAY);
        }

        // --- 2. Déclaration et configuration du bouton playOverlay ---
        Button playOverlay = new Button("\u25B6");
        playOverlay.getStyleClass().add("play-overlay-button");

        // --- 3. Action du bouton Play (Logique de Démarrage Album) ---
        playOverlay.setOnAction(e -> {
            if (!album.getTracks().isEmpty()) {
                // Utilise loadQueue pour démarrer la lecture de l'album entier
                miniPlayer.loadQueue(album.getTracks(), 0);
                miniPlayer.play();
            }
        });

        // --- 4. Empilement de la couverture et du bouton (StackPane) ---
        StackPane artwork = new StackPane(cover, playOverlay);
        artwork.getStyleClass().add("music-cover");

        // --- 5. Labels d'informations ---
        Label title = new Label(album.getTitle()); // Utilisation de getTitle() corrigée
        title.getStyleClass().add("music-card-title");
        title.setWrapText(true);

        String artistName = album.getTracks().isEmpty() ? "Artiste inconnu" : album.getTracks().getFirst().getArtistName();
        Label artist = new Label(artistName + " (" + album.getTracks().size() + " titres)");
        artist.getStyleClass().add("music-card-subtitle");
        artist.setWrapText(true);

        // --- 6. Action du double-clic sur la carte (Logique de Démarrage Album) ---
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                // Simple clic : Afficher les morceaux de l'album dans la zone dédiée

                // 1. Nettoyage de l'interface et mise à jour du contenu
                playlistList.getSelectionModel().select(null); // Nettoyer la sélection de Playlist
                albumTracksTitle.setText("Morceaux de l'album : " + album.getTitle());
                selectedAlbumTracksList.setItems(album.getTracks());

                // 2. Rendre la zone dédiée visible
                trackDisplayContainer.setVisible(true);

            } else if (event.getClickCount() == 2) {
                // Double clic : Lancer la lecture de l'album
                if (!album.getTracks().isEmpty()) {
                    miniPlayer.loadQueue(album.getTracks(), 0);
                    miniPlayer.play();
                }
            }
        });

        // --- 7. Assemblage de la carte ---
        card.getChildren().addAll(artwork, title, artist);

        return card;
    }


    private Button pillButton(String label) {
        Button button = new Button(label);
        button.getStyleClass().add("pill-button");
        return button;
    }

    private Button outlineButton(String label) {
        Button button = new Button(label);
        button.getStyleClass().add("outline-button");
        return button;
    }

    private Button navEntry(String label) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-entry");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private VBox libraryCard(String title, String description, String actionLabel, Runnable action) {
        VBox card = new VBox(10);
        card.getStyleClass().add("library-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("library-card-title");
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("library-card-desc");
        Button actionButton = new Button(actionLabel);
        actionButton.getStyleClass().add("outline-button");
        actionButton.setOnAction(e -> action.run());
        card.getChildren().addAll(titleLabel, descLabel, actionButton);
        return card;
    }

    private record TrackFormData(String title, String album, String duration, String filePath) {
    }
}

