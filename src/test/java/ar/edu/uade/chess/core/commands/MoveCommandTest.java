package ar.edu.uade.chess.core.commands;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/** Test clásico de Command: estado antes de ejecutar == estado después de deshacer. */
class MoveCommandTest {

    @Test
    void moveCommand_ExecuteThenUndo_RestoresOriginalBoard() {
        // Arrange
        Board board = emptyBoard();
        board.placePiece(PieceFactory.knight(Color.WHITE), sq("g1"));
        var boardBefore = snapshot(board);
        MoveCommand command = new MoveCommand(board, sq("g1"), sq("f3"));

        // Act
        command.execute();
        var boardAfterExecute = snapshot(board);
        command.undo();

        // Assert
        assertNotEquals(boardBefore, boardAfterExecute);
        assertEquals(boardBefore, snapshot(board));
    }

    @Test
    void undoCapture_ReturnsCapturedPieceToItsSquare() {
        Board board = emptyBoard();
        Piece rook = PieceFactory.rook(Color.WHITE);
        Piece blackKnight = PieceFactory.knight(Color.BLACK);
        board.placePiece(rook, sq("a1"));
        board.placePiece(blackKnight, sq("a7"));
        MoveCommand command = new MoveCommand(board, sq("a1"), sq("a7"));

        command.execute();
        assertSame(blackKnight, command.getCapturedPiece());
        command.undo();

        assertSame(rook, board.pieceAt(sq("a1")));
        assertSame(blackKnight, board.pieceAt(sq("a7")));
    }

    @Test
    void undo_RestoresWhetherThePieceHadMoved() {
        Board board = emptyBoard();
        Piece pawn = PieceFactory.pawn(Color.WHITE);
        board.placePiece(pawn, sq("e2"));
        MoveCommand command = new MoveCommand(board, sq("e2"), sq("e4"));

        command.execute();
        assertTrue(pawn.hasMoved());
        command.undo();

        assertFalse(pawn.hasMoved(), "tras deshacer, el peón vuelve a poder avanzar dos casillas");
        assertTrue(pawn.possibleMoves(board, sq("e2")).contains(sq("e4")));
    }
}
