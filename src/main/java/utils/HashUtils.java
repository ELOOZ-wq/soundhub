package utils;

import org.mindrot.jbcrypt.BCrypt;

public final class HashUtils {

    private static final int WORK_FACTOR = 10;

    private HashUtils() {
    }

    public static String hashPassword(String plainPassword) {
        ValidationUtils.require(plainPassword != null && !plainPassword.isBlank(), "Mot de passe requis.");
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean matches(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
