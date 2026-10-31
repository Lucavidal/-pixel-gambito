package ar.edu.uade.chess.core.ai;

import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.setup.PieceFactory;
import ar.edu.uade.chess.core.setup.StandardSetup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/** La IA: legalidad, determinismo con semilla y efecto de la dificultad. */
class ComputerPlayerTest {

    private static ComputerPlayer ai(IPlayStyle style, Difficulty difficulty, long seed) {
        return new ComputerPlayer(style, difficulty, new MoveAnalyzer(new PieceValue()), new Random(seed));
    }

    @Test
    void chosenMove_IsAlwaysLegal_InAWholeGameOfAiAgainstAi() {
        for (long seed = 0; seed < 5; seed++) {
            Game game = gameWith(new StandardSetup().createBoard(), Color.WHITE);
            ComputerPlayer white = ai(new AggressiveStyle(), Difficulty.EASY, seed);
            ComputerPlayer black = ai(new DefensiveStyle(), Difficulty.MEDIUM, seed + 100);

            for (int ply = 0; ply < 30 && !game.getStatus().isOver(); ply++) {
                ComputerPlayer current = game.getTurn() == Color.WHITE ? white : black;
                Move move = current.chooseMove(game);

                assertTrue(game.legalMoves(move.from()).contains(move.to()), "jugada ilegal: " + move);
                assertEquals(MoveResult.SUCCESS, game.move(move.from(), move.to()));
            }
        }
    }

    @Test
    void sameSeed_ChoosesTheSameMoves() {
        List<Move> first = playTenPlies(42);
        List<Move> second = playTenPlies(42);

        assertEquals(first, second);
    }

    private List<Move> playTenPlies(long seed) {
        Game game = gameWith(new StandardSetup().createBoard(), Color.WHITE);
        ComputerPlayer player = ai(new BalancedStyle(), Difficulty.EASY, seed);
        List<Move> moves = new ArrayList<>();
        for (int ply = 0; ply < 10; ply++) {
            Move move = player.chooseMove(game);
            game.move(move.from(), move.to());
            moves.add(move);
        }
        return moves;
    }

    @Test
    void neverChoosesAMoveThatLeavesItsOwnKingInCheck() {
        // La torre negra de e7 está clavada por la torre blanca de e1: capturar la dama de h7 es ilegal.
        ComputerPlayer player = ai(new AggressiveStyle(), Difficulty.EASY, 7);
        for (int i = 0; i < 20; i++) {
            Board board = emptyBoard();
            board.placePiece(PieceFactory.king(Color.BLACK), sq("e8"));
            board.placePiece(PieceFactory.rook(Color.BLACK), sq("e7"));
            board.placePiece(PieceFactory.rook(Color.WHITE), sq("e1"));
            board.placePiece(PieceFactory.queen(Color.WHITE), sq("h7"));
            board.placePiece(PieceFactory.king(Color.WHITE), sq("a1"));
            Game game = gameWith(board, Color.BLACK);

            Move move = player.chooseMove(game);

            assertNotEquals(new Move(sq("e7"), sq("h7")), move);
            assertEquals(MoveResult.SUCCESS, game.move(move.from(), move.to()));
            assertFalse(game.isInCheck(Color.BLACK));
        }
    }

    @Test
    void easyDifficulty_SometimesMissesTheBestMove_HardNever() {
        // Una misma IA decide 50 veces (como en una partida real): el azar avanza entre decisiones.
        ComputerPlayer easy = ai(new AggressiveStyle(), Difficulty.EASY, 2024);
        ComputerPlayer hard = ai(new AggressiveStyle(), Difficulty.HARD, 2024);
        int easyMisses = 0;
        int hardMisses = 0;
        for (int i = 0; i < 50; i++) {
            if (!easy.chooseMove(freeKnightPosition()).to().equals(sq("d5"))) {
                easyMisses++;
            }
            if (!hard.chooseMove(freeKnightPosition()).to().equals(sq("d5"))) {
                hardMisses++;
            }
        }

        assertTrue(easyMisses > 0, "el fácil debería equivocarse alguna vez");
        assertTrue(easyMisses < 50, "el fácil no debería equivocarse siempre");
        assertEquals(0, hardMisses);
    }

    /** Blancas pueden capturar gratis un caballo en d5 con la torre de d1. */
    static Game freeKnightPosition() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("a1"));
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("d1"));
        board.placePiece(PieceFactory.knight(Color.BLACK), sq("d5"));
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        return gameWith(board, Color.WHITE);
    }
}
