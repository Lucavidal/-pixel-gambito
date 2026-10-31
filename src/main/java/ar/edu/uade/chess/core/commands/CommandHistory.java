package ar.edu.uade.chess.core.commands;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Historial de comandos con dos pilas: hechos y deshechos.
 *  - execute: ejecuta, apila en "hechos" y borra "deshechos" (una jugada nueva anula el rehacer).
 *  - undo:    saca de "hechos", deshace y pasa a "deshechos".
 *  - redo:    saca de "deshechos", ejecuta y vuelve a "hechos".
 */
public class CommandHistory {

    private final Deque<ICommand> done = new ArrayDeque<>();
    private final Deque<ICommand> undone = new ArrayDeque<>();

    public void execute(ICommand command) {
        command.execute();
        done.push(command);
        undone.clear();
    }

    public boolean canUndo() { return !done.isEmpty(); }

    public boolean canRedo() { return !undone.isEmpty(); }

    public boolean undo() {
        if (!canUndo()) {
            return false;
        }
        ICommand command = done.pop();
        command.undo();
        undone.push(command);
        return true;
    }

    public boolean redo() {
        if (!canRedo()) {
            return false;
        }
        ICommand command = undone.pop();
        command.execute();
        done.push(command);
        return true;
    }

    public int moveCount() { return done.size(); }
}
