package ar.edu.uade.chess.core.extensibility;

import ar.edu.uade.chess.core.ai.AggressiveStyle;
import ar.edu.uade.chess.core.ai.BalancedStyle;
import ar.edu.uade.chess.core.ai.ComputerPlayer;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.ai.MoveAnalyzer;
import ar.edu.uade.chess.core.ai.PieceValue;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.game.MoveResult;
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
import java.util.Random;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Las extensiones de la defensa aplicadas a la IA: tablero de 10x10 y una pieza nueva.
 * Ni la IA ni el núcleo se modifican para que estos tests pasen.
 */
class AiExtensibilityTest {

    private static ComputerPlayer ai(long seed) {
        return new ComputerPlayer(new AggressiveStyle(), Difficulty.HARD, new MoveAnalyzer(new PieceValue()), new Random(seed));
    }

    /** La misma pieza nueva de ExtensibilityTest: torre + caballo. */
    private static Piece chancellor(Color color) {
        return new Piece("Canciller", 'K', color, false, List.of(
                new SlidingMovement(Direction.ORTHOGONAL),
                new JumpMovement(Direction.KNIGHT_JUMPS)));
    }

    @Test
    void ai_CapturesOnARowThatOnlyExistsInATenByTenBoard() {
        Board board = new Board(10, 10);
        board.placePiece(PieceFactory.king(Color.WHITE), new Position(0, 5));
        board.placePiece(PieceFactory.rook(Color.WHITE), new Position(0, 0));
        board.placePiece(PieceFactory.knight(Color.BLACK), new Position(9, 0)); // fila 10
        board.placePiece(PieceFactory.king(Color.BLACK), new Position(9, 9));
        Game game = gameWith(board, Color.WHITE);

        Move move = ai(1).chooseMove(game);

        assertEquals(new Move(new Position(0, 0), new Position(9, 0)), move);
    }

    @Test
    void ai_PlaysOnlyLegalMovesInATenByTenBoard() {
        Board board = new Board(10, 10);
        for (int column = 0; column < 10; column++) {
            board.placePiece(PieceFactory.pawn(Color.WHITE), new Position(1, column));
            board.placePiece(PieceFactory.pawn(Color.BLACK), new Position(8, column));
        }
        board.placePiece(PieceFactory.king(Color.WHITE), new Position(0, 5));
        board.placePiece(PieceFactory.queen(Color.WHITE), new Position(0, 4));
        board.placePiece(PieceFactory.king(Color.BLACK), new Position(9, 5));
        board.placePiece(chancellor(Color.BLACK), new Position(9, 4));
        Game game = gameWith(board, Color.WHITE);
        ComputerPlayer white = new ComputerPlayer(new BalancedStyle(), Difficulty.EASY, new MoveAnalyzer(new PieceValue()), new Random(3));
        ComputerPlayer black = ai(4);

        for (int ply = 0; ply < 20 && !game.getStatus().isOver(); ply++) {
            Move move = (game.getTurn() == Color.WHITE ? white : black).chooseMove(game);
            assertEquals(MoveResult.SUCCESS, game.move(move.from(), move.to()), "jugada ilegal: " + move);
        }
    }

    @Test
    void ai_UsesANewPieceBuiltFromExistingRules() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("a1"));
        board.placePiece(chancellor(Color.WHITE), sq("d4"));
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("e6")); // solo alcanzable con el salto de caballo
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        Game game = gameWith(board, Color.WHITE);

        Move move = ai(5).chooseMove(game);

        assertEquals(new Move(sq("d4"), sq("e6")), move);
    }

    @Test
    void newPiece_IsValuedFromItsRules() {
        PieceValue value = new PieceValue();

        assertTrue(value.of(chancellor(Color.WHITE), 8, 8) > value.of(PieceFactory.rook(Color.WHITE), 8, 8));
    }
}
