package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.setup.PieceFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/** Cada estilo (Strategy) elige lo que dice que prioriza. Se usa dificultad HARD para que no haya azar. */
class PlayStyleTest {

    private static Move choose(IPlayStyle style, Game game, long seed) {
        return new ComputerPlayer(style, Difficulty.HARD, new MoveAnalyzer(new PieceValue()), new Random(seed))
                .chooseMove(game);
    }

    @Test
    void aggressive_CapturesWhenACaptureIsAvailable() {
        for (long seed = 0; seed < 10; seed++) {
            Move move = choose(new AggressiveStyle(), ComputerPlayerTest.freeKnightPosition(), seed);

            assertEquals(new Move(sq("d1"), sq("d5")), move);
        }
    }

    @Test
    void defensive_DoesNotLeaveAPieceHangingWhenItHasAnAlternative() {
        // El caballo blanco de d4 está atacado por la torre negra de d8: mover el rey lo deja colgado.
        for (long seed = 0; seed < 10; seed++) {
            Board board = emptyBoard();
            board.placePiece(PieceFactory.king(Color.WHITE), sq("a1"));
            board.placePiece(PieceFactory.knight(Color.WHITE), sq("d4"));
            board.placePiece(PieceFactory.rook(Color.BLACK), sq("d8"));
            board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
            Game game = gameWith(board, Color.WHITE);

            Move move = choose(new DefensiveStyle(), game, seed);
            game.move(move.from(), move.to());

            assertEquals(sq("d4"), move.from(), "debería poner a salvo el caballo");
            assertTrue(game.legalMoves(sq("d8")).stream().noneMatch(move.to()::equals),
                    "el caballo no debe quedar al alcance de la torre");
        }
    }

    @Test
    void defensive_KeepsPiecesNextToItsKing() {
        MoveFacts sheltered = new MoveFacts(0, false, false, 0, 2);
        MoveFacts exposed = new MoveFacts(0, false, false, 0, 0);

        assertTrue(new DefensiveStyle().score(sheltered) > new DefensiveStyle().score(exposed));
    }

    @Test
    void balanced_PrefersASafeCaptureOverTheSameCaptureLeavingAPieceHanging() {
        MoveFacts safe = new MoveFacts(8, false, false, 0, 0);
        MoveFacts risky = new MoveFacts(8, false, false, 14, 0);

        assertTrue(new BalancedStyle().score(safe) > new BalancedStyle().score(risky));
        assertEquals(new AggressiveStyle().score(safe), new AggressiveStyle().score(risky),
                "al agresivo le da igual dejar piezas colgadas");
    }

    @Test
    void everyStyle_DeliversMateInOne() {
        for (IPlayStyle style : List.of(new AggressiveStyle(), new DefensiveStyle(), new BalancedStyle())) {
            Move move = choose(style, backRankMate(), 7);

            assertEquals(new Move(sq("a1"), sq("a8")), move, style.getDisplayName());
        }
    }

    @Test
    void analyzingAMove_LeavesTheGameExactlyAsItWas() {
        Game game = backRankMate();
        Map<?, ?> before = snapshot(game.getBoard());

        MoveFacts facts = new MoveAnalyzer(new PieceValue()).analyze(game, new Move(sq("a1"), sq("a8")));

        assertTrue(facts.givesCheckmate());
        assertEquals(before, snapshot(game.getBoard()));
        assertEquals(Color.WHITE, game.getTurn());
    }

    /** Mate de pasillo: Ta8# (el rey negro está encerrado por sus propios peones). */
    static Game backRankMate() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("g7"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("h7"));
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));
        board.placePiece(PieceFactory.king(Color.WHITE), sq("g1"));
        return gameWith(board, Color.WHITE);
    }
}
