package ar.edu.uade.chess.core;

import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilidades SOLO para tests. Permiten escribir casillas como "e4" en los
 * tests para que se lean como ajedrez, sin que el núcleo dependa del adaptador
 * de notación.
 */
public final class TestHelper {

    private TestHelper() { }

    /** "e4" -> Position(3, 4) */
    public static Position sq(String square) {
        return new Position(square.charAt(1) - '1', square.charAt(0) - 'a');
    }

    public static Board emptyBoard() {
        return new Board(8, 8);
    }

    public static Game gameWith(Board board, Color turn) {
        CheckDetector detector = new CheckDetector();
        return new Game(board, new MoveValidator(detector), detector, new CommandHistory(), turn);
    }

    /**
     * Foto del tablero: qué pieza (identidad) hay en cada casilla.
     * Cumple el mismo rol que Board.Clone() + Board.Equals() en el ejemplo del profesor.
     */
    public static Map<Position, Piece> snapshot(BoardView board) {
        Map<Position, Piece> photo = new HashMap<>();
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.columns(); c++) {
                Position p = new Position(r, c);
                if (board.pieceAt(p) != null) {
                    photo.put(p, board.pieceAt(p));
                }
            }
        }
        return photo;
    }
}
