package mx.uady.sgt.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Motor inmutable del Sistema de Gestión de Turnos (SGT).
 * Administra la cola única FIFO, el ciclo de vida de los turnos y las tres ventanillas lógicas
 * {@code V01}, {@code V02} y {@code V03}.
 */
public final class TurnoEngine {

    private final List<Turno> turnos;
    private final long lastSequence;

    private TurnoEngine(List<Turno> turnos, long lastSequence) {
        this.turnos = List.copyOf(turnos);
        this.lastSequence = lastSequence;
    }

    /**
     * Crea un motor vacío sin turnos registrados y con secuencia inicial en 0.
     *
     * @return instancia vacía de {@link TurnoEngine}
     */
    public static TurnoEngine empty() {
        return new TurnoEngine(List.of(), 0L);
    }

    /**
     * Restaura un motor validando todas las invariantes estructurales sobre los turnos y la
     * última secuencia registrada.
     *
     * @param turnos colección de turnos persistidos
     * @param lastSequence última secuencia emitida
     * @return instancia restaurada e inmutable de {@link TurnoEngine}
     * @throws InvariantViolationException si hay nulos, duplicados, más de un turno
     * {@code IN_SERVICE} por ventanilla o {@code lastSequence} menor que la secuencia máxima
     */
    public static TurnoEngine restore(Collection<Turno> turnos, long lastSequence) {
        if (turnos == null) {
            throw new InvariantViolationException("turnos collection must not be null");
        }
        if (lastSequence < 0) {
            throw new InvariantViolationException("lastSequence must not be negative: " + lastSequence);
        }

        Set<TurnoId> seenIds = new HashSet<>();
        Set<Long> seenSequences = new HashSet<>();
        Set<VentanillaId> occupiedVentanillas = EnumSet.noneOf(VentanillaId.class);
        long maxSequence = 0L;

        for (Turno turno : turnos) {
            if (turno == null) {
                throw new InvariantViolationException("turnos collection must not contain null elements");
            }
            if (!seenIds.add(turno.id())) {
                throw new InvariantViolationException("Duplicate TurnoId found: " + turno.id().value());
            }
            if (!seenSequences.add(turno.sequence())) {
                throw new InvariantViolationException("Duplicate sequence found: " + turno.sequence());
            }
            if (turno.sequence() > maxSequence) {
                maxSequence = turno.sequence();
            }
            if (turno.state() == TurnoState.IN_SERVICE) {
                VentanillaId ventanilla = turno.assignedVentanilla();
                if (!occupiedVentanillas.add(ventanilla)) {
                    throw new InvariantViolationException(
                            "Multiple IN_SERVICE turnos assigned to ventanilla " + ventanilla);
                }
            }
        }

        if (lastSequence < maxSequence) {
            throw new InvariantViolationException(
                    "lastSequence (" + lastSequence + ") cannot be less than max sequence (" + maxSequence + ")");
        }

        List<Turno> sorted = new ArrayList<>(turnos);
        sorted.sort(Comparator.comparingLong(Turno::sequence));
        return new TurnoEngine(sorted, lastSequence);
    }

    /**
     * Registra un nuevo turno en espera al final de la cola incrementando {@code lastSequence}.
     *
     * @param now instante de registro proporcionado por el invocador
     * @return resultado exitoso con el nuevo estado del motor y el turno creado
     */
    public Result<Transition> register(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("registeredAt must not be null");
        }
        long nextSequence = Math.incrementExact(lastSequence);
        Turno created = Turno.register(nextSequence, now);

        List<Turno> updated = new ArrayList<>(turnos.size() + 1);
        updated.addAll(turnos);
        updated.add(created);

        TurnoEngine nextEngine = new TurnoEngine(updated, nextSequence);
        return new Result.Success<>(new Transition(nextEngine, created));
    }

    /**
     * Asigna el turno en espera de menor secuencia a la ventanilla indicada e inicia su atención.
     * Evalúa primero si la ventanilla está ocupada y después si la fila está vacía (D8).
     *
     * @param ventanilla ventanilla solicitante
     * @return éxito con el turno asignado o rechazo tipado sin alterar el estado
     */
    public Result<Transition> assignNext(VentanillaId ventanilla) {
        if (ventanilla == null) {
            throw new IllegalArgumentException("ventanilla must not be null");
        }

        Optional<Turno> currentInVentanilla = findInServiceByVentanilla(ventanilla);
        if (currentInVentanilla.isPresent()) {
            return new Result.Rejection<>(
                    RejectionReason.VENTANILLA_OCCUPIED,
                    currentInVentanilla.get().id(),
                    ventanilla);
        }

        Optional<Turno> nextWaiting = turnos.stream()
                .filter(turno -> turno.state() == TurnoState.WAITING)
                .findFirst();
        if (nextWaiting.isEmpty()) {
            return new Result.Rejection<>(RejectionReason.QUEUE_EMPTY, null, ventanilla);
        }

        Turno assigned = nextWaiting.get().startService(ventanilla);
        TurnoEngine nextEngine = replacing(nextWaiting.get(), assigned);
        return new Result.Success<>(new Transition(nextEngine, assigned));
    }

    /**
     * Finaliza el turno actualmente en atención en la ventanilla indicada.
     *
     * @param ventanilla ventanilla donde concluye la atención
     * @return éxito con el turno atendido o rechazo si la ventanilla está libre
     */
    public Result<Transition> finishService(VentanillaId ventanilla) {
        if (ventanilla == null) {
            throw new IllegalArgumentException("ventanilla must not be null");
        }

        Optional<Turno> currentInVentanilla = findInServiceByVentanilla(ventanilla);
        if (currentInVentanilla.isEmpty()) {
            return new Result.Rejection<>(RejectionReason.VENTANILLA_FREE, null, ventanilla);
        }

        Turno current = currentInVentanilla.get();
        Turno finished = current.finish();
        TurnoEngine nextEngine = replacing(current, finished);
        return new Result.Success<>(new Transition(nextEngine, finished));
    }

    /**
     * Cancela un turno únicamente si existe y su estado actual es {@link TurnoState#WAITING}.
     *
     * @param turnoId identificador del turno a cancelar
     * @return éxito con el turno cancelado o rechazo si no existe o no es cancelable
     */
    public Result<Transition> cancel(TurnoId turnoId) {
        if (turnoId == null) {
            throw new IllegalArgumentException("turnoId must not be null");
        }

        Optional<Turno> found = find(turnoId);
        if (found.isEmpty()) {
            return new Result.Rejection<>(RejectionReason.TURNO_NOT_FOUND, turnoId, null);
        }

        Turno target = found.get();
        if (target.state() != TurnoState.WAITING) {
            return new Result.Rejection<>(
                    RejectionReason.TURNO_NOT_CANCELLABLE,
                    turnoId,
                    target.assignedVentanilla());
        }

        Turno cancelled = target.cancel();
        TurnoEngine nextEngine = replacing(target, cancelled);
        return new Result.Success<>(new Transition(nextEngine, cancelled));
    }

    /**
     * Devuelve la fila de espera actual con los turnos {@link TurnoState#WAITING} en orden
     * ascendente de secuencia.
     *
     * @return lista inmodificable de turnos en espera
     */
    public List<Turno> queue() {
        return turnos.stream()
                .filter(turno -> turno.state() == TurnoState.WAITING)
                .toList();
    }

    /**
     * Devuelve el estado actual de las tres ventanillas en orden fijo: {@code V01}, {@code V02},
     * {@code V03}.
     *
     * @return lista inmodificable con las tres ventanillas
     */
    public List<Ventanilla> ventanillas() {
        List<Ventanilla> result = new ArrayList<>(VentanillaId.values().length);
        for (VentanillaId id : VentanillaId.values()) {
            result.add(new Ventanilla(id, findInServiceByVentanilla(id)));
        }
        return List.copyOf(result);
    }

    /**
     * Devuelve todos los turnos registrados en orden ascendente de secuencia.
     *
     * @return lista inmodificable con el historial completo
     */
    public List<Turno> history() {
        return turnos;
    }

    /**
     * Busca un turno por su identificador.
     *
     * @param id identificador del turno (puede ser {@code null})
     * @return turno encontrado o {@link Optional#empty()}
     */
    public Optional<Turno> find(TurnoId id) {
        if (id == null) {
            return Optional.empty();
        }
        return turnos.stream()
                .filter(turno -> turno.id().equals(id))
                .findFirst();
    }

    /**
     * Devuelve el último número secuencial emitido por el motor.
     *
     * @return última secuencia emitida (0 si nunca se ha registrado un turno)
     */
    public long lastSequence() {
        return lastSequence;
    }

    /**
     * Devuelve todos los turnos administrados por el motor en orden ascendente de secuencia.
     *
     * @return lista inmodificable de turnos
     */
    public List<Turno> turnos() {
        return turnos;
    }

    private Optional<Turno> findInServiceByVentanilla(VentanillaId ventanilla) {
        return turnos.stream()
                .filter(turno -> turno.state() == TurnoState.IN_SERVICE
                        && turno.assignedVentanilla() == ventanilla)
                .findFirst();
    }

    private TurnoEngine replacing(Turno previous, Turno updated) {
        List<Turno> nextTurnos = new ArrayList<>(turnos.size());
        for (Turno item : turnos) {
            nextTurnos.add(item.id().equals(previous.id()) ? updated : item);
        }
        return new TurnoEngine(nextTurnos, lastSequence);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TurnoEngine that)) {
            return false;
        }
        return lastSequence == that.lastSequence && Objects.equals(turnos, that.turnos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(turnos, lastSequence);
    }

    @Override
    public String toString() {
        return "TurnoEngine[lastSequence=" + lastSequence + ", turnos=" + turnos + "]";
    }
}
