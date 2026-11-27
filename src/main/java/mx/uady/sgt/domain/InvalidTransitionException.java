package mx.uady.sgt.domain;

/**
 * Excepción lanzada cuando se intenta ejecutar una transición de estado ilegal sobre un {@code Turno}.
 */
public class InvalidTransitionException extends RuntimeException {

    /**
     * Crea la excepción con el detalle técnico de la transición rechazada.
     *
     * @param message descripción del error
     */
    public InvalidTransitionException(String message) {
        super(message);
    }
}
