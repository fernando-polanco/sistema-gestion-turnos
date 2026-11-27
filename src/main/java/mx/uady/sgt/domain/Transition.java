package mx.uady.sgt.domain;

/**
 * Resultado exitoso de una transición en el motor de turnos.
 *
 * @param newState nuevo estado inmutable del motor tras aplicar la operación
 * @param turno turno creado o modificado por la operación
 */
public record Transition(TurnoEngine newState, Turno turno) {

    /**
     * Valida que el nuevo estado y el turno afectado no sean nulos.
     */
    public Transition {
        if (newState == null) {
            throw new IllegalArgumentException("newState must not be null");
        }
        if (turno == null) {
            throw new IllegalArgumentException("turno must not be null");
        }
    }
}
