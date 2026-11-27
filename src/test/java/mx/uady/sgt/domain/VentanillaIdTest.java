package mx.uady.sgt.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Pruebas de parseo e invariantes de VentanillaId")
class VentanillaIdTest {

    @Test
    @DisplayName("Expone únicamente las tres ventanillas V01, V02 y V03")
    void shouldExposeExactlyThreeVentanillas() {
        assertEquals(3, VentanillaId.values().length);
        assertEquals(VentanillaId.V01, VentanillaId.values()[0]);
        assertEquals(VentanillaId.V02, VentanillaId.values()[1]);
        assertEquals(VentanillaId.V03, VentanillaId.values()[2]);
    }

    @Test
    @DisplayName("Parsea identificadores válidos en mayúsculas, minúsculas y con espacios")
    void shouldParseValidVentanillasWithTrimAndCaseInsensitivity() {
        assertEquals(Optional.of(VentanillaId.V01), VentanillaId.parse("V01"));
        assertEquals(Optional.of(VentanillaId.V02), VentanillaId.parse("v02"));
        assertEquals(Optional.of(VentanillaId.V03), VentanillaId.parse("  v03  "));
        assertEquals(Optional.of(VentanillaId.V01), VentanillaId.parse("\tV01\n"));
    }

    @ParameterizedTest
    @DisplayName("Devuelve vacío ante valores nulos, vacíos, espacios o identificadores inválidos")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "V04", "x", "V1", "T001", "V001"})
    void shouldReturnEmptyForInvalidInputs(String raw) {
        assertTrue(VentanillaId.parse(raw).isEmpty());
    }
}
