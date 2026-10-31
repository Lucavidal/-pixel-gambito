package ar.edu.uade.chess.core.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El historial se prueba con un MOCK: un comando que solo guarda evidencia de
 * cómo lo usaron (como MockEmailSender en el ejemplo del profesor), para
 * verificarla en el Assert. No hace falta ningún tablero para probar
 * la lógica de deshacer/rehacer.
 */
class CommandHistoryTest {

    /** MOCK: registra cada llamada en una lista compartida, para verificarla después. */
    private static final class MockCommand implements ICommand {
        private final String name;
        private final List<String> calls;

        MockCommand(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override public void execute() { calls.add("execute " + name); }
        @Override public void undo() { calls.add("undo " + name); }
    }

    private CommandHistory history;
    private List<String> calls;

    @BeforeEach
    void setUp() {
        history = new CommandHistory();
        calls = new ArrayList<>();
    }

    @Test
    void undo_UndoesInReverseOrder() {
        // Arrange
        history.execute(new MockCommand("A", calls));
        history.execute(new MockCommand("B", calls));

        // Act
        history.undo();
        history.undo();

        // Assert
        assertEquals(List.of("execute A", "execute B", "undo B", "undo A"), calls);
    }

    @Test
    void redo_ExecutesTheUndoneCommandAgain() {
        history.execute(new MockCommand("A", calls));
        history.undo();

        assertTrue(history.redo());
        assertEquals("execute A", calls.get(calls.size() - 1));
        assertEquals(1, history.moveCount());
    }

    @Test
    void newCommand_ClearsTheRedoStack() {
        history.execute(new MockCommand("A", calls));
        history.undo();

        history.execute(new MockCommand("B", calls));

        assertFalse(history.canRedo());
    }

    @Test
    void undoWithEmptyHistory_ReturnsFalse() {
        assertFalse(history.undo());
        assertFalse(history.redo());
    }
}
