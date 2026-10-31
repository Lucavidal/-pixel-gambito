package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Direction;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.movement.JumpMovement;
import ar.edu.uade.chess.core.movement.SlidingMovement;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que se puede verificar de la interfaz gráfica sin mostrarla: qué casilla
 * corresponde a cada clic (en 8x8 y en 10x10) y qué se dibuja para cada pieza.
 */
class BoardPanelTest {

    // Panel de 560x560 con margen de 20: en 8x8 cada casilla mide (560 - 40) / 8 = 65 px.
    private static BoardPanel panelShowing(Board board) {
        BoardPanel panel = new BoardPanel(new PieceGlyphs());
        panel.setSize(560, 560);
        panel.showBoard(board, null, Set.of());
        return panel;
    }

    @Test
    void clickPosition_IsTranslatedToTheRightSquare_WithWhiteAtTheBottom() {
        BoardPanel panel = panelShowing(emptyBoard());

        assertEquals(sq("a1"), panel.positionAt(20 + 10, 20 + 7 * 65 + 10));
        assertEquals(sq("h8"), panel.positionAt(20 + 7 * 65 + 10, 20 + 10));
        assertEquals(sq("e4"), panel.positionAt(20 + 4 * 65 + 30, 20 + 4 * 65 + 30));
    }

    @Test
    void clickOutsideTheBoard_ReturnsNull() {
        BoardPanel panel = panelShowing(emptyBoard());

        assertNull(panel.positionAt(5, 5));
        assertNull(panel.positionAt(559, 559));
    }

    @Test
    void tenByTenBoard_UsesItsOwnSize() {
        // En 10x10 cada casilla mide (560 - 40) / 10 = 52 px; la columna 10 es la "j".
        BoardPanel panel = panelShowing(new Board(10, 10));

        assertEquals(new Position(9, 9), panel.positionAt(20 + 9 * 52 + 10, 20 + 10));
        assertEquals(new Position(0, 0), panel.positionAt(20 + 10, 20 + 9 * 52 + 10));
    }

    @Test
    void classicPieces_UseTheirFigure_AndANewPieceUsesItsLetter() {
        PieceGlyphs glyphs = new PieceGlyphs();
        Piece chancellor = new Piece("Canciller", 'K', Color.WHITE, false, List.of(
                new SlidingMovement(Direction.ORTHOGONAL), new JumpMovement(Direction.KNIGHT_JUMPS)));

        assertEquals("♔", glyphs.of(PieceFactory.king(Color.WHITE)));
        assertEquals("♞", glyphs.of(PieceFactory.knight(Color.BLACK)));
        assertEquals("K", glyphs.of(chancellor));
    }
}
