package mx.uady.sgt.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Identificador único e inmutable de un {@code Turno}, con formato {@code ^T\d{3,}$}.
 *
 * @param value representación textual del identificador
 */
public record TurnoId(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^T\\d{3,}$");

    /**
     * Valida que el identificador cumpla con el formato {@code ^T\d{3,}$}.
     */
    public TurnoId {
        if (value == null || !VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid TurnoId format: " + value);
        }
    }

    /**
     * Genera el identificador canónico a partir de una secuencia positiva.
     *
     * @param sequence número de secuencia mayor o igual a 1
     * @return identificador con formato {@code T%03d}
     */
    public static TurnoId fromSequence(long sequence) {
        if (sequence < 1) {
            throw new IllegalArgumentException("Sequence must be >= 1: " + sequence);
        }
        return new TurnoId(String.format(Locale.ROOT, "T%03d", sequence));
    }

    /**
     * Parsea una cadena aplicando recorte de espacios y conversión a mayúsculas.
     *
     * @param raw texto de entrada (puede ser {@code null})
     * @return {@link TurnoId} válido o {@link Optional#empty()} si no cumple el formato
     */
    public static Optional<TurnoId> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!VALID_PATTERN.matcher(normalized).matches()) {
            return Optional.empty();
        }
        return Optional.of(new TurnoId(normalized));
    }
}
