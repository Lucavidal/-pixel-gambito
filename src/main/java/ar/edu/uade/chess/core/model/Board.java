package ar.edu.uade.chess.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * El tablero: una grilla de casillas que pueden tener una pieza o estar vacías.
 *
 * El tamaño NO está fijo en 8x8: se recibe por constructor. Eso permite
 * responder a una extensión como "cambiar el tamaño del tablero" sin modificar
 * esta clase.
 *
 * El tablero solo sabe guardar y mover piezas; NO sabe si un movimiento es
 * legal (eso es responsabilidad de MoveValidator — Single Responsibility).
 */
public class Board implements BoardView {

    private final int rows;
    private final int columns;
    private final Piece[][] squares;

    public Board(int rows, int columns) {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("El tablero debe tener al menos una fila y una columna");
        }
        this.rows = rows;
        this.columns = columns;
        this.squares = new Piece[rows][columns];
    }

    @Override public int rows() { return rows; }
    @Override public int columns() { return columns; }

    @Override
    public boolean contains(Position p) {
        return p.row() >= 0 && p.row() < rows && p.column() >= 0 && p.column() < columns;
    }

    @Override
    public Piece pieceAt(Position p) {
        validateInside(p);
        return squares[p.row()][p.column()];
    }

    public void placePiece(Piece piece, Position p) {
        validateInside(p);
        squares[p.row()][p.column()] = piece;
    }

    /** Vacía la casilla y devuelve lo que había (o null). */
    public Piece removePiece(Position p) {
        Piece previous = pieceAt(p);
        squares[p.row()][p.column()] = null;
        return previous;
    }

    /**
     * Mueve lo que haya en from hacia to, sin validar reglas.
     * @return la pieza que estaba en to (capturada), o null.
     */
    public Piece movePiece(Position from, Position to) {
        Piece piece = removePiece(from);
        Piece captured = removePiece(to);
        placePiece(piece, to);
        return captured;
    }

    public List<Position> positionsOf(Color color) {
        List<Position> result = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                Piece piece = squares[r][c];
                if (piece != null && piece.getColor() == color) {
                    result.add(new Position(r, c));
                }
            }
        }
        return result;
    }

    /** Posición del rey de ese color, o null si no hay rey en el tablero. */
    public Position kingPosition(Color color) {
        for (Position p : positionsOf(color)) {
            if (pieceAt(p).isKing()) {
                return p;
            }
        }
        return null;
    }

    private void validateInside(Position p) {
        if (!contains(p)) {
            throw new IllegalArgumentException("Posición fuera del tablero: " + p);
        }
    }
}
