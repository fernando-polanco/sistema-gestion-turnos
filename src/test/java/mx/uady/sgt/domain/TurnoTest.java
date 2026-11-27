package mx.uady.sgt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pruebas de invariantes y transiciones de Turno")
class TurnoTest {

    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

    @Test
    @DisplayName("Registra un turno válido en estado En espera sin ventanilla")
    void shouldRegisterValidWaitingTurno() {
        Turno turno = Turno.register(1, NOW);

        assertEquals(new TurnoId("T001"), turno.id());
        assertEquals(1L, turno.sequence());
        assertEquals(NOW, turno.registeredAt());
        assertEquals(TurnoState.WAITING, turno.state());
        assertNull(turno.assignedVentanilla());
    }

    @Test
    @DisplayName("Ejecuta transiciones válidas devolviendo nuevas instancias inmutables")
    void shouldExecuteValidTransitionsImmutably() {
        Turno waiting = Turno.register(1, NOW);
        Turno inService = waiting.startService(VentanillaId.V02);
        Turno served = inService.finish();
        Turno cancelled = waiting.cancel();

        assertEquals(TurnoState.WAITING, waiting.state());
        assertNull(waiting.assignedVentanilla());

        assertEquals(TurnoState.IN_SERVICE, inService.state());
        assertEquals(VentanillaId.V02, inService.assignedVentanilla());

        assertEquals(TurnoState.SERVED, served.state());
        assertEquals(VentanillaId.V02, served.assignedVentanilla());

        assertEquals(TurnoState.CANCELLED, cancelled.state());
        assertNull(cancelled.assignedVentanilla());
    }

    @Test
    @DisplayName("Rechaza invariantes inválidas en el constructor compacto de Turno")
    void shouldRejectInvalidConstructorInvariants() {
        TurnoId t1 = TurnoId.fromSequence(1);
        TurnoId t2 = TurnoId.fromSequence(2);

        assertThrows(IllegalArgumentException.class,
                () -> new Turno(null, 1, NOW, TurnoState.WAITING, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, 0, NOW, TurnoState.WAITING, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, -5, NOW, TurnoState.WAITING, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, 1, null, TurnoState.WAITING, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, 1, NOW, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t2, 1, NOW, TurnoState.WAITING, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, 1, NOW, TurnoState.WAITING, VentanillaId.V01));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, 1, NOW, TurnoState.CANCELLED, VentanillaId.V01));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, 1, NOW, TurnoState.IN_SERVICE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Turno(t1, 1, NOW, TurnoState.SERVED, null));
        assertThrows(IllegalArgumentException.class,
                () -> Turno.register(1, null));
    }

    @Test
    @DisplayName("Lanza InvalidTransitionException ante transiciones ilegales en Turno")
    void shouldThrowOnIllegalTransitions() {
        Turno waiting = Turno.register(1, NOW);
        Turno inService = waiting.startService(VentanillaId.V01);
        Turno served = inService.finish();
        Turno cancelled = waiting.cancel();

        // Illegal startService calls
        assertThrows(InvalidTransitionException.class, () -> inService.startService(VentanillaId.V02));
        assertThrows(InvalidTransitionException.class, () -> served.startService(VentanillaId.V01));
        assertThrows(InvalidTransitionException.class, () -> cancelled.startService(VentanillaId.V01));
        assertThrows(IllegalArgumentException.class, () -> waiting.startService(null));

        // Illegal finish calls
        assertThrows(InvalidTransitionException.class, waiting::finish);
        assertThrows(InvalidTransitionException.class, served::finish);
        assertThrows(InvalidTransitionException.class, cancelled::finish);

        // Illegal cancel calls
        assertThrows(InvalidTransitionException.class, inService::cancel);
        assertThrows(InvalidTransitionException.class, served::cancel);
        assertThrows(InvalidTransitionException.class, cancelled::cancel);
    }

    @Test
    @DisplayName("Valida consistencia de la vista derivada Ventanilla")
    void shouldValidateVentanillaView() {
        Turno waiting = Turno.register(1, NOW);
        Turno inService = waiting.startService(VentanillaId.V01);

        Ventanilla free = new Ventanilla(VentanillaId.V01, Optional.empty());
        assertTrue(free.isFree());

        Ventanilla occupied = new Ventanilla(VentanillaId.V01, Optional.of(inService));
        assertFalse(occupied.isFree());

        assertThrows(IllegalArgumentException.class,
                () -> new Ventanilla(VentanillaId.V02, Optional.of(inService)));
        assertThrows(IllegalArgumentException.class,
                () -> new Ventanilla(VentanillaId.V01, Optional.of(waiting)));
    }
}
