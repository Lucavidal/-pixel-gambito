package ar.edu.uade.chess.core.model;

import java.util.List;

/**
 * Un desplazamiento (rowDelta, columnDelta). Se usa tanto para direcciones
 * de deslizamiento (torre, alfil) como para saltos (caballo, rey).
 */
public record Direction(int rowDelta, int columnDelta) {

    public static final List<Direction> ORTHOGONAL = List.of(
            new Direction(1, 0), new Direction(-1, 0),
            new Direction(0, 1), new Direction(0, -1));

    public static final List<Direction> DIAGONAL = List.of(
            new Direction(1, 1), new Direction(1, -1),
            new Direction(-1, 1), new Direction(-1, -1));

    public static final List<Direction> KNIGHT_JUMPS = List.of(
            new Direction(2, 1), new Direction(2, -1),
            new Direction(-2, 1), new Direction(-2, -1),
            new Direction(1, 2), new Direction(1, -2),
            new Direction(-1, 2), new Direction(-1, -2));

    public static List<Direction> all() {
        return java.util.stream.Stream.concat(ORTHOGONAL.stream(), DIAGONAL.stream()).toList();
    }
}
