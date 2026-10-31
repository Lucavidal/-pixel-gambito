package ar.edu.uade.chess.core.model;

import ar.edu.uade.chess.core.movement.IMovementRule;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Una pieza de ajedrez armada por COMPOSICIÓN, no por herencia.
 *
 * No existe una clase Rook, Bishop o Queen: una pieza "es" lo que sus reglas de
 * movimiento dicen que es. La dama, por ejemplo, es simplemente una pieza con
 * una regla de deslizamiento en las 8 direcciones. Agregar una pieza nueva
 * (por ejemplo, una que combine torre y caballo) es combinar reglas que ya
 * existen, sin tocar ninguna clase.
 */
public final class Piece {

    private final String name;
    private final char symbol;
    private final Color color;
    private final boolean isKing;
    private final List<IMovementRule> movementRules;
    private boolean hasMoved;

    public Piece(String name, char symbol, Color color, boolean isKing, List<IMovementRule> movementRules) {
        this.name = Objects.requireNonNull(name);
        this.symbol = symbol;
        this.color = Objects.requireNonNull(color);
        this.isKing = isKing;
        this.movementRules = List.copyOf(movementRules);
        if (this.movementRules.isEmpty()) {
            throw new IllegalArgumentException("Una pieza necesita al menos una regla de movimiento");
        }
    }

    /**
     * Casillas a las que la pieza podría llegar según SUS PROPIAS reglas
     * (sin considerar todavía si deja al rey propio en jaque).
     * Es la unión de lo que aporta cada regla.
     */
    public Set<Position> possibleMoves(BoardView board, Position origin) {
        Set<Position> destinations = new LinkedHashSet<>();
        for (IMovementRule rule : movementRules) {
            destinations.addAll(rule.destinations(board, origin, this));
        }
        return destinations;
    }

    public boolean isEnemyOf(Piece other) {
        return other != null && other.color != this.color;
    }

    public String getName() { return name; }
    public char getSymbol() { return symbol; }
    public Color getColor() { return color; }
    public boolean isKing() { return isKing; }
    public boolean hasMoved() { return hasMoved; }

    /** La llama el comando de movimiento al ejecutarse. */
    public void markAsMoved() { this.hasMoved = true; }

    /** La llama el comando de movimiento al deshacerse, para volver al estado exacto anterior. */
    public void restoreHasMoved(boolean previousValue) { this.hasMoved = previousValue; }

    @Override
    public String toString() {
        return name + " " + color;
    }
}
