package mx.uady.sgt.domain;

/**
 * Resultado tipado de una operación del dominio, sin textos de interfaz.
 *
 * @param <T> tipo del valor devuelto en caso de éxito
 */
public sealed interface Result<T> permits Result.Success, Result.Rejection {

    /**
     * Resultado exitoso con el valor producido por la operación.
     *
     * @param value valor resultante (no nulo)
     * @param <T> tipo del valor exitoso
     */
    record Success<T>(T value) implements Result<T> {

        /**
         * Valida que el valor exitoso no sea nulo.
         */
        public Success {
            if (value == null) {
                throw new IllegalArgumentException("Success value must not be null");
            }
        }
    }

    /**
     * Resultado rechazado por una regla de negocio o validación de entrada.
     *
     * <p>Tanto {@code turnoId} como {@code ventanillaId} pueden ser {@code null} cuando no
     * apliquen al motivo del rechazo (por ejemplo, {@link RejectionReason#QUEUE_EMPTY} puede omitir
     * {@code turnoId}, o {@link RejectionReason#TURNO_NOT_FOUND} puede omitir {@code ventanillaId}).</p>
     *
     * @param reason motivo tipado del rechazo (no nulo)
     * @param turnoId identificador del turno implicado, o {@code null} si no aplica
     * @param ventanillaId identificador de la ventanilla implicada, o {@code null} si no aplica
     * @param <T> tipo esperado en caso de éxito
     */
    record Rejection<T>(
            RejectionReason reason,
            TurnoId turnoId,
            VentanillaId ventanillaId) implements Result<T> {

        /**
         * Valida que el motivo de rechazo no sea nulo.
         */
        public Rejection {
            if (reason == null) {
                throw new IllegalArgumentException("Rejection reason must not be null");
            }
        }
    }
}
