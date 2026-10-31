package ar.edu.uade.chess.core.model;

/**
 * Vista de SOLO LECTURA del tablero (Interface Segregation).
 *
 * Las reglas de movimiento y los adaptadores (por ejemplo, la consola que lo
 * dibuja) solo necesitan consultar el tablero, nunca modificarlo. Dándoles
 * esta interfaz en vez de la clase Board, es imposible que muevan una pieza
 * "por afuera" de la partida.
 */
public interface BoardView {

    int rows();

    int columns();

    boolean contains(Position position);

    /** Devuelve la pieza en esa casilla, o null si está vacía. */
    Piece pieceAt(Position position);

    default boolean isEmpty(Position position) {
        return pieceAt(position) == null;
    }
}
