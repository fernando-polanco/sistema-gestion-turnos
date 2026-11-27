package mx.uady.sgt.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * Identificadores fijos de las tres ventanillas lógicas del sistema.
 */
public enum VentanillaId {
    V01,
    V02,
    V03;

    /**
     * Parsea un texto a {@link VentanillaId} aplicando recorte de espacios e ignorando mayúsculas.
     *
     * @param raw texto de entrada (puede ser {@code null})
     * @return identificador correspondiente o {@link Optional#empty()} si es nulo o inválido
     */
    public static Optional<VentanillaId> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "V01" -> Optional.of(V01);
            case "V02" -> Optional.of(V02);
            case "V03" -> Optional.of(V03);
            default -> Optional.empty();
        };
    }
}
