package mx.uady.sgt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Verificación básica del entorno de pruebas")
class SmokeTest {

    @Test
    @DisplayName("Ejecuta prueba mínima con JUnit 6 y Surefire")
    void shouldRunMinimalSmokeAssertion() {
        assertTrue(true);
    }
}
