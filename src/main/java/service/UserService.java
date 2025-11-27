package service;

import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Role;
import model.User;
import model.UserStatus;
import utils.HashUtils;
import utils.ValidationUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {

    private final UserDAO userDAO;
    private final ObservableList<User> users = FXCollections.observableArrayList();

    public UserService() {
        this.userDAO = new UserDAO();
        loadUsers();
        seedIfEmpty();
    }

    private void loadUsers() {
        users.clear();
        users.addAll(userDAO.findAll());
    }

    private void seedIfEmpty() {
        if (users.isEmpty()) {
            addUser("superadmin", "super@soundhub.local", "Super#2024", Role.SUPER_ADMIN, UserStatus.ACTIVE);
            addUser("clara-admin", "clara@soundhub.local", "Admin#2024", Role.ADMIN, UserStatus.ACTIVE);
            addUser("marco", "marco@soundhub.local", "User#2024", Role.USER, UserStatus.ACTIVE);
            addUser("alice", "alice@soundhub.local", "User#2024", Role.USER, UserStatus.ACTIVE);
            addUser("pendingUser", "pending@soundhub.local", "User#2024", Role.USER, UserStatus.PENDING);
        }
    }

    private User addUser(String username, String email, String rawPassword, Role role, UserStatus status) {
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Email invalide");
        }
        String passwordHash = HashUtils.hashPassword(rawPassword);
        User user = new User(0, username, email, passwordHash, role, status);
        User savedUser = userDAO.save(user);
        users.add(savedUser);
        return savedUser;
    }

    public User registerUser(String username, String email, String password) {
        ValidationUtils.require(ValidationUtils.isNotBlank(username), "Le nom d'utilisateur est requis.");
        ValidationUtils.require(ValidationUtils.isValidEmail(email), "Email invalide.");
        ValidationUtils.require(ValidationUtils.hasMinLength(password, 6), "Mot de passe trop court.");
        ensureUnique(username, email);
        return addUser(username, email, password, Role.USER, UserStatus.PENDING);
    }

    private void ensureUnique(String username, String email) {
        // Vérifier si l'email existe déjà
        if (userDAO.findByEmailOrUsername(email).isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        // Vérifier si le nom d'utilisateur existe déjà
        if (userDAO.findByEmailOrUsername(username).isPresent()) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }
    }

    public User authenticate(String login, String password) {
        ValidationUtils.require(ValidationUtils.isNotBlank(login), "Identifiant requis.");
        ValidationUtils.require(ValidationUtils.isNotBlank(password), "Mot de passe requis.");

        User user = userDAO.findByEmailOrUsername(login)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        if (!HashUtils.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe incorrect.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Le compte est " + user.getStatus().toString().toLowerCase(Locale.FRENCH) + ".");
        }
        return user;
    }

    public void updateStatus(User user, UserStatus status) {
        Objects.requireNonNull(user, "Utilisateur requis");
        user.setStatus(status);
        userDAO.save(user);
        loadUsers(); // Recharger la liste
    }

    public void updateRole(User user, Role role) {
        Objects.requireNonNull(user, "Utilisateur requis");
        user.setRole(role);
        userDAO.save(user);
        loadUsers(); // Recharger la liste
    }

    public ObservableList<User> getUsers() {
        return FXCollections.unmodifiableObservableList(users);
    }

    public List<User> getPendingUsers() {
        return users.stream()
                .filter(u -> u.getStatus() == UserStatus.PENDING)
                .collect(Collectors.toList());
    }

    public List<User> getAdmins() {
        return users.stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .collect(Collectors.toList());
    }

    public List<User> getArtists() {
        return users.stream()
                .filter(u -> u.getRole() != Role.SUPER_ADMIN)
                .collect(Collectors.toList());
    }
}
