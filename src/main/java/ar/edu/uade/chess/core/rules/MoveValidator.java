package ar.edu.uade.chess.core.rules;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Decide si un movimiento es LEGAL. Un movimiento es legal si:
 *  1. el destino está entre los destinos posibles de la pieza (sus propias reglas), y
 *  2. después de hacerlo, el rey propio NO queda en jaque.
 *
 * El punto 2 se verifica "simulando" el movimiento sobre el tablero y
 * revirtiéndolo enseguida, sin efectos secundarios.
 *
 * Recibe el CheckDetector por constructor (inyección de dependencias).
 */
public class MoveValidator {

    private final CheckDetector checkDetector;

    public MoveValidator(CheckDetector checkDetector) {
        this.checkDetector = checkDetector;
    }

    public Set<Position> legalMoves(Board board, Position origin) {
        Set<Position> legal = new LinkedHashSet<>();
        Piece piece = board.pieceAt(origin);
        if (piece == null) {
            return legal;
        }
        for (Position destination : piece.possibleMoves(board, origin)) {
            if (!leavesKingInCheck(board, origin, destination, piece.getColor())) {
                legal.add(destination);
            }
        }
        return legal;
    }

    /**
     * ¿El destino respeta las reglas propias de la pieza? (no mira el jaque)
     * Es el mismo razonamiento que MoveValidator.IsValidMove del ejemplo del profesor.
     */
    public boolean isValidMove(Board board, Position origin, Position destination) {
        Piece piece = board.pieceAt(origin);
        return piece != null && piece.possibleMoves(board, origin).contains(destination);
    }

    public boolean leavesKingInCheck(Board board, Position origin, Position destination, Color color) {
        Piece captured = board.movePiece(origin, destination);   // simular
        boolean inCheck = checkDetector.isInCheck(board, color);
        board.movePiece(destination, origin);                    // revertir
        board.placePiece(captured, destination);
        return inCheck;
    }

    public boolean hasAnyLegalMove(Board board, Color color) {
        for (Position origin : board.positionsOf(color)) {
            if (!legalMoves(board, origin).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
