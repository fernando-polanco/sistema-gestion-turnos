package mx.uady.sgt.domain;

/**
 * Motivos tipados por los cuales el dominio o la capa de aplicación rechazan una operación.
 */
public enum RejectionReason {
    VENTANILLA_OCCUPIED,
    QUEUE_EMPTY,
    VENTANILLA_FREE,
    TURNO_NOT_FOUND,
    TURNO_NOT_CANCELLABLE,
    INVALID_INPUT
}
