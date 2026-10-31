package ar.edu.uade.chess.core.movement;

import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Movimiento del peón, el único que es distinto para avanzar que para capturar:
 *  - avanza 1 casilla hacia adelante si está vacía;
 *  - en su primer movimiento puede avanzar 2, si ambas casillas están vacías;
 *  - captura solo en diagonal hacia adelante.
 *
 * "Adelante" lo define el color (Color.forwardDirection), así no hay un if por bando.
 */
public final class PawnMovement implements IMovementRule {

    @Override
    public Set<Position> destinations(BoardView board, Position origin, Piece piece) {
        Set<Position> destinations = new LinkedHashSet<>();
        int forward = piece.getColor().forwardDirection();

        Position oneStep = origin.offset(forward, 0);
        if (board.contains(oneStep) && board.isEmpty(oneStep)) {
            destinations.add(oneStep);

            Position twoSteps = origin.offset(2 * forward, 0);
            if (!piece.hasMoved() && board.contains(twoSteps) && board.isEmpty(twoSteps)) {
                destinations.add(twoSteps);
            }
        }

        for (int side : new int[]{-1, 1}) {
            Position diagonal = origin.offset(forward, side);
            if (board.contains(diagonal) && piece.isEnemyOf(board.pieceAt(diagonal))) {
                destinations.add(diagonal);
            }
        }
        return destinations;
    }
}
