package ar.edu.uade.chess.core.movement;

import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Direction;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Movimiento de "un solo paso": la pieza llega directamente a cada destino,
 * sin importar lo que haya en el medio.
 *
 * Caballo = saltos en L, Rey = un paso en las 8 direcciones.
 */
public final class JumpMovement implements IMovementRule {

    private final List<Direction> jumps;

    public JumpMovement(List<Direction> jumps) {
        this.jumps = List.copyOf(jumps);
    }

    @Override
    public Set<Position> destinations(BoardView board, Position origin, Piece piece) {
        Set<Position> destinations = new LinkedHashSet<>();
        for (Direction jump : jumps) {
            Position destination = origin.offset(jump);
            if (!board.contains(destination)) {
                continue;
            }
            Piece occupant = board.pieceAt(destination);
            if (occupant == null || piece.isEnemyOf(occupant)) {
                destinations.add(destination);
            }
        }
        return destinations;
    }
}
