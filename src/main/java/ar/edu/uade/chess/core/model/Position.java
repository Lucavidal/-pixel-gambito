package ar.edu.uade.chess.core.model;

/**
 * Una casilla del tablero, en coordenadas del núcleo: fila y columna empezando en 0.
 * La fila 0 es la del lado de las blancas.
 *
 * El núcleo NO conoce la notación algebraica ("e4"): traducir eso es trabajo
 * de un adaptador. Es inmutable (record), así que puede compartirse sin riesgo.
 * El record genera solo equals/hashCode, que es lo que el Position del profesor
 * escribe a mano para poder usarla como clave.
 */
public record Position(int row, int column) {

    /** Devuelve una nueva posición desplazada; no modifica la actual. */
    public Position offset(int rowDelta, int columnDelta) {
        return new Position(row + rowDelta, column + columnDelta);
    }

    public Position offset(Direction direction) {
        return offset(direction.rowDelta(), direction.columnDelta());
    }
}
