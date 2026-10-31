package ar.edu.uade.chess.core.game;

/**
 * Resultado de intentar un movimiento. Un enum (y no excepciones) porque un movimiento
 * inválido es parte normal del juego. Los mensajes son los que ve el jugador, en español.
 */
public enum MoveResult {
    SUCCESS("Movimiento realizado."),
    OUT_OF_BOARD("La casilla está fuera del tablero."),
    EMPTY_SQUARE("No hay ninguna pieza en la casilla de origen."),
    OPPONENT_PIECE("Esa pieza es del rival: no es tu turno."),
    INVALID_PIECE_MOVE("La pieza no puede moverse a esa casilla según sus reglas."),
    LEAVES_KING_IN_CHECK("Ese movimiento deja a tu rey en jaque."),
    GAME_OVER("La partida ya terminó.");

    private final String message;

    MoveResult(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }

    public boolean isSuccess() { return this == SUCCESS; }
}
