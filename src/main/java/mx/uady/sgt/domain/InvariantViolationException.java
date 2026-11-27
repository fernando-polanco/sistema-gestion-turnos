package mx.uady.sgt.domain;

/**
 * Excepción lanzada cuando se viola una invariante estructural al restaurar o validar el dominio.
 */
public class InvariantViolationException extends RuntimeException {

    /**
     * Crea la excepción con el detalle técnico de la invariante violada.
     *
     * @param message descripción del error
     */
    public InvariantViolationException(String message) {
        super(message);
    }
}
