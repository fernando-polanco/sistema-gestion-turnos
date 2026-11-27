# ESPECIFICACIÓN DE REQUISITOS DE SOFTWARE
## Sistema de Gestión de Turnos Universitarios (SGT)

Documento base para ADA-09 · Construcción de Software · Unidad III  
Versión 1.0 | 10 de noviembre de 2026

*Adaptación académica de la estructura SRS descrita en ISO/IEC/IEEE 29148:2018; características de calidad referenciadas a ISO/IEC 25010:2023. No constituye certificación ni reproducción íntegra del estándar.*

### Control del documento

| Campo | Valor |
|---|---|
| **Identificador** | ERS-SGT -001 |
| **Estado** | Propuesta de especificación para uso académico |
| **Versión** | 1.0 |
| **Fecha** | 10/11/2026 |
| **Destinatarios** | Equipos de ADA-09 y docente de Construcción de Software |
| **Alcance de la aprobación** | Describe el software por construir; no define todavía rúbrica ni actividad final |

### Guía de lectura
Cada requisito utiliza un identificador único: RF (funcional), RN (regla de negocio), RI (interfaz), RD (datos), RNF (no funcional) y RC (restricción). Las pruebas indicadas son criterios sugeridos de verificación, no procedimientos completos de prueba. “Deberá” indica obligatoriedad dentro de esta especificación.

---

## 1. Introducción

### 1.1 Propósito
Definir de forma verificable el comportamiento, las interfaces, los datos, las restricciones y las características de calidad del SGT. Esta especificación constituye el marco común de implementación para equipos que pueden escoger libremente su lenguaje, framework e infraestructura local.

### 1.2 Alcance del producto
Aplicación para administrar una cola única de turnos de atención con disciplina FIFO y tres ventanillas lógicas. Permite registrar turnos, consultar la fila, asignar el siguiente turno a una ventanilla libre, finalizar atenciones, cancelar turnos en espera y consultar el historial. Conserva la información tras cerrar y abrir la aplicación.

### 1.3 Referentes normativos y nivel de alineación
Se utiliza una estructura académica adaptada al contenido de la especificación de requisitos de software (SRS) descrito en ISO/IEC/IEEE 29148:2018: propósito, alcance, contexto, interfaces, requisitos, restricciones, verificabilidad y trazabilidad. Las características no funcionales se organizan con referencia al modelo de calidad de ISO/IEC 25010:2023. Las obligaciones y umbrales específicos de este documento son decisiones didácticas y no textos normativos de esos estándares.

### 1.4 Definiciones

| Término | Definición para el proyecto |
|---|---|
| **Turno** | Solicitud individual de atención identificada de forma única. |
| **FIFO** | First In, First Out: el turno en espera registrado antes se asigna primero. |
| **Ventanilla** | Punto lógico de atención identificado V01, V02 o V03; no representa otra computadora. |
| **En espera** | Turno creado, aún no asignado. |
| **En atención** | Turno asignado a una ventanilla y aún no finalizado. |
| **Atendido** | Turno cuya atención ha concluido. |
| **Cancelado** | Turno cerrado sin atención, solo desde el estado en espera. |
| **Historial** | Consulta de todos los turnos persistidos y su estado actual. |

---

## 2. Descripción general del sistema

### 2.1 Usuarios y ambiente
Usuario operativo: persona que maneja la aplicación para registrar y gestionar turnos. No se contemplan roles ni cuentas. Ambiente: una sola instalación o instancia operativa local que administra las tres ventanillas. La interfaz puede ser web local o de escritorio; no se requiere funcionamiento simultáneo desde múltiples computadoras.

### 2.2 Modelo conceptual
Una cola única conserva el orden de registro de los turnos pendientes. Cuando se solicita asignación desde una ventanilla libre, se elige el turno pendiente más antiguo. Pueden coexistir hasta tres turnos en atención, uno por ventanilla. Las ventanillas ocupadas no reciben otro turno hasta finalizar la atención actual.

### 2.3 Alcance excluido
Quedan excluidos: prioridad de atención, gestión dinámica de ventanillas, múltiples sedes, acceso concurrente desde distintos equipos, autenticación, perfiles de usuario, notificaciones, cálculo de tiempos de espera, indicadores, reportes, exportaciones, integración externa, despliegue público y mecanismos de atención automática en segundo plano.

### 2.4 Supuestos de alcance adoptados
Para convertir los acuerdos conceptuales en requisitos verificables se fijan estos detalles mínimos: tres ventanillas V01–V03 predefinidas; identificador de turno generado por el sistema; sin captura obligatoria de datos personales ni tipo de trámite; una sola operación a la vez en la aplicación; persistencia local; asignación de turno e inicio de atención en una misma acción; consulta histórica sin filtros avanzados. Estos son supuestos de especificación para la actividad y pueden revisarse antes de publicar la versión definitiva.

---

## 3. Modelo de datos y estados

### 3.1 Datos esenciales

| Elemento | Atributos mínimos | Observaciones |
|---|---|---|
| **Turno** | id, secuencia, fechaHoraRegistro, estado, ventanillaAsignada | El ID y la secuencia son únicos; ventanillaAsignada es nula antes de la asignación. |
| **Ventanilla** | id, turnoActual | Tres IDs fijos; turnoActual puede estar vacío. |
| **Persistencia** | Conjunto de turnos y relación de atención vigente | Debe recuperarse al reiniciar; se admite archivo o base local. |

### 3.2 Estados y transiciones permitidas

| Estado inicial | Operación | Estado final | Condición |
|---|---|---|---|
| — | Registrar turno | En espera | Se genera ID y orden de llegada. |
| En espera | Asignar siguiente | En atención | Ventanilla libre; turno más antiguo en espera. |
| En espera | Cancelar | Cancelado | El turno existe y sigue esperando. |
| En atención | Finalizar atención | Atendido | Es el turno actualmente asignado a esa ventanilla. |

“Atendido” y “Cancelado” son estados terminales. No existe transición de En atención a Cancelado, ni reapertura de un turno terminado. La disponibilidad de la ventanilla se deriva de la existencia o ausencia de un turno en atención asignado a ella.

### 3.3 Invariantes
- Un turno no puede estar asignado a más de una ventanilla.
- Una ventanilla no puede tener más de un turno En atención.
- En una asignación se elige el turno En espera con menor secuencia.
- Los turnos atendidos o cancelados permanecen en el historial, sin volver a la cola.
- Los ID y la secuencia de registro no se reutilizan después de reiniciar.

---

## 4. Requisitos funcionales

### RF-01 · Consultar ventanillas
El sistema deberá mostrar las ventanillas V01, V02 y V03 e indicar para cada una si está libre o qué turno se encuentra En atención.  
**Verificación:** Con un turno activo en V02, la consulta muestra V02 ocupada y V01/V03 libres.

### RF-02 · Registrar turno
El sistema deberá crear un turno con identificador y secuencia únicos, fecha y hora de registro, estado En espera y ventanilla sin asignar; lo incorporará al final de la cola.  
**Verificación:** Registrar tres turnos y comprobar el orden y unicidad de identificadores.

### RF-03 · Consultar fila
El sistema deberá presentar los turnos En espera en orden ascendente de secuencia, incluyendo su identificador y fecha/hora de registro.  
**Verificación:** Crear varios turnos, asignar y cancelar algunos, y comprobar que la cola solo incluye pendientes en orden FIFO.

### RF-04 · Asignar siguiente turno
Al solicitar atención desde una ventanilla libre, el sistema deberá seleccionar el turno En espera de menor secuencia, cambiar su estado a En atención y asociarlo exclusivamente a la ventanilla seleccionada.  
**Verificación:** Registrar T1,T2,T3; asignar desde V03 y después V01: deben asignarse T1 y T2, respectivamente.

### RF-05 · Rechazar asignaciones imposibles
El sistema deberá impedir la asignación cuando la ventanilla seleccionada esté ocupada o no existan turnos En espera; mostrará una explicación y conservará el estado previo.  
**Verificación:** Intentar asignar sobre ventanilla ocupada y con fila vacía; verificar que nada cambie.

### RF-06 · Finalizar atención
El sistema deberá permitir finalizar el turno En atención en una ventanilla seleccionada, cambiarlo a Atendido y dejar libre esa ventanilla.  
**Verificación:** Finalizar en V01 y comprobar estado del turno y disponibilidad de V01.

### RF-07 · Rechazar finalización inválida
El sistema deberá impedir finalizar una atención desde una ventanilla libre, sin modificar otros turnos o ventanillas.  
**Verificación:** Solicitar finalizar en V02 libre; verificar mensaje y ausencia de cambios.

### RF-08 · Cancelar turno pendiente
El sistema deberá permitir cancelar un turno únicamente si su estado es En espera, cambiándolo a Cancelado y retirándolo de la fila sin eliminar su historial.  
**Verificación:** Cancelar T2 en espera; comprobar que se excluye de la cola y permanece en historial.

### RF-09 · Rechazar cancelación inválida
El sistema deberá rechazar la cancelación de un ID inexistente o de un turno que no esté En espera y preservar el estado de la aplicación.  
**Verificación:** Intentar cancelar ID inexistente y turnos En atención, Atendido y Cancelado.

### RF-10 · Consultar historial simple
El sistema deberá permitir consultar todos los turnos registrados, incluidos En espera, En atención, Atendido y Cancelado, mostrando como mínimo ID, fecha/hora de registro, estado y ventanilla asignada cuando exista.  
**Verificación:** Crear un turno de cada estado y comprobar que los cuatro aparecen en consulta.

### RF-11 · Persistir y recuperar estados
El sistema deberá conservar los turnos, sus estados, secuencias y asignaciones de ventanillas al cerrar normalmente y reiniciar la aplicación.  
**Verificación:** Registrar, asignar, atender y cancelar; reiniciar y comparar datos, incluyendo ventanilla aún ocupada.

### RF-12 · Retroalimentación de operaciones
El sistema deberá confirmar las operaciones correctas e informar de forma clara aquellas rechazadas, sin indicar éxito cuando la persistencia necesaria haya fallado.  
**Verificación:** Provocar operación inválida y fallo controlado de escritura; comprobar mensajes y resultado.

---

## 5. Reglas de negocio

| ID | Regla vinculante |
|---|---|
| **RN-01** | Todos los turnos comparten una sola cola FIFO, sin prioridades. |
| **RN-02** | El orden FIFO se determina por una secuencia creciente asignada al registrar el turno; no por orden alfabético del ID ni por precisión del reloj. |
| **RN-03** | Solo existen las ventanillas V01, V02 y V03; están predefinidas. |
| **RN-04** | Solo puede existir un turno En atención por ventanilla. |
| **RN-05** | Un turno no puede estar En atención en dos ventanillas. |
| **RN-06** | Asignar un turno implica iniciar inmediatamente su atención. |
| **RN-07** | Un turno En espera puede cancelarse; uno En atención, Atendido o Cancelado no puede cancelarse. |
| **RN-08** | Solo un turno En atención puede finalizarse y pasar a Atendido. |
| **RN-09** | Los turnos Atendido y Cancelado no regresan a En espera ni se eliminan del historial. |
| **RN-10** | Si no existen turnos pendientes, una solicitud de asignación no altera los datos. |

---

## 6. Interfaces externas y requisitos de datos

### 6.1 Interfaz de usuario

**RI-01 · Operaciones visibles**  
La interfaz deberá ofrecer controles identificables para registrar turno, consultar fila, consultar ventanillas, asignar desde una ventanilla, finalizar, cancelar por ID y consultar historial.  
**Verificación:** Realizar cada operación usando exclusivamente la interfaz entregada.

**RI-02 · Visualización consistente**  
La interfaz deberá actualizar o permitir actualizar claramente la fila, el estado de ventanillas y el historial después de una operación; no deberá mostrar confirmaciones contradictorias.  
**Verificación:** Ejecutar asignaciones y finalizaciones y observar actualización coherente.

**RI-03 · Identificación de acciones**  
Los controles deberán usar etiquetas en español comprensibles y diferenciar “Asignar siguiente”, “Finalizar” y “Cancelar”.  
**Verificación:** Inspección de pantallas y uso de las operaciones.

### 6.2 Persistencia y datos

**RD-01 · Almacenamiento local**  
Los datos persistentes deberán almacenarse localmente en archivo o base de datos, sin necesidad de cuentas ni conectividad a servicios externos.  
**Verificación:** Ejecutar sin internet y reiniciar manteniendo registros.

**RD-02 · Integridad de operación**  
Una operación de asignación, finalización o cancelación deberá dejar consistentes turnos y ventanillas; ante un fallo de guardado, el sistema no deberá comunicar éxito ni dejar en almacenamiento una relación parcialmente aplicada.  
**Verificación:** Simular fallo controlado de guardado; comprobar estado recuperado.

**RD-03 · Validación de datos persistidos**  
Si al iniciar no es posible leer la información guardada o esta es inválida, el sistema deberá informar el problema y evitar sobrescribir silenciosamente los datos existentes.  
**Verificación:** Probar con archivo de persistencia dañado y verificar el tratamiento seguro.

---

## 7. Requisitos no funcionales

Los siguientes requisitos son criterios de calidad de producto seleccionados para el alcance académico. Se distinguen interacción/usabilidad, seguridad, mantenibilidad, fiabilidad y desempeño. Los umbrales operativos son metas didácticas propuestas, no valores prescritos por ISO/IEC 25010:2023.

### 7.1 Capacidad de interacción / usabilidad

**RNF-U01 · Operabilidad**  
Un usuario que conozca la descripción básica del sistema deberá poder registrar, asignar, finalizar y cancelar un turno desde la interfaz sin usar comandos internos ni editar archivos.  
**Verificación:** Recorrido funcional de las cuatro operaciones con la interfaz.

**RNF-U02 · Mensajes útiles**  
Ante acciones no permitidas, el sistema deberá explicar la causa (por ejemplo, ventanilla ocupada o turno no cancelable) e indicar el identificador implicado cuando corresponda.  
**Verificación:** Ejecutar cinco errores representativos y examinar mensajes.

**RNF-U03 · Estado identificable**  
La interfaz deberá distinguir visualmente y mediante texto los estados En espera, En atención, Atendido y Cancelado; no deberá depender exclusivamente de colores.  
**Verificación:** Inspección del historial y consulta de fila/ventanillas.

**RNF-U04 · Prevención de errores**  
Antes de cancelar un turno pendiente, la interfaz deberá solicitar confirmación explícita; una cancelación rechazada no deberá producir cambios.  
**Verificación:** Intentar cancelar y rechazar confirmación; estado inalterado.

### 7.2 Seguridad y protección de la información

**RNF-S01 · Validación en lógica de negocio**  
El sistema deberá aplicar las reglas de estado y asignación en la lógica del dominio, aun si una acción se invoca sin utilizar los controles habituales de la interfaz.  
**Verificación:** Pruebas de funciones o servicios con estados inválidos.

**RNF-S02 · Sin secretos embebidos**  
El código y la documentación entregados no deberán incluir contraseñas reales, tokens, claves API ni rutas a datos privados de los integrantes.  
**Verificación:** Revisión de repositorio, configuración y documentación.

**RNF-S03 · Alcance local**  
La solución deberá funcionar localmente sin exponer obligatoriamente datos ni operaciones del sistema a internet. No se requiere autenticación porque la especificación contempla un único operador local.  
**Verificación:** Comprobar funcionamiento desconectado de internet y configuración de red, si aplica.

**RNF-S04 · Manejo seguro de entradas**  
Los identificadores y demás entradas recibidas deberán validarse antes de usarse, evitando fallos no controlados o ejecución no intencionada de instrucciones sobre datos.  
**Verificación:** Probar ID vacíos, malformados e inexistentes; revisar acceso a datos.

### 7.3 Mantenibilidad y calidad de construcción

**RNF-M01 · Separación de responsabilidades**  
Las reglas FIFO y transiciones de estado deberán poder verificarse sin ejecutar la interfaz gráfica; se deberá evitar mezclar su implementación con código de presentación y almacenamiento.  
**Verificación:** Inspección de estructura y ejecución de pruebas unitarias aisladas.

**RNF-M02 · Estándar y legibilidad**  
El código deberá seguir un estándar de codificación identificado, con nombres significativos, métodos de responsabilidad clara y sin duplicación injustificada de reglas de negocio.  
**Verificación:** Revisión técnica por muestreo y contraste con estándar elegido.

**RNF-M03 · Pruebas automatizadas**  
El proyecto deberá incorporar pruebas unitarias automatizadas para FIFO, estados, asignación múltiple, cancelación, finalización y rechazos; las pruebas deberán poder ejecutarse mediante instrucciones documentadas.  
**Verificación:** Ejecutar batería de pruebas en el entorno entregado.

**RNF-M04 · Documentación mínima**  
El repositorio deberá contener instrucciones para preparar, ejecutar y probar la aplicación, además de explicaciones de decisiones técnicas no evidentes.  
**Verificación:** Seguir instrucciones en instalación limpia compatible.

**RNF-M05 · Revisión final**  
El equipo deberá identificar oportunidades reales de mejora de diseño o legibilidad encontradas al revisar su versión final, sin obligación de aplicar refactorización en esta actividad.  
**Verificación:** Registro breve de hallazgos, incluso si la revisión concluye que no hay cambios pertinentes.

### 7.4 Fiabilidad y eficiencia

**RNF-R01 · Recuperación tras reinicio**  
Después de un cierre normal, la siguiente ejecución deberá recuperar íntegramente las operaciones previamente confirmadas y preservar los turnos En atención.  
**Verificación:** Reinicio con turnos en cuatro estados.

**RNF-R02 · Integridad ante rechazos**  
Toda operación inválida deberá dejar inalterados los datos y el orden de la fila.  
**Verificación:** Comparar estado antes y después de operaciones rechazadas.

**RNF-P01 · Respuesta interactiva**  
Con hasta 200 turnos almacenados, las operaciones normales de registro, consulta de fila, asignación y finalización deberán responder en no más de 2 segundos en el equipo utilizado para la demostración, excluyendo el arranque inicial.  
**Verificación:** Medición local de operaciones con 200 turnos de prueba; registrar características del equipo.

---

## 8. Restricciones de diseño y operación

| ID | Restricción |
|---|---|
| **RC-01** | Lenguaje, framework y tipo de interfaz de libre elección del equipo. |
| **RC-02** | Un único operador local y una sola instancia funcional; no se requiere sincronización entre equipos. |
| **RC-03** | Tres ventanillas predefinidas; no se administran altas, bajas ni modificaciones. |
| **RC-04** | Persistencia local en archivo o base de datos; sin obligación de servidor externo. |
| **RC-05** | No se exige autenticación, autorización por roles ni procesamiento de datos personales de solicitantes. |
| **RC-06** | No se exige refactorización durante la construcción; se contempla revisión final de oportunidades. |
| **RC-07** | La aplicación debe poder ejecutarse y verificarse con los recursos y herramientas declarados en su documentación. |

---

## 9. Casos de verificación de referencia

| Caso | Preparación / acción | Resultado esperado |
|---|---|---|
| **CV-01** | Registrar T1, T2 y T3; asignar desde V02 y V01. | V02 recibe T1; V01 recibe T2; T3 permanece En espera. |
| **CV-02** | Con V02 ocupada, intentar asignar desde V02. | Rechazo, sin modificación del turno vigente ni de la fila. |
| **CV-03** | Con tres ventanillas ocupadas y turnos pendientes, intentar cuarta atención. | No se habilita una cuarta atención; los pendientes siguen en fila. |
| **CV-04** | Finalizar turno en V02 y asignar nuevamente desde V02. | La ventanilla queda libre; recibe el siguiente pendiente. |
| **CV-05** | Cancelar T2 mientras está En espera. | T2 figura Cancelado; siguiente asignación omite T2. |
| **CV-06** | Intentar cancelar un turno En atención. | Se rechaza; conserva turno y ventanilla ocupada. |
| **CV-07** | Reiniciar con T1 En atención y T2 En espera. | Se recuperan estados, asignación y secuencia; próximo turno conserva orden. |
| **CV-08** | Reiniciar luego de crear varios IDs. | No se reutiliza ningún identificador. |
| **CV-09** | Interrumpir guardado mediante mecanismo de prueba. | No se anuncia éxito; no se recuperan relaciones parciales. |
| **CV-10** | Mostrar turnos Atendido y Cancelado. | Ambos permanecen consultables en historial. |

---

## 10. Trazabilidad resumida

| Capacidad | Requisitos asociados | Casos de referencia |
|---|---|---|
| **Registro y cola** | RF-02, RF-03, RN-01, RN-02 | CV-01, CV-05 |
| **Ventanillas y asignación** | RF-01, RF-04, RF-05, RN-03–RN-06 | CV-01–CV-04 |
| **Finalización** | RF-06, RF-07, RN-08 | CV-04 |
| **Cancelación** | RF-08, RF-09, RN-07, RN-09 | CV-05, CV-06 |
| **Historial** | RF-10, RN-09 | CV-10 |
| **Persistencia** | RF-11, RD-01–RD-03, RNF-R01 | CV-07–CV-09 |
| **Calidad de producto** | RNF-U01–U04, RNF-S01–S04, RNF-M01–M05, RNF-R02, RNF-P01 | Inspección, pruebas y demostración |

---

## 11. Criterios de aceptación global

1. El conjunto de operaciones RF-01 a RF-12 es ejecutable mediante la interfaz, sin herramientas externas para el operador.
2. Los estados y restricciones de FIFO se cumplen de manera consistente, incluso con las tres ventanillas en atención.
3. Los turnos y asignaciones sobreviven a un cierre y reinicio normales.
4. Las pruebas unitarias previstas son ejecutables y cubren reglas de selección, estados y rechazos.
5. El código y la documentación permiten evaluar legibilidad, mantenimiento y trazabilidad de cambios.
6. No se agregan requisitos ajenos al alcance como autenticación, concurrencia distribuida o reportes avanzados.

---

## 12. Referencias

- **ISO/IEC/IEEE 29148:2018.** *Systems and software engineering — Life cycle processes — Requirements engineering*. ISO, IEEE, IEC. https://www.iso.org/standard/72089.html
- **ISO/IEC 25010:2023.** *Systems and software engineering — SQuaRE — Product quality model*. ISO. https://www.iso.org/standard/78176.html

*Nota de interpretación: el modelo ISO/IEC 25010:2023 utiliza la denominación “interaction capability” para la característica de calidad del producto relacionada con la interacción; en esta especificación se conserva también el término pedagógico “usabilidad” para facilitar su lectura. Los requisitos concretos, métodos de prueba y ejemplos son elaboración académica para SGT.*
