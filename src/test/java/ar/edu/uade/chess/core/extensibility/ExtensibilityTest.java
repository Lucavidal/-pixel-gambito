package ar.edu.uade.chess.core.extensibility;

import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Direction;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.movement.JumpMovement;
import ar.edu.uade.chess.core.movement.SlidingMovement;
import ar.edu.uade.chess.core.setup.IBoardSetup;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simula las "extensiones no programadas" de la defensa individual.
 * Ninguno de estos tests requiere modificar una sola línea del código del núcleo:
 * todo se logra COMBINANDO piezas existentes o IMPLEMENTANDO una interfaz.
 */
class ExtensibilityTest {

    /** Una pieza nueva que no existe en el ajedrez clásico: se mueve como torre + caballo. */
    private static Piece chancellor(Color color) {
        return new Piece("Canciller", 'K', color, false, List.of(
                new SlidingMovement(Direction.ORTHOGONAL),
                new JumpMovement(Direction.KNIGHT_JUMPS)));
    }

    @Test
    void newPiece_IsBuiltByCombiningExistingRules() {
        Board board = emptyBoard();
        board.placePiece(chancellor(Color.WHITE), sq("d4"));

        Set<Position> moves = board.pieceAt(sq("d4")).possibleMoves(board, sq("d4"));

        assertEquals(14 + 8, moves.size(), "14 de torre + 8 de caballo");
    }

    @Test
    void newPiece_WorksWithCheckDetectorWithoutTouchingIt() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("e1"));
        board.placePiece(PieceFactory.king(Color.BLACK), sq("a8"));
        board.placePiece(chancellor(Color.BLACK), sq("f3")); // ataca e1 con salto de caballo

        Game game = gameWith(board, Color.WHITE);

        assertEquals(GameStatus.CHECK, game.getStatus());
    }

    @Test
    void tenByTenBoard_WorksWithANewSetup() {
        IBoardSetup tenByTen = () -> {
            Board b = new Board(10, 10);
            b.placePiece(PieceFactory.king(Color.WHITE), new Position(0, 5));
            b.placePiece(PieceFactory.king(Color.BLACK), new Position(9, 5));
            b.placePiece(PieceFactory.rook(Color.WHITE), new Position(0, 0));
            return b;
        };

        Game game = gameWith(tenByTen.createBoard(), Color.WHITE);
        // Fila 8 (la novena): no existe en un tablero de 8x8.
        MoveResult result = game.move(new Position(0, 0), new Position(8, 0));

        assertEquals(MoveResult.SUCCESS, result);
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(10, game.getBoard().rows());
    }
}
