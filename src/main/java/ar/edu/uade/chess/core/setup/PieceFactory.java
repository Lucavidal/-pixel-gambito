package ar.edu.uade.chess.core.setup;

import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Direction;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.movement.JumpMovement;
import ar.edu.uade.chess.core.movement.PawnMovement;
import ar.edu.uade.chess.core.movement.SlidingMovement;

import java.util.List;

/**
 * Patrón FACTORY: el único lugar donde se decide con qué reglas se arma cada pieza.
 *
 * Justificación: se crean 32 piezas y cada una es una combinación de reglas.
 * Sin esta fábrica, esas "recetas" estarían repetidas por todo el código.
 * Agregar una pieza nueva = agregar un método acá (o armarla afuera combinando
 * reglas existentes); ninguna otra clase se modifica.
 *
 * Los nombres y símbolos ("Peón", 'P') son los que ve el jugador en la consola,
 * por eso quedan en español.
 */
public final class PieceFactory {

    private PieceFactory() { }

    public static Piece pawn(Color color) {
        return new Piece("Peón", 'P', color, false, List.of(new PawnMovement()));
    }

    public static Piece rook(Color color) {
        return new Piece("Torre", 'T', color, false,
                List.of(new SlidingMovement(Direction.ORTHOGONAL)));
    }

    public static Piece knight(Color color) {
        return new Piece("Caballo", 'C', color, false,
                List.of(new JumpMovement(Direction.KNIGHT_JUMPS)));
    }

    public static Piece bishop(Color color) {
        return new Piece("Alfil", 'A', color, false,
                List.of(new SlidingMovement(Direction.DIAGONAL)));
    }

    /** La dama no es "torre + alfil heredado": es una pieza con una regla de deslizamiento en 8 direcciones. */
    public static Piece queen(Color color) {
        return new Piece("Dama", 'D', color, false,
                List.of(new SlidingMovement(Direction.all())));
    }

    public static Piece king(Color color) {
        return new Piece("Rey", 'R', color, true,
                List.of(new JumpMovement(Direction.all())));
    }
}
