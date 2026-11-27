package mx.uady.sgt.domain;

import java.util.Optional;

/**
 * Vista derivada que representa una ventanilla lógica y su turno actualmente en atención.
 * No es un dato persistido directamente; se calcula a partir del estado de los turnos.
 *
 * @param id identificador fijo de la ventanilla
 * @param currentTurno turno actualmente en atención, o {@link Optional#empty()} si está libre
 */
public record Ventanilla(VentanillaId id, Optional<Turno> currentTurno) {

    /**
     * Valida la consistencia entre la ventanilla y el turno asignado si existe.
     */
    public Ventanilla {
        if (id == null) {
            throw new IllegalArgumentException("Ventanilla id must not be null");
        }
        if (currentTurno == null) {
            throw new IllegalArgumentException("currentTurno must not be null");
        }
        currentTurno.ifPresent(turno -> {
            if (turno.state() != TurnoState.IN_SERVICE || turno.assignedVentanilla() != id) {
                throw new IllegalArgumentException(
                        "currentTurno must be IN_SERVICE and assigned to " + id);
            }
        });
    }

    /**
     * Indica si la ventanilla se encuentra disponible para recibir un nuevo turno.
     *
     * @return {@code true} si no tiene un turno en atención
     */
    public boolean isFree() {
        return currentTurno.isEmpty();
    }
}
