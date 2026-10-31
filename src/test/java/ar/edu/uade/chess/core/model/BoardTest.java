package ar.edu.uade.chess.core.model;

import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import static ar.edu.uade.chess.core.TestHelper.sq;
import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    void movePiece_ToEmptySquare_LeavesOriginEmptyAndDestinationOccupied() {
        // Arrange
        Board board = new Board(8, 8);
        Piece rook = PieceFactory.rook(Color.WHITE);
        board.placePiece(rook, sq("a1"));

        // Act
        Piece captured = board.movePiece(sq("a1"), sq("a5"));

        // Assert
        assertNull(captured);
        assertTrue(board.isEmpty(sq("a1")));
        assertSame(rook, board.pieceAt(sq("a5")));
    }

    @Test
    void movePiece_OntoOpponentPiece_ReturnsCapturedPiece() {
        Board board = new Board(8, 8);
        Piece blackBishop = PieceFactory.bishop(Color.BLACK);
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));
        board.placePiece(blackBishop, sq("a5"));

        Piece captured = board.movePiece(sq("a1"), sq("a5"));

        assertSame(blackBishop, captured);
    }

    @Test
    void contains_PositionOutOfRange_ReturnsFalse() {
        Board board = new Board(8, 8);

        assertFalse(board.contains(new Position(8, 0)));
        assertFalse(board.contains(new Position(0, -1)));
        assertTrue(board.contains(new Position(7, 7)));
    }

    @Test
    void pieceAt_OutsideBoard_ThrowsException() {
        Board board = new Board(8, 8);

        assertThrows(IllegalArgumentException.class, () -> board.pieceAt(new Position(9, 9)));
    }

    @Test
    void board_CanHaveAnySize() {
        Board board = new Board(10, 12);

        assertEquals(10, board.rows());
        assertEquals(12, board.columns());
        assertTrue(board.contains(new Position(9, 11)));
    }

    @Test
    void kingPosition_FindsTheKingOfTheRequestedColor() {
        Board board = new Board(8, 8);
        board.placePiece(PieceFactory.king(Color.WHITE), sq("e1"));
        board.placePiece(PieceFactory.king(Color.BLACK), sq("e8"));

        assertEquals(sq("e1"), board.kingPosition(Color.WHITE));
        assertEquals(sq("e8"), board.kingPosition(Color.BLACK));
    }
}
