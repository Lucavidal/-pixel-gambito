package ar.edu.uade.chess.core.movement;

import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import java.util.Set;

/**
 * Patrón STRATEGY: una forma de moverse, intercambiable y combinable.
 *
 * Cada pieza tiene una o más reglas. Cada regla responde una sola pregunta:
 * "desde este origen, ¿a qué casillas podría llegar esta pieza?".
 * Recibe una BoardView (solo lectura) porque nunca necesita modificar nada.
 */
public interface IMovementRule {

    Set<Position> destinations(BoardView board, Position origin, Piece piece);
}
