package ar.edu.uade.chess.core.rules;

import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

class MoveValidatorTest {

    private MoveValidator validator;
    private Board board;

    @BeforeEach
    void setUp() {
        validator = new MoveValidator(new CheckDetector());
        board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("e1"));
    }

    @Test
    void pinnedPiece_CannotLeaveTheLineOfAttack() {
        // El alfil en e2 tapa a la torre de e8: si se mueve, el rey queda en jaque.
        board.placePiece(PieceFactory.bishop(Color.WHITE), sq("e2"));
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("e8"));

        Set<Position> legal = validator.legalMoves(board, sq("e2"));

        assertTrue(legal.isEmpty());
        assertTrue(validator.leavesKingInCheck(board, sq("e2"), sq("d3"), Color.WHITE));
    }

    @Test
    void king_CannotMoveToAttackedSquare() {
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("d8"));

        Set<Position> legal = validator.legalMoves(board, sq("e1"));

        assertFalse(legal.contains(sq("d1")));
        assertFalse(legal.contains(sq("d2")));
        assertTrue(legal.contains(sq("f1")));
    }

    @Test
    void whileInCheck_OnlyMovesThatResolveItAreLegal() {
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("e8"));
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a4"));

        Set<Position> rookLegalMoves = validator.legalMoves(board, sq("a4"));

        assertEquals(Set.of(sq("e4")), rookLegalMoves, "la única jugada de la torre es interponerse");
    }

    @Test
    void validating_DoesNotModifyTheBoard() {
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("e8"));
        board.placePiece(PieceFactory.queen(Color.WHITE), sq("d4"));
        var before = snapshot(board);

        validator.legalMoves(board, sq("d4"));
        validator.hasAnyLegalMove(board, Color.WHITE);

        assertEquals(before, snapshot(board));
    }
}
