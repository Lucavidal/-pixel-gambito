package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.model.Position;

import java.util.Optional;
import java.util.Set;

/**
 * Traduce clics sobre casillas en jugadas, sin saber nada de Swing:
 *  - primer clic sobre una pieza propia con movimientos: la selecciona y guarda sus destinos;
 *  - segundo clic sobre uno de esos destinos: devuelve la jugada;
 *  - clic en otra pieza propia: cambia la selección; clic en cualquier otro lado: la cancela.
 *
 * Está separada de la ventana para poder testearla sin abrir ninguna.
 */
public class ClickToMove {

    private Position selected;
    private Set<Position> targets = Set.of();

    /**
     * @param square         casilla clickeada
     * @param legalFromSquare destinos legales de la pieza en esa casilla si es del bando que mueve (vacío si no)
     * @return la jugada, si el clic completó una
     */
    public Optional<Move> click(Position square, Set<Position> legalFromSquare) {
        if (selected != null && targets.contains(square)) {
            Move move = new Move(selected, square);
            clear();
            return Optional.of(move);
        }
        if (!legalFromSquare.isEmpty()) {
            selected = square;
            targets = Set.copyOf(legalFromSquare);
        } else {
            clear();
        }
        return Optional.empty();
    }

    public void clear() {
        selected = null;
        targets = Set.of();
    }

    /** Casilla seleccionada, o null. */
    public Position getSelected() { return selected; }

    public Set<Position> getTargets() { return targets; }
}
