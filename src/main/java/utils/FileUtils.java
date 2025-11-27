package utils;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FileUtils {

    private static final String MEDIA_DIRECTORY = "media";
    private static final List<String> SUPPORTED_AUDIO_EXTENSIONS = Arrays.asList(
        "*.mp3", "*.wav", "*.m4a", "*.aac", "*.ogg", "*.flac"
    );

    static {
        // Créer le dossier media s'il n'existe pas
        createMediaDirectory();
    }

    /**
     * Ouvre un FileChooser pour sélectionner un fichier audio
     */
    public static File selectAudioFile(Window ownerWindow) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier audio");
        
        // Filtrer les fichiers audio
        FileChooser.ExtensionFilter audioFilter = new FileChooser.ExtensionFilter(
            "Fichiers Audio", SUPPORTED_AUDIO_EXTENSIONS
        );
        fileChooser.getExtensionFilters().add(audioFilter);
        
        // Définir le répertoire initial (dossier utilisateur)
        String userHome = System.getProperty("user.home");
        File initialDirectory = new File(userHome);
        if (initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }
        
        return fileChooser.showOpenDialog(ownerWindow);
    }

    /**
     * Copie un fichier audio dans le dossier media et retourne le nouveau chemin
     */
    public static String copyAudioFileToMedia(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("Fichier source invalide");
        }

        // Vérifier l'extension du fichier
        String fileName = sourceFile.getName();
        String extension = getFileExtension(fileName);
        if (!isAudioFile(fileName)) {
            throw new IllegalArgumentException("Format de fichier non supporté: " + extension);
        }

        // Créer un nom unique pour éviter les conflits
        String uniqueFileName = generateUniqueFileName(fileName);
        Path targetPath = Paths.get(MEDIA_DIRECTORY, uniqueFileName);

        // Copier le fichier
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Retourner le chemin relatif
        return targetPath.toString().replace("\\", "/"); // Normaliser les séparateurs
    }

    /**
     * Vérifie si un fichier est un fichier audio supporté
     */
    public static boolean isAudioFile(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return Arrays.asList("mp3", "wav", "m4a", "aac", "ogg", "flac").contains(extension);
    }

    /**
     * Obtient l'extension d'un fichier
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Génère un nom de fichier unique
     */
    private static String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        
        // Nettoyer le nom de base (supprimer les caractères spéciaux)
        baseName = baseName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        
        // Ajouter un UUID pour garantir l'unicité
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return baseName + "_" + uniqueId + "." + extension;
    }

    /**
     * Crée le dossier media s'il n'existe pas
     */
    private static void createMediaDirectory() {
        try {
            Path mediaPath = Paths.get(MEDIA_DIRECTORY);
            if (!Files.exists(mediaPath)) {
                Files.createDirectories(mediaPath);
                System.out.println("Dossier media créé: " + mediaPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du dossier media: " + e.getMessage());
        }
    }

    /**
     * Vérifie si un fichier existe dans le dossier media
     */
    public static boolean mediaFileExists(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        Path filePath = Paths.get(relativePath);
        return Files.exists(filePath);
    }

    /**
     * Obtient la taille d'un fichier en octets
     */
    public static long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Formate la taille d'un fichier en format lisible
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
