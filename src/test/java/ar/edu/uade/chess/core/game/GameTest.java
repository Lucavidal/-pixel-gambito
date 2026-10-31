package ar.edu.uade.chess.core.game;

import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.PieceFactory;
import ar.edu.uade.chess.core.setup.StandardSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/** Reglas de la partida completa: turnos, captura, validación, jaque, mate, ahogado, deshacer. */
class GameTest {

    private Game game;

    @BeforeEach
    void newStandardGame() {
        CheckDetector detector = new CheckDetector();
        game = new Game(new StandardSetup().createBoard(),
                new MoveValidator(detector), detector, new CommandHistory());
    }

    // ---------- Turnos ----------

    @Test
    void newGame_StartsWithWhiteAndInProgress() {
        assertEquals(Color.WHITE, game.getTurn());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }

    @Test
    void validMove_PassesTheTurnToTheOpponent() {
        MoveResult result = game.move(sq("e2"), sq("e4"));

        assertEquals(MoveResult.SUCCESS, result);
        assertEquals(Color.BLACK, game.getTurn());
        assertNull(game.getBoard().pieceAt(sq("e2")));
        assertEquals('P', game.getBoard().pieceAt(sq("e4")).getSymbol());
    }

    @Test
    void movingOpponentPiece_IsRejectedAndTurnDoesNotChange() {
        MoveResult result = game.move(sq("e7"), sq("e5"));

        assertEquals(MoveResult.OPPONENT_PIECE, result);
        assertEquals(Color.WHITE, game.getTurn());
    }

    // ---------- Validación de movimientos inválidos ----------

    @Test
    void movingFromEmptySquare_IsRejected() {
        assertEquals(MoveResult.EMPTY_SQUARE, game.move(sq("e4"), sq("e5")));
    }

    @Test
    void moveThePieceCannotMake_IsRejectedWithoutChangingAnything() {
        var boardBefore = snapshot(game.getBoard());

        MoveResult result = game.move(sq("a1"), sq("a5")); // la torre está bloqueada por el peón

        assertEquals(MoveResult.INVALID_PIECE_MOVE, result);
        assertEquals(boardBefore, snapshot(game.getBoard()));
        assertEquals(Color.WHITE, game.getTurn());
    }

    @Test
    void movingOutOfBoard_IsRejected() {
        assertEquals(MoveResult.OUT_OF_BOARD, game.move(sq("e2"), new Position(9, 4)));
    }

    // ---------- Captura ----------

    @Test
    void capture_ReplacesTheOpponentPiece() {
        game.move(sq("e2"), sq("e4"));
        game.move(sq("d7"), sq("d5"));

        MoveResult result = game.move(sq("e4"), sq("d5"));

        assertEquals(MoveResult.SUCCESS, result);
        assertEquals(Color.WHITE, game.getBoard().pieceAt(sq("d5")).getColor());
        assertEquals(15L, snapshot(game.getBoard()).values().stream().filter(p -> p.getColor() == Color.BLACK).count());
    }

    // ---------- Jaque y jaque mate ----------

    @Test
    void afterCheck_StatusIsCheckAndKingIsInCheck() {
        game.move(sq("e2"), sq("e4"));
        game.move(sq("f7"), sq("f6"));

        game.move(sq("d1"), sq("h5")); // jaque al rey negro

        assertEquals(GameStatus.CHECK, game.getStatus());
        assertTrue(game.isInCheck(Color.BLACK));
    }

    @Test
    void check_CannotBeIgnored() {
        game.move(sq("e2"), sq("e4"));
        game.move(sq("f7"), sq("f6"));
        game.move(sq("d1"), sq("h5"));

        MoveResult result = game.move(sq("a7"), sq("a6")); // no resuelve el jaque

        assertEquals(MoveResult.LEAVES_KING_IN_CHECK, result);
    }

    @Test
    void foolsMate_EndsTheGameInCheckmate() {
        game.move(sq("f2"), sq("f3"));
        game.move(sq("e7"), sq("e5"));
        game.move(sq("g2"), sq("g4"));

        game.move(sq("d8"), sq("h4"));

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
        assertEquals(Color.WHITE, game.getTurn(), "pierde el que tiene el turno");
        assertEquals(MoveResult.GAME_OVER, game.move(sq("a2"), sq("a3")));
    }

    @Test
    void kingWithNoMovesAndNotInCheck_IsStalemate() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.BLACK), sq("a8"));
        board.placePiece(PieceFactory.queen(Color.WHITE), sq("b6"));
        board.placePiece(PieceFactory.king(Color.WHITE), sq("h1"));

        Game stalemate = gameWith(board, Color.BLACK);

        assertEquals(GameStatus.STALEMATE, stalemate.getStatus());
    }

    // ---------- Deshacer / rehacer ----------

    @Test
    void undo_RestoresBoardAndTurn() {
        var boardBefore = snapshot(game.getBoard());
        game.move(sq("g1"), sq("f3"));

        assertTrue(game.undo());

        assertEquals(boardBefore, snapshot(game.getBoard()));
        assertEquals(Color.WHITE, game.getTurn());
    }

    @Test
    void undoCheckmate_ResumesTheGame() {
        game.move(sq("f2"), sq("f3"));
        game.move(sq("e7"), sq("e5"));
        game.move(sq("g2"), sq("g4"));
        game.move(sq("d8"), sq("h4"));

        game.undo();

        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(Color.BLACK, game.getTurn());
        assertTrue(game.redo());
        assertEquals(GameStatus.CHECKMATE, game.getStatus());
    }

    @Test
    void legalMoves_OfTheStartingKnight() {
        Set<Position> legal = game.legalMoves(sq("g1"));

        assertEquals(Set.of(sq("f3"), sq("h3")), legal);
    }
}
