package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

/**
 * Dibuja el tablero como texto. Blancas en MAYÚSCULA, negras en minúscula,
 * casillas vacías con un punto. Solo usa BoardView (lectura).
 */
public class TextRenderer {

    public String render(BoardView board) {
        StringBuilder sb = new StringBuilder();
        String letters = header(board.columns());
        sb.append(letters).append('\n');
        for (int row = board.rows() - 1; row >= 0; row--) {
            sb.append(String.format("%2d ", row + 1));
            for (int col = 0; col < board.columns(); col++) {
                sb.append(symbol(board.pieceAt(new Position(row, col)))).append(' ');
            }
            sb.append(String.format("%d%n", row + 1));
        }
        sb.append(letters).append('\n');
        return sb.toString();
    }

    private String header(int columns) {
        StringBuilder sb = new StringBuilder("   ");
        for (int c = 0; c < columns; c++) {
            sb.append((char) ('a' + c)).append(' ');
        }
        return sb.toString();
    }

    private char symbol(Piece piece) {
        if (piece == null) {
            return '.';
        }
        return piece.getColor() == Color.WHITE
                ? Character.toUpperCase(piece.getSymbol())
                : Character.toLowerCase(piece.getSymbol());
    }
}
