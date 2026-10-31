package ar.edu.uade.chess.core.movement;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.edu.uade.chess.core.TestHelper.emptyBoard;
import static ar.edu.uade.chess.core.TestHelper.sq;
import static org.junit.jupiter.api.Assertions.*;

/** Movimiento propio de cada una de las 6 piezas, probado sin ninguna interfaz gráfica. */
class PieceMovementTest {

    private Set<Position> movesFrom(Board board, String square) {
        return board.pieceAt(sq(square)).possibleMoves(board, sq(square));
    }

    // ---------- Torre ----------

    @Test
    void rookOnEmptyBoard_Has14Moves() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("d4"));

        Set<Position> moves = movesFrom(board, "d4");

        assertEquals(14, moves.size());
        assertTrue(moves.contains(sq("d8")));
        assertTrue(moves.contains(sq("a4")));
        assertFalse(moves.contains(sq("e5")), "la torre no se mueve en diagonal");
    }

    @Test
    void rookBlockedByOwnPiece_CannotCaptureOrJumpIt() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("a3"));

        Set<Position> moves = movesFrom(board, "a1");

        assertTrue(moves.contains(sq("a2")));
        assertFalse(moves.contains(sq("a3")));
        assertFalse(moves.contains(sq("a4")));
    }

    @Test
    void rookFacingOpponentPiece_CanCaptureButNotJumpIt() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("a3"));

        Set<Position> moves = movesFrom(board, "a1");

        assertTrue(moves.contains(sq("a3")));
        assertFalse(moves.contains(sq("a4")));
    }

    // ---------- Alfil ----------

    @Test
    void bishopInCenter_Has13DiagonalMoves() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.bishop(Color.WHITE), sq("d4"));

        Set<Position> moves = movesFrom(board, "d4");

        assertEquals(13, moves.size());
        assertTrue(moves.contains(sq("h8")));
        assertTrue(moves.contains(sq("a1")));
        assertFalse(moves.contains(sq("d5")));
    }

    // ---------- Dama ----------

    @Test
    void queenInCenter_Has27Moves() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.queen(Color.WHITE), sq("d4"));

        assertEquals(27, movesFrom(board, "d4").size());
    }

    // ---------- Caballo ----------

    @Test
    void knightInCenter_Has8Moves() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.knight(Color.WHITE), sq("d4"));

        assertEquals(8, movesFrom(board, "d4").size());
    }

    @Test
    void knightInCorner_Has2Moves() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.knight(Color.WHITE), sq("a1"));

        assertEquals(Set.of(sq("b3"), sq("c2")), movesFrom(board, "a1"));
    }

    @Test
    void knight_JumpsOverOtherPieces() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.knight(Color.WHITE), sq("b1"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("b2"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("c2"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("a2"));

        Set<Position> moves = movesFrom(board, "b1");

        assertTrue(moves.contains(sq("a3")));
        assertTrue(moves.contains(sq("c3")));
    }

    // ---------- Rey ----------

    @Test
    void kingInCenter_Has8OneStepMoves() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("e4"));

        Set<Position> moves = movesFrom(board, "e4");

        assertEquals(8, moves.size());
        assertFalse(moves.contains(sq("e6")), "el rey avanza un solo paso");
    }

    // ---------- Peón ----------

    @Test
    void pawnNotMoved_CanAdvanceOneOrTwoSquares() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("e2"));

        assertEquals(Set.of(sq("e3"), sq("e4")), movesFrom(board, "e2"));
    }

    @Test
    void pawnAlreadyMoved_OnlyAdvancesOneSquare() {
        Board board = emptyBoard();
        Piece pawn = PieceFactory.pawn(Color.WHITE);
        pawn.markAsMoved();
        board.placePiece(pawn, sq("e3"));

        assertEquals(Set.of(sq("e4")), movesFrom(board, "e3"));
    }

    @Test
    void pawnBlockedInFront_CannotAdvanceOrCaptureForward() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("e2"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("e3"));

        assertTrue(movesFrom(board, "e2").isEmpty());
    }

    @Test
    void pawn_CapturesOnlyDiagonally() {
        Board board = emptyBoard();
        Piece pawn = PieceFactory.pawn(Color.WHITE);
        pawn.markAsMoved();
        board.placePiece(pawn, sq("e4"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("d5"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("f5")); // propia: no se captura

        assertEquals(Set.of(sq("e5"), sq("d5")), movesFrom(board, "e4"));
    }

    @Test
    void blackPawn_AdvancesTowardLowerRows() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("d7"));

        assertEquals(Set.of(sq("d6"), sq("d5")), movesFrom(board, "d7"));
    }
}
