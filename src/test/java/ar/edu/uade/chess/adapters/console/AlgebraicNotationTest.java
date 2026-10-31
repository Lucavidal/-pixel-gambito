package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlgebraicNotationTest {

    private final AlgebraicNotation notation = new AlgebraicNotation();

    @Test
    void toPosition_TranslatesTextToPosition() {
        assertEquals(new Position(0, 0), notation.toPosition("a1"));
        assertEquals(new Position(3, 4), notation.toPosition("e4"));
        assertEquals(new Position(7, 7), notation.toPosition("H8"));
    }

    @Test
    void toPosition_SupportsBoardsWithMoreThan9Rows() {
        assertEquals(new Position(9, 9), notation.toPosition("j10"));
    }

    @Test
    void toText_TranslatesPositionToText() {
        assertEquals("e4", notation.toText(new Position(3, 4)));
    }

    @Test
    void invalidText_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> notation.toPosition("zz"));
        assertThrows(IllegalArgumentException.class, () -> notation.toPosition("4e"));
        assertThrows(IllegalArgumentException.class, () -> notation.toPosition(""));
    }
}
