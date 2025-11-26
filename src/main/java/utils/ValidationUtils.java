package utils;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private ValidationUtils() {
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean hasMinLength(String value, int length) {
        return isNotBlank(value) && value.length() >= length;
    }

    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    public static void require(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static Duration parseDuration(String value) {
        if (!isNotBlank(value)) {
            throw new IllegalArgumentException("La durée est requise.");
        }
        String sanitized = value.trim().toLowerCase(Locale.FRENCH);
        if (sanitized.contains(":")) {
            String[] parts = sanitized.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Format attendu mm:ss.");
            }
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return Duration.ofMinutes(minutes).plusSeconds(seconds);
        }
        double minutes = Double.parseDouble(sanitized.replace(',', '.'));
        long totalSeconds = Math.round(minutes * 60);
        return Duration.ofSeconds(totalSeconds);
    }
}
