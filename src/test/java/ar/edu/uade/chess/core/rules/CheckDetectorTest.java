package ar.edu.uade.chess.core.rules;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ar.edu.uade.chess.core.TestHelper.emptyBoard;
import static ar.edu.uade.chess.core.TestHelper.sq;
import static org.junit.jupiter.api.Assertions.*;

class CheckDetectorTest {

    private CheckDetector detector;
    private Board board;

    @BeforeEach
    void setUp() {
        detector = new CheckDetector();
        board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("e1"));
    }

    @Test
    void kingInLineOfOpponentRook_IsInCheck() {
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("e8"));

        assertTrue(detector.isInCheck(board, Color.WHITE));
    }

    @Test
    void attackBlockedByAnotherPiece_IsNotCheck() {
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("e8"));
        board.placePiece(PieceFactory.bishop(Color.WHITE), sq("e4"));

        assertFalse(detector.isInCheck(board, Color.WHITE));
    }

    @Test
    void opponentKnight_GivesCheckEvenWithPiecesInBetween() {
        board.placePiece(PieceFactory.knight(Color.BLACK), sq("f3"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("e2"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("f2"));

        assertTrue(detector.isInCheck(board, Color.WHITE));
    }

    @Test
    void opponentPawnDiagonal_GivesCheck() {
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("d2"));

        assertTrue(detector.isInCheck(board, Color.WHITE));
    }

    @Test
    void opponentPawnDirectlyInFront_DoesNotGiveCheck() {
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("e2"));

        assertFalse(detector.isInCheck(board, Color.WHITE));
    }

    @Test
    void ownPieces_DoNotGiveCheck() {
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("e8"));

        assertFalse(detector.isInCheck(board, Color.WHITE));
    }

    @Test
    void noKingOnBoard_IsNotCheck() {
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));

        assertFalse(detector.isInCheck(board, Color.BLACK));
    }
}
