package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.adapters.web.WebDtos.MoveOptionDto;
import ar.edu.uade.chess.adapters.web.WebDtos.MoveResponse;
import ar.edu.uade.chess.adapters.web.WebDtos.PieceDto;
import ar.edu.uade.chess.adapters.web.WebDtos.StateDto;
import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.StandardSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** El adaptador web traduce el puerto IGame a DTOs; acá se prueba sin levantar el servidor. */
class WebGameSessionTest {

    private WebGameSession session;

    @BeforeEach
    void setUp() {
        CheckDetector detector = new CheckDetector();
        MoveValidator validator = new MoveValidator(detector);
        session = new WebGameSession(
                () -> new Game(new StandardSetup().createBoard(), validator, detector, new CommandHistory()),
                new AlgebraicNotation(), new PieceTypes());
    }

    @Test
    void initialState_Has32PiecesInPlace_AndWhiteToMove() {
        StateDto state = session.state();

        assertEquals(32, state.pieces().size());
        assertEquals("white", state.turn());
        assertEquals("IN_PROGRESS", state.status());
        assertNull(state.checkedKing());
        assertTrue(state.pieces().contains(new PieceDto("e1", "king", "white", "R", "Rey")));
        assertTrue(state.pieces().contains(new PieceDto("d8", "queen", "black", "D", "Dama")));
        assertTrue(state.pieces().contains(new PieceDto("g1", "knight", "white", "C", "Caballo")));
    }

    @Test
    void legalMoves_ComeFromTheCore() {
        assertEquals(List.of(new MoveOptionDto("e3", false), new MoveOptionDto("e4", false)),
                session.legalMoves("e2").moves());
        assertEquals(List.of(new MoveOptionDto("f3", false), new MoveOptionDto("h3", false)),
                session.legalMoves("g1").moves());
        assertTrue(session.legalMoves("e4").moves().isEmpty());
    }

    @Test
    void capture_IsMarkedInLegalMoves_AndListedAsCaptured() {
        play("e2", "e4", "d7", "d5");
        assertTrue(session.legalMoves("e4").moves().contains(new MoveOptionDto("d5", true)));

        MoveResponse response = session.move("e4", "d5");

        assertTrue(response.success());
        assertEquals(List.of("pawn"), response.state().capturedBlack());
        assertEquals(31, response.state().pieces().size());
        assertEquals("e4xd5", response.state().history().get(2).text());
        // Los datos de la jugada, para que el front la escriba en cualquier idioma
        WebDtos.PlayedMoveDto capture = response.state().history().get(2);
        assertEquals("pawn", capture.pieceType());
        assertTrue(capture.capture());
        assertEquals("", capture.suffix());
    }

    @Test
    void rejectedMove_ReturnsTheReasonFromTheCore_AndDoesNotChangeTheBoard() {
        MoveResponse wrongTurn = session.move("e7", "e5");
        MoveResponse invalid = session.move("e2", "e5");

        assertEquals("OPPONENT_PIECE", wrongTurn.result());
        assertEquals("INVALID_PIECE_MOVE", invalid.result());
        assertFalse(invalid.success());
        assertEquals("La pieza no puede moverse a esa casilla según sus reglas.", invalid.message());
        assertTrue(invalid.state().history().isEmpty());
    }

    @Test
    void check_ReportsTheKingSquare_AndTheScoresheetMarksIt() {
        // 1. e4 f5 2. Dh5+ : el rey negro queda en jaque
        play("e2", "e4", "f7", "f5");
        MoveResponse response = session.move("d1", "h5");

        assertEquals("CHECK", response.state().status());
        assertEquals("e8", response.state().checkedKing());
        assertEquals("Dd1-h5+", response.state().history().get(2).text());
        WebDtos.PlayedMoveDto check = response.state().history().get(2);
        assertEquals("queen", check.pieceType());
        assertFalse(check.capture());
        assertEquals("+", check.suffix());
    }

    @Test
    void undo_UsesTheCoreHistory_AndRestoresTheCapturedPiece() {
        play("e2", "e4", "d7", "d5", "e4", "d5");

        StateDto afterUndo = session.undo().state();

        assertEquals(32, afterUndo.pieces().size());
        assertEquals("white", afterUndo.turn());
        assertTrue(afterUndo.capturedBlack().isEmpty());
        assertTrue(afterUndo.canRedo());
        assertTrue(session.redo().success());
        assertEquals(31, session.state().pieces().size());
    }

    @Test
    void undo_WithNoMoves_DoesNothing() {
        assertFalse(session.undo().success());
    }

    @Test
    void reset_StartsANewGame() {
        play("e2", "e4");
        StateDto state = session.reset().state();
        assertEquals("white", state.turn());
        assertTrue(state.history().isEmpty());
        assertTrue(state.pieces().contains(new PieceDto("e2", "pawn", "white", "P", "Peón")));
    }

    @Test
    void badSquare_IsRejectedAsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> session.legalMoves("x"));
        assertEquals("OUT_OF_BOARD", session.move("e2", "e9").result());
    }

    private void play(String... squares) {
        for (int i = 0; i < squares.length; i += 2) {
            assertTrue(session.move(squares[i], squares[i + 1]).success(), squares[i] + "-" + squares[i + 1]);
        }
    }
}
