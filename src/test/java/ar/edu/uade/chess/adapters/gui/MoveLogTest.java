package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Direction;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.movement.JumpMovement;
import ar.edu.uade.chess.core.movement.SlidingMovement;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.uade.chess.core.TestHelper.sq;
import static org.junit.jupiter.api.Assertions.*;

/** La planilla de jugadas y capturas que muestra la interfaz, sin abrir ninguna ventana. */
class MoveLogTest {

    private final MoveLog log = new MoveLog(new AlgebraicNotation());

    @Test
    void movesAreWrittenLikeAScoresheet() {
        log.record(new Move(sq("e2"), sq("e4")), PieceFactory.pawn(Color.WHITE), null, "");
        log.record(new Move(sq("g8"), sq("f6")), PieceFactory.knight(Color.BLACK), null, "");
        log.record(new Move(sq("d1"), sq("d7")), PieceFactory.queen(Color.WHITE), PieceFactory.pawn(Color.BLACK), "+");

        assertEquals(List.of("  1.  e2-e4      Cg8-f6", "  2.  Dd1xd7+    "), log.numberedLines());
    }

    @Test
    void capturedPieces_AreListedByColor() {
        Piece blackPawn = PieceFactory.pawn(Color.BLACK);
        Piece whiteKnight = PieceFactory.knight(Color.WHITE);
        log.record(new Move(sq("d1"), sq("d7")), PieceFactory.queen(Color.WHITE), blackPawn, "");
        log.record(new Move(sq("e8"), sq("d7")), PieceFactory.king(Color.BLACK), whiteKnight, "");

        assertEquals(List.of(blackPawn), log.capturedPiecesOf(Color.BLACK));
        assertEquals(List.of(whiteKnight), log.capturedPiecesOf(Color.WHITE));
    }

    @Test
    void undoAndRedo_FollowTheGame_AndANewMoveDiscardsTheRedo() {
        Move first = new Move(sq("e2"), sq("e4"));
        Move second = new Move(sq("e7"), sq("e5"));
        log.record(first, PieceFactory.pawn(Color.WHITE), null, "");
        log.record(second, PieceFactory.pawn(Color.BLACK), null, "");

        log.undo();
        assertEquals(first, log.lastMove());
        log.redo();
        assertEquals(second, log.lastMove());

        log.undo();
        log.record(new Move(sq("d7"), sq("d5")), PieceFactory.pawn(Color.BLACK), null, "");
        log.redo(); // no hay nada para rehacer: la jugada nueva lo anuló
        assertEquals(2, log.entries().size());
        assertEquals(sq("d5"), log.lastMove().to());
    }

    @Test
    void aNewPiece_IsWrittenWithItsOwnLetter() {
        Piece chancellor = new Piece("Canciller", 'K', Color.WHITE, false, List.of(
                new SlidingMovement(Direction.ORTHOGONAL), new JumpMovement(Direction.KNIGHT_JUMPS)));
        log.record(new Move(sq("d4"), sq("e6")), chancellor, null, "#");

        assertEquals("Kd4-e6#", log.text(log.entries().get(0)));
    }

    @Test
    void emptyLog_HasNoLastMove() {
        assertNull(log.lastMove());
        assertTrue(log.numberedLines().isEmpty());
    }
}
