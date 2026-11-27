package mx.uady.sgt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pruebas del motor inmutable TurnoEngine (FIFO, ventanillas e invariantes)")
class TurnoEngineTest {

    private static final Instant T0 = Instant.parse("2026-11-26T10:00:00Z");

    private static Transition requireSuccess(Result<Transition> result) {
        if (result instanceof Result.Success<Transition>(Transition transition)) {
            return transition;
        }
        return fail("Expected Success result, got: " + result);
    }

    private static TurnoEngine assertSuccess(Result<Transition> result) {
        return requireSuccess(result).newState();
    }

    private static Result.Rejection<Transition> assertRejection(
            Result<Transition> result, RejectionReason expectedReason) {
        if (result instanceof Result.Rejection<Transition> rejection) {
            assertEquals(expectedReason, rejection.reason());
            return rejection;
        }
        return fail("Expected Rejection result, got: " + result);
    }

    @Test
    @DisplayName("CV-01 y RN-01/03/06: registra T001, T002, T003 y asigna FIFO a V02 y V01")
    void shouldAssignFifoToSelectedFreeVentanillas() {
        TurnoEngine engine = TurnoEngine.empty();
        engine = assertSuccess(engine.register(T0));
        engine = assertSuccess(engine.register(T0.plusSeconds(10)));
        engine = assertSuccess(engine.register(T0.plusSeconds(20)));

        Transition s1 = requireSuccess(engine.assignNext(VentanillaId.V02));
        assertEquals(new TurnoId("T001"), s1.turno().id());
        assertEquals(TurnoState.IN_SERVICE, s1.turno().state());
        assertEquals(VentanillaId.V02, s1.turno().assignedVentanilla());
        engine = s1.newState();

        Transition s2 = requireSuccess(engine.assignNext(VentanillaId.V01));
        assertEquals(new TurnoId("T002"), s2.turno().id());
        assertEquals(TurnoState.IN_SERVICE, s2.turno().state());
        assertEquals(VentanillaId.V01, s2.turno().assignedVentanilla());
        engine = s2.newState();

        assertEquals(List.of(new TurnoId("T003")), engine.queue().stream().map(Turno::id).toList());
        List<Ventanilla> ventanillas = engine.ventanillas();
        assertEquals(List.of(VentanillaId.V01, VentanillaId.V02, VentanillaId.V03),
                ventanillas.stream().map(Ventanilla::id).toList());
        assertEquals(new TurnoId("T002"), ventanillas.get(0).currentTurno().orElseThrow().id());
        assertEquals(new TurnoId("T001"), ventanillas.get(1).currentTurno().orElseThrow().id());
        assertTrue(ventanillas.get(2).isFree());
    }

    @Test
    @DisplayName("CV-02 y D8: rechaza asignar en ventanilla ocupada antes que fila vacía y conserva estado")
    void shouldRejectAssignWhenVentanillaOccupiedEvenWithEmptyQueue() {
        TurnoEngine engine = TurnoEngine.empty();
        engine = assertSuccess(engine.register(T0));
        engine = assertSuccess(engine.assignNext(VentanillaId.V02));

        // Queue is empty AND V02 is occupied -> D8 requires VENTANILLA_OCCUPIED before QUEUE_EMPTY
        TurnoEngine snapshot = TurnoEngine.restore(engine.turnos(), engine.lastSequence());
        Result.Rejection<Transition> emptyQueueRejection =
                assertRejection(engine.assignNext(VentanillaId.V02), RejectionReason.VENTANILLA_OCCUPIED);
        assertEquals(new TurnoId("T001"), emptyQueueRejection.turnoId());
        assertEquals(VentanillaId.V02, emptyQueueRejection.ventanillaId());
        assertEquals(snapshot, engine);

        // Now with waiting turno in queue and V02 still occupied
        engine = assertSuccess(engine.register(T0.plusSeconds(5)));
        TurnoEngine beforeRejection = TurnoEngine.restore(engine.turnos(), engine.lastSequence());
        Result.Rejection<Transition> occupiedRejection =
                assertRejection(engine.assignNext(VentanillaId.V02), RejectionReason.VENTANILLA_OCCUPIED);
        assertEquals(new TurnoId("T001"), occupiedRejection.turnoId());
        assertEquals(VentanillaId.V02, occupiedRejection.ventanillaId());
        assertEquals(beforeRejection, engine);
        assertEquals(1, engine.queue().size());
    }

    @Test
    @DisplayName("RN-10: rechaza asignación con QUEUE_EMPTY cuando la ventanilla está libre y no hay pendientes")
    void shouldRejectAssignWhenQueueIsEmptyAndVentanillaFree() {
        TurnoEngine engine = TurnoEngine.empty();
        Result.Rejection<Transition> rejection =
                assertRejection(engine.assignNext(VentanillaId.V01), RejectionReason.QUEUE_EMPTY);
        assertEquals(VentanillaId.V01, rejection.ventanillaId());
        assertEquals(TurnoEngine.empty(), engine);
    }

    @Test
    @DisplayName("CV-03 y RN-04/05: con las tres ventanillas ocupadas y pendientes en fila, rechaza nueva asignación")
    void shouldRejectFourthAssignmentWhenAllThreeVentanillasOccupied() {
        TurnoEngine engine = TurnoEngine.empty();
        for (int i = 0; i < 5; i++) {
            engine = assertSuccess(engine.register(T0.plusSeconds(i)));
        }
        engine = assertSuccess(engine.assignNext(VentanillaId.V01));
        engine = assertSuccess(engine.assignNext(VentanillaId.V02));
        engine = assertSuccess(engine.assignNext(VentanillaId.V03));

        TurnoEngine snapshot = TurnoEngine.restore(engine.turnos(), engine.lastSequence());
        for (VentanillaId ventanilla : VentanillaId.values()) {
            assertRejection(engine.assignNext(ventanilla), RejectionReason.VENTANILLA_OCCUPIED);
        }

        assertEquals(snapshot, engine);
        assertEquals(List.of(new TurnoId("T004"), new TurnoId("T005")),
                engine.queue().stream().map(Turno::id).toList());
        assertTrue(engine.ventanillas().stream().noneMatch(Ventanilla::isFree));
    }

    @Test
    @DisplayName("CV-04 y RN-08: finaliza turno actual de la ventanilla y luego asigna el siguiente pendiente")
    void shouldFinishCurrentServiceAndAssignNextPending() {
        TurnoEngine engine = TurnoEngine.empty();
        engine = assertSuccess(engine.register(T0));
        engine = assertSuccess(engine.register(T0.plusSeconds(5)));
        engine = assertSuccess(engine.assignNext(VentanillaId.V02));

        Transition finishTransition = requireSuccess(engine.finishService(VentanillaId.V02));
        assertEquals(new TurnoId("T001"), finishTransition.turno().id());
        assertEquals(TurnoState.SERVED, finishTransition.turno().state());
        assertEquals(VentanillaId.V02, finishTransition.turno().assignedVentanilla());
        engine = finishTransition.newState();

        assertTrue(engine.ventanillas().get(1).isFree());

        // Reject finishing again on now-free V02
        TurnoEngine beforeFreeRejection = TurnoEngine.restore(engine.turnos(), engine.lastSequence());
        Result.Rejection<Transition> freeRejection =
                assertRejection(engine.finishService(VentanillaId.V02), RejectionReason.VENTANILLA_FREE);
        assertEquals(VentanillaId.V02, freeRejection.ventanillaId());
        assertEquals(beforeFreeRejection, engine);

        // Assign next pending to V02
        Transition reassign = requireSuccess(engine.assignNext(VentanillaId.V02));
        assertEquals(new TurnoId("T002"), reassign.turno().id());
        assertEquals(TurnoState.IN_SERVICE, reassign.turno().state());
    }

    @Test
    @DisplayName("CV-05 y RN-09: cancela turno en espera, conserva historial y mantiene orden FIFO en siguientes asignaciones")
    void shouldPreserveFifoOrderAfterCancellations() {
        TurnoEngine engine = TurnoEngine.empty();
        engine = assertSuccess(engine.register(T0));
        engine = assertSuccess(engine.register(T0.plusSeconds(1)));
        engine = assertSuccess(engine.register(T0.plusSeconds(2)));
        engine = assertSuccess(engine.register(T0.plusSeconds(3)));

        Transition cancelTransition = requireSuccess(engine.cancel(new TurnoId("T002")));
        assertEquals(TurnoState.CANCELLED, cancelTransition.turno().state());
        engine = cancelTransition.newState();

        assertEquals(List.of(new TurnoId("T001"), new TurnoId("T003"), new TurnoId("T004")),
                engine.queue().stream().map(Turno::id).toList());
        assertEquals(4, engine.history().size());
        assertEquals(TurnoState.CANCELLED,
                engine.find(new TurnoId("T002")).orElseThrow().state());

        Transition firstAssigned = requireSuccess(engine.assignNext(VentanillaId.V01));
        assertEquals(new TurnoId("T001"), firstAssigned.turno().id());
        engine = firstAssigned.newState();

        Transition secondAssigned = requireSuccess(engine.assignNext(VentanillaId.V02));
        assertEquals(new TurnoId("T003"), secondAssigned.turno().id());
        engine = secondAssigned.newState();

        Transition thirdAssigned = requireSuccess(engine.assignNext(VentanillaId.V03));
        assertEquals(new TurnoId("T004"), thirdAssigned.turno().id());
    }

    @Test
    @DisplayName("CV-06 y RN-07: rechaza cancelación de ID inexistente o de turnos IN_SERVICE, SERVED o CANCELLED")
    void shouldRejectInvalidCancellationsWithoutChangingState() {
        TurnoEngine engine = TurnoEngine.empty();
        engine = assertSuccess(engine.register(T0));
        engine = assertSuccess(engine.register(T0.plusSeconds(1)));
        engine = assertSuccess(engine.register(T0.plusSeconds(2)));
        engine = assertSuccess(engine.assignNext(VentanillaId.V01)); // T001 IN_SERVICE
        engine = assertSuccess(engine.assignNext(VentanillaId.V02)); // T002 IN_SERVICE
        engine = assertSuccess(engine.finishService(VentanillaId.V02)); // T002 SERVED
        engine = assertSuccess(engine.cancel(new TurnoId("T003"))); // T003 CANCELLED

        TurnoEngine snapshot = TurnoEngine.restore(engine.turnos(), engine.lastSequence());

        // Non-existent ID
        Result.Rejection<Transition> notFound =
                assertRejection(engine.cancel(new TurnoId("T999")), RejectionReason.TURNO_NOT_FOUND);
        assertEquals(new TurnoId("T999"), notFound.turnoId());

        // IN_SERVICE
        Result.Rejection<Transition> inServiceRejection =
                assertRejection(engine.cancel(new TurnoId("T001")), RejectionReason.TURNO_NOT_CANCELLABLE);
        assertEquals(new TurnoId("T001"), inServiceRejection.turnoId());
        assertEquals(VentanillaId.V01, inServiceRejection.ventanillaId());

        // SERVED
        assertRejection(engine.cancel(new TurnoId("T002")), RejectionReason.TURNO_NOT_CANCELLABLE);

        // CANCELLED
        assertRejection(engine.cancel(new TurnoId("T003")), RejectionReason.TURNO_NOT_CANCELLABLE);

        assertEquals(snapshot, engine);
        assertFalse(engine.ventanillas().get(0).isFree());
    }

    @Test
    @DisplayName("RN-02: transición T999 -> T1000 conserva el orden FIFO por secuencia numérica y no alfabético")
    void shouldPreserveFifoOrderAcrossT999ToT1000Boundary() {
        Turno t998 = Turno.register(998L, T0).startService(VentanillaId.V01).finish();
        TurnoEngine engine = TurnoEngine.restore(List.of(t998), 998L);

        engine = assertSuccess(engine.register(T0.plusSeconds(1))); // T999
        engine = assertSuccess(engine.register(T0.plusSeconds(2))); // T1000

        assertEquals(1000L, engine.lastSequence());
        assertEquals(List.of(new TurnoId("T999"), new TurnoId("T1000")),
                engine.queue().stream().map(Turno::id).toList());

        Transition assigned999 = requireSuccess(engine.assignNext(VentanillaId.V01));
        assertEquals(new TurnoId("T999"), assigned999.turno().id());
        engine = assigned999.newState();

        Transition assigned1000 = requireSuccess(engine.assignNext(VentanillaId.V02));
        assertEquals(new TurnoId("T1000"), assigned1000.turno().id());
    }

    @Test
    @DisplayName("Valida invariantes de restore ante duplicados, doble IN_SERVICE y lastSequence inválida")
    void shouldRejectInvalidRestoreStatesWithInvariantViolation() {
        Turno t1 = Turno.register(1L, T0);
        Turno t1Duplicate = Turno.register(1L, T0.plusSeconds(5));
        Turno t2InV01 = Turno.register(2L, T0.plusSeconds(1)).startService(VentanillaId.V01);
        Turno t3InV01 = Turno.register(3L, T0.plusSeconds(2)).startService(VentanillaId.V01);

        // Null collection / negative lastSequence / null element
        assertThrows(InvariantViolationException.class, () -> TurnoEngine.restore(null, 0L));
        assertThrows(InvariantViolationException.class, () -> TurnoEngine.restore(List.of(), -1L));
        assertThrows(InvariantViolationException.class,
                () -> TurnoEngine.restore(java.util.Collections.singletonList(null), 1L));

        // Duplicate id and sequence
        assertThrows(InvariantViolationException.class,
                () -> TurnoEngine.restore(List.of(t1, t1Duplicate), 1L));

        // Two IN_SERVICE on same ventanilla
        assertThrows(InvariantViolationException.class,
                () -> TurnoEngine.restore(List.of(t2InV01, t3InV01), 3L));

        // lastSequence less than max sequence
        assertThrows(InvariantViolationException.class,
                () -> TurnoEngine.restore(List.of(t1, t2InV01), 1L));

        // Valid restore sorts out-of-order input and keeps higher lastSequence
        TurnoEngine restored = TurnoEngine.restore(List.of(t2InV01, t1), 10L);
        assertEquals(10L, restored.lastSequence());
        assertEquals(List.of(new TurnoId("T001"), new TurnoId("T002")),
                restored.history().stream().map(Turno::id).toList());
    }

    @Test
    @DisplayName("Garantiza inmutabilidad del motor original y colecciones de consulta inmodificables")
    void shouldGuaranteeImmutabilityAndUnmodifiableViews() {
        TurnoEngine original = TurnoEngine.empty();
        TurnoEngine afterRegister = assertSuccess(original.register(T0));
        TurnoEngine afterAssign = assertSuccess(afterRegister.assignNext(VentanillaId.V01));
        TurnoEngine afterFinish = assertSuccess(afterAssign.finishService(VentanillaId.V01));

        assertEquals(0, original.turnos().size());
        assertEquals(0L, original.lastSequence());
        assertEquals(1, afterRegister.queue().size());
        assertEquals(0, afterAssign.queue().size());
        assertEquals(TurnoState.SERVED, afterFinish.history().getFirst().state());
        assertNotEquals(original, afterRegister);

        // Unmodifiable collections
        assertThrows(UnsupportedOperationException.class, () -> afterRegister.queue().clear());
        assertThrows(UnsupportedOperationException.class, () -> afterRegister.history().clear());
        assertThrows(UnsupportedOperationException.class, () -> afterRegister.turnos().clear());
        assertThrows(UnsupportedOperationException.class, () -> afterRegister.ventanillas().clear());

        // Safe find(null)
        assertTrue(afterRegister.find(null).isEmpty());
    }
}
