package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lo que la interfaz recuerda de la partida para MOSTRARLA: la lista de jugadas,
 * las piezas capturadas y cuál fue la última jugada (para resaltarla).
 *
 * Es un detalle de presentación: el núcleo ya guarda su propio historial para
 * deshacer (CommandHistory) y no hace falta exponerlo. Esta clase acompaña
 * deshacer/rehacer con dos pilas, igual que CommandHistory, y no sabe nada de Swing.
 */
public class MoveLog {

    /** Una jugada ya hecha, con lo necesario para escribirla y mostrar capturas. */
    public record Entry(Move move, Piece moved, Piece captured, String suffix) { }

    private final AlgebraicNotation notation;
    private final List<Entry> done = new ArrayList<>();
    private final Deque<Entry> undone = new ArrayDeque<>();

    public MoveLog(AlgebraicNotation notation) {
        this.notation = notation;
    }

    /**
     * Registra una jugada nueva (anula lo que se podía rehacer, como en CommandHistory).
     * @param suffix "+" si dio jaque, "#" si dio mate, "" si no
     */
    public void record(Move move, Piece moved, Piece captured, String suffix) {
        done.add(new Entry(move, moved, captured, suffix));
        undone.clear();
    }

    public void undo() {
        if (!done.isEmpty()) {
            undone.push(done.remove(done.size() - 1));
        }
    }

    public void redo() {
        if (!undone.isEmpty()) {
            done.add(undone.pop());
        }
    }

    /** Hay alguna jugada deshecha que se puede rehacer. */
    public boolean canRedo() {
        return !undone.isEmpty();
    }

    public void clear() {
        done.clear();
        undone.clear();
    }

    public List<Entry> entries() {
        return List.copyOf(done);
    }

    /** Última jugada hecha, o null. */
    public Move lastMove() {
        return done.isEmpty() ? null : done.get(done.size() - 1).move();
    }

    /** Piezas de ese color que fueron capturadas, en el orden en que cayeron. */
    public List<Piece> capturedPiecesOf(Color color) {
        List<Piece> result = new ArrayList<>();
        for (Entry entry : done) {
            if (entry.captured() != null && entry.captured().getColor() == color) {
                result.add(entry.captured());
            }
        }
        return result;
    }

    /**
     * La jugada escrita para una persona: letra de la pieza (los peones no llevan),
     * origen, "-" o "x" si capturó, destino y "+" / "#". Ejemplos: "e2-e4", "Cg1-f3", "Dd1xd7#".
     * Usa la letra que define la pieza, así funciona con piezas nuevas.
     */
    public String text(Entry entry) {
        String letter = entry.moved().getSymbol() == 'P' ? "" : String.valueOf(entry.moved().getSymbol());
        return letter + notation.toText(entry.move().from())
                + (entry.captured() == null ? "-" : "x")
                + notation.toText(entry.move().to()) + entry.suffix();
    }

    /** Las jugadas agrupadas de a dos, como en una planilla: "1. e2-e4   e7-e5". */
    public List<String> numberedLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < done.size(); i += 2) {
            String white = text(done.get(i));
            String black = i + 1 < done.size() ? text(done.get(i + 1)) : "";
            lines.add(String.format("%3d.  %-10s %s", i / 2 + 1, white, black));
        }
        return lines;
    }
}
