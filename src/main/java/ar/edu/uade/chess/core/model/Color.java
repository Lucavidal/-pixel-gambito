package ar.edu.uade.chess.core.model;

/**
 * Bando de un jugador. Además de identificar el bando, sabe hacia dónde avanza
 * (las blancas suben de fila, las negras bajan), lo que evita ifs por color
 * dentro de las reglas de movimiento.
 */
public enum Color {
    WHITE(+1),
    BLACK(-1);

    private final int forwardDirection;

    Color(int forwardDirection) {
        this.forwardDirection = forwardDirection;
    }

    /** +1 si el bando avanza hacia filas mayores, -1 si avanza hacia filas menores. */
    public int forwardDirection() {
        return forwardDirection;
    }

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
