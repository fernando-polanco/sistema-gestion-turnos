package mx.uady.sgt.domain;

/**
 * Estados posibles del ciclo de vida de un {@code Turno}.
 */
public enum TurnoState {
    WAITING("En espera"),
    IN_SERVICE("En atención"),
    SERVED("Atendido"),
    CANCELLED("Cancelado");

    private final String label;

    TurnoState(String label) {
        this.label = label;
    }

    /**
     * Etiqueta legible en español del estado.
     *
     * @return etiqueta del estado
     */
    public String label() {
        return label;
    }

    /**
     * Indica si el estado es terminal y no admite más transiciones.
     *
     * @return {@code true} si es {@link #SERVED} o {@link #CANCELLED}
     */
    public boolean isTerminal() {
        return switch (this) {
            case SERVED, CANCELLED -> true;
            case WAITING, IN_SERVICE -> false;
        };
    }

    /**
     * Verifica si la transición hacia el estado destino está permitida.
     *
     * @param target estado destino (puede ser {@code null})
     * @return {@code true} únicamente para {@code WAITING->IN_SERVICE},
     * {@code WAITING->CANCELLED} e {@code IN_SERVICE->SERVED}
     */
    public boolean canTransitionTo(TurnoState target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case WAITING -> target == IN_SERVICE || target == CANCELLED;
            case IN_SERVICE -> target == SERVED;
            case SERVED, CANCELLED -> false;
        };
    }
}
