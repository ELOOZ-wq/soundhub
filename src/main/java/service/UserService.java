package service;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class UserService {

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    public UserService() {
        seed();
    }

    private void seed() {
        addUser("superadmin", "super@soundhub.local", "Super#2024", Role.SUPER_ADMIN, UserStatus.ACTIVE);
        addUser("clara-admin", "clara@soundhub.local", "Admin#2024", Role.ADMIN, UserStatus.ACTIVE);
        addUser("marco", "marco@soundhub.local", "User#2024", Role.USER, UserStatus.ACTIVE);
        addUser("alice", "alice@soundhub.local", "User#2024", Role.USER, UserStatus.ACTIVE);
        addUser("pendingUser", "pending@soundhub.local", "User#2024", Role.USER, UserStatus.PENDING);
    }

    private User addUser(String username, String email, String rawPassword, Role role, UserStatus status) {
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Email invalide");
        }
        String passwordHash = HashUtils.hashPassword(rawPassword);
        User user = new User(idSequence.getAndIncrement(), username, email, passwordHash, role, status);
        users.add(user);
        return user;
    }

    public User registerUser(String username, String email, String password) {
        ValidationUtils.require(ValidationUtils.isNotBlank(username), "Le nom d'utilisateur est requis.");
        ValidationUtils.require(ValidationUtils.isValidEmail(email), "Email invalide.");
        ValidationUtils.require(ValidationUtils.hasMinLength(password, 6), "Mot de passe trop court.");
        ensureUnique(username, email);
        return addUser(username, email, password, Role.USER, UserStatus.PENDING);
    }

    private void ensureUnique(String username, String email) {
        Optional<User> usernameClash = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
        Optional<User> emailClash = users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
        if (usernameClash.isPresent()) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris.");
        }
        if (emailClash.isPresent()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }
    }

    public User authenticate(String login, String password) {
        ValidationUtils.require(ValidationUtils.isNotBlank(login), "Identifiant requis.");
        ValidationUtils.require(ValidationUtils.isNotBlank(password), "Mot de passe requis.");

        User user = users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(login) || u.getEmail().equalsIgnoreCase(login))
                .findFirst()
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
    }

    public void updateRole(User user, Role role) {
        Objects.requireNonNull(user, "Utilisateur requis");
        user.setRole(role);
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
