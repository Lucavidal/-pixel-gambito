package ar.edu.uade.chess.core.game;

import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;

import java.util.Set;

/**
 * PUERTO DE ENTRADA del núcleo.
 *
 * Es lo único que un adaptador (consola, interfaz gráfica, API web, una IA)
 * necesita conocer para jugar. El núcleo no sabe quién lo llama.
 */
public interface IGame {

    MoveResult move(Position from, Position to);

    boolean undo();

    boolean redo();

    Color getTurn();

    GameStatus getStatus();

    boolean isInCheck(Color color);

    /** Movimientos legales de la pieza en esa casilla (útil para resaltar casillas en una UI). */
    Set<Position> legalMoves(Position origin);

    /** Solo lectura: el adaptador puede dibujar el tablero, pero no modificarlo. */
    BoardView getBoard();
}
