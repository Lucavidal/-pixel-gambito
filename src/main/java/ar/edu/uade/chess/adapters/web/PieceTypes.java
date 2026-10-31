package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.core.model.Piece;

import java.util.Map;

/**
 * Traduce una pieza del núcleo al "tipo" que entiende el front para elegir el modelo 3D.
 *
 * El núcleo no tiene clases Rook o Knight (las piezas se arman por composición),
 * así que se usa el símbolo que define cada pieza en PieceFactory. Una pieza nueva
 * con otro símbolo viaja como "unknown" y el front le pone un modelo genérico.
 */
public class PieceTypes {

    private static final Map<Character, String> BY_SYMBOL = Map.of(
            'P', "pawn",
            'T', "rook",
            'C', "knight",
            'A', "bishop",
            'D', "queen",
            'R', "king");

    public String typeOf(Piece piece) {
        return BY_SYMBOL.getOrDefault(piece.getSymbol(), "unknown");
    }
}
