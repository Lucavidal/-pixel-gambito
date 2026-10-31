package ar.edu.uade.chess.core.movement;

import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Direction;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Movimiento "de largo alcance": avanza casilla por casilla en cada dirección
 * hasta salir del tablero o chocar con una pieza. Si choca con una enemiga,
 * esa casilla también es destino (captura).
 *
 * Torre = ortogonales, Alfil = diagonales, Dama = todas.
 */
public final class SlidingMovement implements IMovementRule {

    private final List<Direction> directions;

    public SlidingMovement(List<Direction> directions) {
        this.directions = List.copyOf(directions);
    }

    @Override
    public Set<Position> destinations(BoardView board, Position origin, Piece piece) {
        Set<Position> destinations = new LinkedHashSet<>();
        for (Direction d : directions) {
            Position current = origin.offset(d);
            while (board.contains(current)) {
                Piece occupant = board.pieceAt(current);
                if (occupant == null) {
                    destinations.add(current);
                } else {
                    if (piece.isEnemyOf(occupant)) {
                        destinations.add(current);
                    }
                    break; // no se puede saltar por encima de otra pieza
                }
                current = current.offset(d);
            }
        }
        return destinations;
    }
}
