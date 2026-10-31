package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.ai.Move;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static ar.edu.uade.chess.core.TestHelper.sq;
import static org.junit.jupiter.api.Assertions.*;

/** La lógica de "clic en la pieza, clic en el destino", sin abrir ninguna ventana. */
class ClickToMoveTest {

    private final ClickToMove selector = new ClickToMove();

    @Test
    void clickingAPieceAndThenATarget_ProducesTheMove() {
        Optional<Move> first = selector.click(sq("e2"), Set.of(sq("e3"), sq("e4")));
        Optional<Move> second = selector.click(sq("e4"), Set.of());

        assertTrue(first.isEmpty());
        assertEquals(Optional.of(new Move(sq("e2"), sq("e4"))), second);
        assertNull(selector.getSelected(), "después de mover no queda nada seleccionado");
    }

    @Test
    void clickingAPiece_SelectsItAndHighlightsItsTargets() {
        selector.click(sq("g1"), Set.of(sq("f3"), sq("h3")));

        assertEquals(sq("g1"), selector.getSelected());
        assertEquals(Set.of(sq("f3"), sq("h3")), selector.getTargets());
    }

    @Test
    void clickingAnotherOwnPiece_ChangesTheSelection() {
        selector.click(sq("g1"), Set.of(sq("f3")));

        Optional<Move> result = selector.click(sq("b1"), Set.of(sq("c3")));

        assertTrue(result.isEmpty());
        assertEquals(sq("b1"), selector.getSelected());
    }

    @Test
    void clickingASquareThatIsNotATarget_CancelsTheSelection() {
        selector.click(sq("g1"), Set.of(sq("f3")));

        Optional<Move> result = selector.click(sq("a5"), Set.of());

        assertTrue(result.isEmpty());
        assertNull(selector.getSelected());
        assertTrue(selector.getTargets().isEmpty());
    }
}
