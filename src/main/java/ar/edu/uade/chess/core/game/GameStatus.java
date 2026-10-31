package ar.edu.uade.chess.core.game;

/**
 * Situación de la partida desde el punto de vista del jugador que tiene el turno.
 *
 * Es un enum y NO el patrón State a propósito: los estados no tienen
 * comportamiento propio distinto (solo informan), y las transiciones se
 * calculan a partir del tablero. Ver el documento de justificación.
 */
public enum GameStatus {
    IN_PROGRESS,
    CHECK,
    CHECKMATE,
    STALEMATE;

    public boolean isOver() {
        return this == CHECKMATE || this == STALEMATE;
    }
}
