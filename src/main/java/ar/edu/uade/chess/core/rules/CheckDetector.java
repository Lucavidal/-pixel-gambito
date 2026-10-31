package ar.edu.uade.chess.core.rules;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

/**
 * Responde una sola pregunta: ¿el rey de este color está en jaque?
 *
 * Un rey está en jaque si alguna pieza rival puede llegar a su casilla según
 * sus propias reglas de movimiento. Como cada pieza ya sabe calcular sus
 * destinos, este detector no necesita conocer ningún tipo de pieza en
 * particular: funciona igual para piezas que se agreguen en el futuro.
 */
public class CheckDetector {

    public boolean isInCheck(Board board, Color color) {
        Position king = board.kingPosition(color);
        return king != null && isAttacked(board, king, color.opposite());
    }

    /** ¿Alguna pieza del bando atacante puede llegar a esta casilla? */
    public boolean isAttacked(Board board, Position square, Color attacker) {
        for (Position origin : board.positionsOf(attacker)) {
            Piece piece = board.pieceAt(origin);
            if (piece.possibleMoves(board, origin).contains(square)) {
                return true;
            }
        }
        return false;
    }
}
