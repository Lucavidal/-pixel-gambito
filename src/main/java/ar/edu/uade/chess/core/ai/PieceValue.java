package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

/**
 * Cuánto "vale" una pieza, SIN conocer su tipo.
 *
 * El valor es su movilidad: a cuántas casillas puede llegar desde el centro de
 * un tablero vacío del mismo tamaño, según sus propias reglas. En 8x8 da
 * dama 27, torre 14, alfil 13, caballo 8, peón 1-2, que respeta el orden clásico.
 *
 * Como no hay una tabla por tipo de pieza, una pieza nueva (por ejemplo, el
 * canciller = torre + caballo) o un tablero de 10x10 se valoran solos.
 */
public class PieceValue {

    public int of(Piece piece, int rows, int columns) {
        Board empty = new Board(rows, columns);
        Position center = new Position(rows / 2, columns / 2);
        return piece.possibleMoves(empty, center).size();
    }
}
