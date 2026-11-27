package mx.uady.sgt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Pruebas de formato, generación y parseo de TurnoId")
class TurnoIdTest {

    @Test
    @DisplayName("Genera identificadores canónicos T%03d desde secuencia y soporta T999 -> T1000")
    void shouldGenerateFromSequence() {
        assertEquals(new TurnoId("T001"), TurnoId.fromSequence(1));
        assertEquals(new TurnoId("T042"), TurnoId.fromSequence(42));
        assertEquals(new TurnoId("T999"), TurnoId.fromSequence(999));
        assertEquals(new TurnoId("T1000"), TurnoId.fromSequence(1000));
    }

    @ParameterizedTest
    @DisplayName("Rechaza secuencias menores que 1 en fromSequence")
    @ValueSource(longs = {0L, -1L, -100L})
    void shouldRejectNonPositiveSequence(long sequence) {
        assertThrows(IllegalArgumentException.class, () -> TurnoId.fromSequence(sequence));
    }

    @Test
    @DisplayName("Parsea identificadores válidos aplicando trim y mayúsculas, incluyendo más de 3 dígitos")
    void shouldParseValidIdsWithTrimAndUppercase() {
        assertEquals(Optional.of(new TurnoId("T001")), TurnoId.parse("T001"));
        assertEquals(Optional.of(new TurnoId("T001")), TurnoId.parse("t001"));
        assertEquals(Optional.of(new TurnoId("T001")), TurnoId.parse("  t001  "));
        assertEquals(Optional.of(new TurnoId("T0001")), TurnoId.parse("T0001"));
        assertEquals(Optional.of(new TurnoId("T1000")), TurnoId.parse(" t1000 "));
    }

    @ParameterizedTest
    @DisplayName("Devuelve vacío al parsear nulo, vacío, espacios, T1, V04 o x")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "T1", "t1", "V04", "x", "T12", "A001", "T001A"})
    void shouldReturnEmptyWhenParsingInvalidIds(String raw) {
        assertTrue(TurnoId.parse(raw).isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Rechaza construcción directa con formato inválido")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "t001", " T001 ", "T1", "V04", "x"})
    void shouldRejectDirectConstructionWithInvalidFormat(String raw) {
        assertThrows(IllegalArgumentException.class, () -> new TurnoId(raw));
    }
}
