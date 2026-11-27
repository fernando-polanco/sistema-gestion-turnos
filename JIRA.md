# Sprints y SCRUMs — construction-project

## Sprint 1 — Parte 1: Dominio y motor FIFO
**ID:** `36`
**Objetivo:** Entregar la arquitectura por capas, el modelo de dominio, los estados e invariantes, el motor FIFO y las tres ventanillas V01–V03. Incluye pruebas de dominio con JUnit 6 y Maven Surefire.

### SCRUMs

- **`SCRUM-6` — Parte 1: Modelo de dominio, estados e invariantes; cola FIFO**  
  Contexto: Implementar el modelo de turnos, sus estados y transiciones válidas, las invariantes del dominio y una cola única FIFO basada en secuencias crecientes. Las operaciones deben preservar la inmutabilidad y rechazar cambios inválidos.
- **`SCRUM-7` — Parte 1: Motor de ventanillas V01, V02 y V03**  
  Contexto: Implementar tres ventanillas fijas, asignación FIFO a ventanillas libres, control de ocupación, finalización de atención y consistencia entre turnos, fila y ventanillas.
- **`SCRUM-17` — Registrar un nuevo turno**  
  Contexto: Implementar el registro de turnos con estado inicial En espera y fecha/hora de registro.
- **`SCRUM-18` — Generar identificadores y secuencias crecientes**  
  Contexto: Definir la generación de identificadores únicos y números secuenciales monotónicos para los turnos.
- **`SCRUM-19` — Consultar la fila de espera**  
  Contexto: Exponer la fila de turnos pendientes ordenada según su secuencia de atención.
- **`SCRUM-20` — Implementar selección FIFO por secuencia**  
  Contexto: Seleccionar el turno En espera con la menor secuencia para garantizar el orden FIFO.
- **`SCRUM-21` — Validar operaciones del ciclo de vida del turno**  
  Contexto: Centralizar las validaciones de estados permitidos y garantizar que las operaciones rechazadas no modifiquen el estado.
- **`SCRUM-22` — Consultar las tres ventanillas**  
  Contexto: Representar V01, V02 y V03 como ventanillas lógicas fijas y permitir consultar su ocupación.
- **`SCRUM-23` — Asignar el siguiente turno a una ventanilla libre**  
  Contexto: Asignar el turno pendiente con menor secuencia a una ventanilla disponible.
- **`SCRUM-24` — Impedir asignaciones inválidas**  
  Contexto: Rechazar asignaciones cuando la fila está vacía, la ventanilla está ocupada o el turno ya fue asignado.
- **`SCRUM-25` — Finalizar la atención de un turno**  
  Contexto: Finalizar la atención de un turno y liberar la ventanilla correspondiente.
- **`SCRUM-26` — Impedir finalizaciones inválidas**  
  Contexto: Validar que solo se finalicen turnos En atención vinculados a la ventanilla correspondiente.
- **`SCRUM-27` — Mantener integridad de asignaciones entre turnos y ventanillas**  
  Contexto: Garantizar que un turno no esté asignado a más de una ventanilla y que una ventanilla no atienda más de un turno simultáneamente.

---

## Sprint 2 — Parte 2: Persistencia CSV
**ID:** `37`  
**Objetivo:** Implementar `TurnoRepository` y persistencia CSV con Apache Commons CSV, carga estricta, escritura atómica, recuperación de datos y protección de identificadores y secuencias.

### SCRUMs

- **`SCRUM-9` — Parte 2: Persistencia CSV segura e íntegra**  
  Contexto: Implementar el puerto `TurnoRepository` y su adaptador CSV. Gestionar `turnos.csv` y `secuencia.csv`, validar datos al cargarlos, realizar escrituras atómicas y conservar contadores para evitar reutilizar identificadores o secuencias.
- **`SCRUM-32` — Definir contrato CSV y puerto TurnoRepository**  
  Contexto: Definir el contrato de persistencia CSV y el puerto `TurnoRepository`, compatible con el modelo de dominio inmutable.
- **`SCRUM-33` — Implementar guardado de turnos y estados**  
  Contexto: Implementar el almacenamiento persistente de turnos, secuencias, estados y relaciones de atención.
- **`SCRUM-34` — Recuperar el estado al iniciar la aplicación**  
  Contexto: Cargar turnos, fila de espera, historial y turnos en atención al iniciar el sistema.
- **`SCRUM-35` — Validar integridad de los datos almacenados**  
  Contexto: Detectar datos inválidos y relaciones inconsistentes durante la lectura de los archivos persistentes.
- **`SCRUM-36` — Gestionar errores de lectura y escritura**  
  Contexto: Manejar errores de persistencia y comunicar cuando una operación no pudo completarse.
- **`SCRUM-37` — Asegurar atomicidad y consistencia de operaciones críticas**  
  Contexto: Evitar estados parciales durante las operaciones de asignación, finalización y cancelación.

---

## Sprint 3 — Parte 3: Servicios y pruebas
**ID:** `38`  
**Objetivo:** Implementar `TurnoService`, resultados tipados y casos de uso RF-01–RF-12. Completar pruebas CV-01–CV-10, rendimiento con hasta 200 turnos, prueba de arquitectura y matriz de trazabilidad.

### SCRUMs

- **`SCRUM-8` — Parte 3: Casos de uso de cancelación e historial**  
  Contexto: Implementar la cancelación de turnos En espera y la consulta del historial de turnos Atendidos y Cancelados, conservando los registros y rechazando transiciones inválidas.
- **`SCRUM-11` — Parte 3: Suite de pruebas, rendimiento y trazabilidad**  
  Contexto: Crear pruebas unitarias y de integración para RF-01–RF-12 y CV-01–CV-10, medir rendimiento con 200 turnos, verificar la arquitectura por capas y completar la matriz de trazabilidad.
- **`SCRUM-28` — Cancelar un turno en espera**  
  Contexto: Permitir cancelar un turno únicamente cuando su estado sea En espera.
- **`SCRUM-29` — Validar cancelación según estado**  
  Contexto: Rechazar cancelaciones de turnos En atención, Atendidos o ya Cancelados.
- **`SCRUM-31` — Consultar historial de turnos**  
  Contexto: Mostrar turnos Atendidos y Cancelados, conservando sus datos de registro y estado final.
- **`SCRUM-46` — Probar orden FIFO y casos de cola**  
  Contexto: Crear pruebas unitarias para el orden por secuencia, la cola vacía y la exclusión de turnos cancelados.
- **`SCRUM-47` — Probar transiciones de estado**  
  Contexto: Probar los estados En espera, En atención, Atendido y Cancelado, incluyendo el comportamiento de los estados terminales.
- **`SCRUM-48` — Probar asignación y finalización de ventanillas**  
  Contexto: Probar asignación FIFO, ventanillas ocupadas, límite de tres ventanillas y liberación después de finalizar la atención.
- **`SCRUM-49` — Probar cancelación y rechazos**  
  Contexto: Probar la cancelación de turnos pendientes y el rechazo de cancelaciones en atención o estados terminales.
- **`SCRUM-50` — Probar persistencia y recuperación tras reinicio**  
  Contexto: Verificar la recuperación de la fila, el historial, las asignaciones activas y los identificadores y secuencias.
- **`SCRUM-51` — Verificar rendimiento con 200 turnos**  
  Contexto: Medir el rendimiento de las operaciones normales con una carga de 200 turnos.
- **`SCRUM-52` — Ejecutar pruebas de integración del sistema**  
  Contexto: Validar los flujos completos entre JavaFX, servicios de dominio y persistencia, incluyendo los escenarios de CV-10.
- **`SCRUM-56` — Implementar TurnoService y resultados tipados**  
  Contexto: Implementar la capa de aplicación sobre el dominio y `TurnoRepository`, orquestar RF-01–RF-12 y devolver resultados `Confirmed`, `Rejected` o `PersistenceFailed`, con mensajes centralizados en español.
- **`SCRUM-57` — Completar prueba de arquitectura y matriz de trazabilidad**  
  Contexto: Crear una prueba automatizada que verifique las dependencias permitidas entre capas y completar la trazabilidad de RF-01–RF-12, CV-01–CV-10 y los RNF aplicables.

---

## Sprint 4 — Parte 4: JavaFX y entrega
**ID:** `39`  
**Objetivo:** Integrar la interfaz JavaFX mediante FXML y CSS, implementar un controlador delgado, conectar los servicios, proporcionar retroalimentación al usuario y preparar la versión final.

### SCRUMs

- **`SCRUM-10` — Parte 4: Interfaz JavaFX, composición y experiencia de usuario**  
  Contexto: Implementar la interfaz gráfica, la composición de la aplicación mediante `Main` y `SgtApplication`, una vista FXML con CSS y un controlador delgado. Integrar fila, ventanillas, registro, asignación, finalización, cancelación e historial.
- **`SCRUM-30` — Solicitar confirmación antes de cancelar**  
  Contexto: Incorporar una confirmación explícita en la interfaz antes de ejecutar la cancelación de un turno.
- **`SCRUM-38` — Construir ventana principal JavaFX**  
  Contexto: Crear la ventana principal y la navegación hacia los módulos de fila, ventanillas, registro e historial.
- **`SCRUM-39` — Construir vista de ventanillas**  
  Contexto: Mostrar V01, V02 y V03, su ocupación y las acciones disponibles para cada una.
- **`SCRUM-40` — Construir vista de fila de espera**  
  Contexto: Mostrar los turnos En espera ordenados por secuencia FIFO.
- **`SCRUM-41` — Construir controles de registro y asignación**  
  Contexto: Incorporar un formulario para registrar turnos y controles para asignar el siguiente turno a una ventanilla.
- **`SCRUM-42` — Construir controles de finalización y cancelación**  
  Contexto: Incorporar acciones para finalizar la atención y cancelar turnos pendientes.
- **`SCRUM-43` — Construir vista de historial**  
  Contexto: Presentar los turnos Atendidos y Cancelados en una vista de consulta.
- **`SCRUM-44` — Implementar mensajes y retroalimentación de operaciones**  
  Contexto: Mostrar mensajes claros en español para confirmar operaciones, informar rechazos y comunicar errores.
- **`SCRUM-45` — Sincronizar actualización de las vistas JavaFX**  
  Contexto: Actualizar las vistas de fila, ventanillas e historial después de operaciones exitosas.
- **`SCRUM-53` — Documentar instalación, ejecución y arquitectura**  
  Contexto: Crear el README con requisitos, comandos Maven, estructura del proyecto, persistencia y alcance funcional para facilitar la instalación y ejecución.
- **`SCRUM-54` — Preparar versión final 1.0.0**  
  Contexto: Verificar compilación, pruebas, configuración y empaquetado de la versión final 1.0.0.
- **`SCRUM-55` — Realizar revisión final del código y alcance**  
  Contexto: Revisar legibilidad, separación de responsabilidades, reglas de negocio y cumplimiento del alcance definido en el ERS.
