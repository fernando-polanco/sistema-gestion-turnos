package mx.uady.sgt.domain;

import java.time.Instant;

/**
 * Representa un turno individual de atención dentro del sistema.
 *
 * @param id identificador único del turno
 * @param sequence número secuencial creciente (mayor o igual a 1)
 * @param registeredAt instante de registro del turno
 * @param state estado actual en el ciclo de vida
 * @param assignedVentanilla ventanilla asignada; {@code null} en {@code WAITING} y {@code CANCELLED}
 */
public record Turno(
        TurnoId id,
        long sequence,
        Instant registeredAt,
        TurnoState state,
        VentanillaId assignedVentanilla) {

    /**
     * Valida las invariantes estructurales de un {@code Turno}.
     */
    public Turno {
        if (id == null) {
            throw new IllegalArgumentException("Turno id must not be null");
        }
        if (sequence < 1) {
            throw new IllegalArgumentException("Sequence must be >= 1: " + sequence);
        }
        if (registeredAt == null) {
            throw new IllegalArgumentException("registeredAt must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (!id.equals(TurnoId.fromSequence(sequence))) {
            throw new IllegalArgumentException(
                    "Turno id " + id.value() + " does not match sequence " + sequence);
        }
        switch (state) {
            case WAITING, CANCELLED -> {
                if (assignedVentanilla != null) {
                    throw new IllegalArgumentException(
                            "assignedVentanilla must be null when state is " + state);
                }
            }
            case IN_SERVICE, SERVED -> {
                if (assignedVentanilla == null) {
                    throw new IllegalArgumentException(
                            "assignedVentanilla must not be null when state is " + state);
                }
            }
        }
    }

    /**
     * Registra un nuevo turno en estado {@link TurnoState#WAITING}.
     *
     * @param sequence número de secuencia asignado (mayor o igual a 1)
     * @param now instante de registro
     * @return nuevo turno en espera sin ventanilla asignada
     */
    public static Turno register(long sequence, Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("registeredAt must not be null");
        }
        return new Turno(TurnoId.fromSequence(sequence), sequence, now, TurnoState.WAITING, null);
    }

    /**
     * Inicia la atención del turno asignándolo a la ventanilla indicada.
     *
     * @param ventanilla ventanilla que atenderá el turno
     * @return nuevo {@code Turno} en estado {@link TurnoState#IN_SERVICE}
     * @throws InvalidTransitionException si el estado actual no permite pasar a {@code IN_SERVICE}
     */
    public Turno startService(VentanillaId ventanilla) {
        if (!state.canTransitionTo(TurnoState.IN_SERVICE)) {
            throw new InvalidTransitionException(
                    "Cannot start service for Turno " + id.value() + " from state " + state);
        }
        if (ventanilla == null) {
            throw new IllegalArgumentException("ventanilla must not be null");
        }
        return new Turno(id, sequence, registeredAt, TurnoState.IN_SERVICE, ventanilla);
    }

    /**
     * Finaliza la atención del turno y lo marca como {@link TurnoState#SERVED}.
     *
     * @return nuevo {@code Turno} en estado terminal {@code SERVED}
     * @throws InvalidTransitionException si el turno no está en estado {@code IN_SERVICE}
     */
    public Turno finish() {
        if (!state.canTransitionTo(TurnoState.SERVED)) {
            throw new InvalidTransitionException(
                    "Cannot finish Turno " + id.value() + " from state " + state);
        }
        return new Turno(id, sequence, registeredAt, TurnoState.SERVED, assignedVentanilla);
    }

    /**
     * Cancela un turno en espera y lo marca como {@link TurnoState#CANCELLED}.
     *
     * @return nuevo {@code Turno} en estado terminal {@code CANCELLED}
     * @throws InvalidTransitionException si el turno no está en estado {@code WAITING}
     */
    public Turno cancel() {
        if (!state.canTransitionTo(TurnoState.CANCELLED)) {
            throw new InvalidTransitionException(
                    "Cannot cancel Turno " + id.value() + " from state " + state);
        }
        return new Turno(id, sequence, registeredAt, TurnoState.CANCELLED, null);
    }
}
