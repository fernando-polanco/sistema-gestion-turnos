package mx.uady.sgt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Pruebas de estados y transiciones de TurnoState")
class TurnoStateTest {

    @Test
    @DisplayName("Devuelve las etiquetas en español esperadas para cada estado")
    void shouldReturnExpectedLabels() {
        assertEquals("En espera", TurnoState.WAITING.label());
        assertEquals("En atención", TurnoState.IN_SERVICE.label());
        assertEquals("Atendido", TurnoState.SERVED.label());
        assertEquals("Cancelado", TurnoState.CANCELLED.label());
    }

    @Test
    @DisplayName("Identifica correctamente los estados terminales y no terminales")
    void shouldIdentifyTerminalStates() {
        assertFalse(TurnoState.WAITING.isTerminal());
        assertFalse(TurnoState.IN_SERVICE.isTerminal());
        assertTrue(TurnoState.SERVED.isTerminal());
        assertTrue(TurnoState.CANCELLED.isTerminal());
    }

    @ParameterizedTest(name = "{0} -> {1} debe ser {2}")
    @DisplayName("Valida la matriz completa 4x4 de transiciones permitidas e inválidas")
    @CsvSource({
        "WAITING, WAITING, false",
        "WAITING, IN_SERVICE, true",
        "WAITING, SERVED, false",
        "WAITING, CANCELLED, true",
        "IN_SERVICE, WAITING, false",
        "IN_SERVICE, IN_SERVICE, false",
        "IN_SERVICE, SERVED, true",
        "IN_SERVICE, CANCELLED, false",
        "SERVED, WAITING, false",
        "SERVED, IN_SERVICE, false",
        "SERVED, SERVED, false",
        "SERVED, CANCELLED, false",
        "CANCELLED, WAITING, false",
        "CANCELLED, IN_SERVICE, false",
        "CANCELLED, SERVED, false",
        "CANCELLED, CANCELLED, false"
    })
    void shouldValidateFullTransitionMatrix(TurnoState from, TurnoState to, boolean expected) {
        assertEquals(expected, from.canTransitionTo(to));
    }

    @Test
    @DisplayName("Rechaza transición hacia estado destino nulo")
    void shouldRejectNullTargetState() {
        for (TurnoState state : TurnoState.values()) {
            assertFalse(state.canTransitionTo(null));
        }
    }
}
