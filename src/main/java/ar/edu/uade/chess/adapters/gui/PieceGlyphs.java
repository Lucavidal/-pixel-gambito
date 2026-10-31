package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;

import java.util.Map;

/**
 * Elige qué dibujar para cada pieza. Es un detalle de PRESENTACIÓN, por eso vive
 * en el adaptador: el núcleo solo sabe el símbolo de la pieza ('R', 'D', ...).
 *
 * Las piezas clásicas se dibujan con su figura Unicode. Una pieza que no está en
 * la tabla (por ejemplo, el canciller 'K' de la defensa) se dibuja con su letra:
 * la interfaz no se rompe con piezas nuevas.
 */
public class PieceGlyphs {

    private static final Map<Character, String> WHITE = Map.of(
            'R', "♔", 'D', "♕", 'T', "♖", 'A', "♗", 'C', "♘", 'P', "♙");
    private static final Map<Character, String> BLACK = Map.of(
            'R', "♚", 'D', "♛", 'T', "♜", 'A', "♝", 'C', "♞", 'P', "♟");

    public String of(Piece piece) {
        char symbol = Character.toUpperCase(piece.getSymbol());
        Map<Character, String> table = piece.getColor() == Color.WHITE ? WHITE : BLACK;
        return table.getOrDefault(symbol, String.valueOf(symbol));
    }
}
