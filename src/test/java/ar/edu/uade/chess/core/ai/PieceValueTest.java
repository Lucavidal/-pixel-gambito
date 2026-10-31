package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El valor de una pieza sale de sus reglas de movimiento, no de una tabla por tipo. */
class PieceValueTest {

    private final PieceValue value = new PieceValue();

    @Test
    void classicPieces_KeepTheUsualOrder_WithoutATablePerType() {
        int queen = value.of(PieceFactory.queen(Color.WHITE), 8, 8);
        int rook = value.of(PieceFactory.rook(Color.WHITE), 8, 8);
        int bishop = value.of(PieceFactory.bishop(Color.WHITE), 8, 8);
        int knight = value.of(PieceFactory.knight(Color.WHITE), 8, 8);
        int pawn = value.of(PieceFactory.pawn(Color.WHITE), 8, 8);

        assertTrue(queen > rook && rook > bishop && bishop > knight && knight > pawn);
    }

    @Test
    void biggerBoard_ScalesTheValueByItself() {
        assertTrue(value.of(PieceFactory.rook(Color.WHITE), 10, 10) > value.of(PieceFactory.rook(Color.WHITE), 8, 8));
    }
}
