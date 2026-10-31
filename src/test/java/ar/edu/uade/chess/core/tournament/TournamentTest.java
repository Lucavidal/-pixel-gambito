package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.ai.IMoveChooser;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.setup.PieceFactory;
import ar.edu.uade.chess.core.setup.StandardSetup;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas del torneo escalera. Las partidas son posiciones armadas (a través de
 * la fábrica de partidas) y el rival juega jugadas fijas, así cada test controla
 * exactamente cómo termina la ronda. No hay consola ni azar.
 */
class TournamentTest {

    /** Rival "guionado": juega las jugadas que se le dieron y, si se le acaban, la primera legal. */
    static final class ScriptedOpponent implements IMoveChooser {
        private final Deque<Move> script;
        int movesPlayed;

        ScriptedOpponent(Move... moves) {
            this.script = new ArrayDeque<>(List.of(moves));
        }

        @Override
        public Move chooseMove(IGame game) {
            movesPlayed++;
            if (!script.isEmpty()) {
                return script.poll();
            }
            for (int r = 0; r < game.getBoard().rows(); r++) {
                for (int c = 0; c < game.getBoard().columns(); c++) {
                    Position origin = new Position(r, c);
                    var piece = game.getBoard().pieceAt(origin);
                    if (piece != null && piece.getColor() == game.getTurn() && !game.legalMoves(origin).isEmpty()) {
                        return new Move(origin, game.legalMoves(origin).iterator().next());
                    }
                }
            }
            throw new IllegalStateException("sin jugadas");
        }
    }

    /** Mate de pasillo: el usuario (blancas) gana con a1 a8. */
    static Game userMatesInOne() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("g7"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("h7"));
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));
        board.placePiece(PieceFactory.king(Color.WHITE), sq("g1"));
        return gameWith(board, Color.WHITE);
    }

    /** Si el usuario juega b1 a3, el rival da mate con e8 e1. */
    private static Game userGetsMatedAfterB1A3() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("h1"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("g2"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("h2"));
        board.placePiece(PieceFactory.knight(Color.WHITE), sq("b1"));
        board.placePiece(PieceFactory.rook(Color.BLACK), sq("e8"));
        board.placePiece(PieceFactory.king(Color.BLACK), sq("a8"));
        return gameWith(board, Color.WHITE);
    }

    /** Si el usuario juega g5 g6, el rey negro queda ahogado. */
    private static Game userStalematesWithG5G6() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        board.placePiece(PieceFactory.king(Color.WHITE), sq("f7"));
        board.placePiece(PieceFactory.queen(Color.WHITE), sq("g5"));
        return gameWith(board, Color.WHITE);
    }

    private static Tournament tournament(Supplier<IGame> games, IMoveChooser opponent, Color userColor) {
        return new Tournament(HistoricalPlayerFactory.createRoster(), games::get, rival -> opponent, userColor);
    }

    @Test
    void winningARound_AdvancesToTheNextRival() {
        Tournament tournament = tournament(TournamentTest::userMatesInOne, new ScriptedOpponent(), Color.WHITE);
        tournament.start();
        HistoricalPlayer firstRival = tournament.getCurrentRival();

        TurnReport report = tournament.play(sq("a1"), sq("a8"));

        assertEquals(RoundOutcome.WON, report.outcome());
        assertNull(report.aiReply(), "si la ronda terminó, la IA no responde");
        assertEquals(2, tournament.getCurrentRound());
        assertNotEquals(firstRival, tournament.getCurrentRival());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
    }

    @Test
    void losingARound_EliminatesTheUser() {
        Tournament tournament = tournament(TournamentTest::userGetsMatedAfterB1A3,
                new ScriptedOpponent(new Move(sq("e8"), sq("e1"))), Color.WHITE);
        tournament.start();

        TurnReport report = tournament.play(sq("b1"), sq("a3"));

        assertEquals(RoundOutcome.LOST, report.outcome());
        assertEquals(new Move(sq("e8"), sq("e1")), report.aiReply());
        assertEquals(TournamentStatus.ELIMINATED, tournament.getStatus());
        assertEquals(1, tournament.getCurrentRound());
        assertEquals(MoveResult.GAME_OVER, tournament.play(sq("a3"), sq("b5")).userResult());
    }

    @Test
    void drawnRound_IsReplayedAgainstTheSameRivalWithANewGame() {
        int[] gamesCreated = {0};
        Tournament tournament = tournament(() -> { gamesCreated[0]++; return userStalematesWithG5G6(); },
                new ScriptedOpponent(), Color.WHITE);
        tournament.start();
        HistoricalPlayer rival = tournament.getCurrentRival();

        TurnReport report = tournament.play(sq("g5"), sq("g6"));

        assertEquals(RoundOutcome.DRAWN, report.outcome());
        assertEquals(1, tournament.getCurrentRound());
        assertEquals(rival, tournament.getCurrentRival());
        assertEquals(2, gamesCreated[0], "la ronda se repite con una partida nueva");
        assertNotNull(tournament.getBoard().pieceAt(sq("g5")), "el tablero vuelve a la posición inicial");
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
    }

    @Test
    void winningTheFinal_MakesTheUserChampion() {
        Tournament tournament = tournament(TournamentTest::userMatesInOne, new ScriptedOpponent(), Color.WHITE);
        tournament.start();

        for (int round = 1; round <= tournament.getTotalRounds(); round++) {
            assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
            tournament.play(sq("a1"), sq("a8"));
        }

        assertEquals(TournamentStatus.CHAMPION, tournament.getStatus());
        assertEquals(tournament.getTotalRounds(), tournament.getCurrentRound());
    }

    @Test
    void rivals_ComeInOrderOfDifficulty() {
        Tournament tournament = tournament(TournamentTest::userMatesInOne, new ScriptedOpponent(), Color.WHITE);
        tournament.start();

        HistoricalPlayer previous = tournament.getCurrentRival();
        while (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            tournament.play(sq("a1"), sq("a8"));
            HistoricalPlayer current = tournament.getCurrentRival();
            assertTrue(current.difficulty().compareTo(previous.difficulty()) >= 0);
            previous = current;
        }
    }

    @Test
    void validUserMove_IsAnsweredByTheAi() {
        ScriptedOpponent opponent = new ScriptedOpponent(new Move(sq("e7"), sq("e5")));
        Tournament tournament = tournament(() -> gameWith(new StandardSetup().createBoard(), Color.WHITE), opponent, Color.WHITE);
        tournament.start();

        TurnReport report = tournament.play(sq("e2"), sq("e4"));

        assertEquals(MoveResult.SUCCESS, report.userResult());
        assertEquals(new Move(sq("e7"), sq("e5")), report.aiReply());
        assertEquals(RoundOutcome.ONGOING, report.outcome());
        assertNotNull(tournament.getBoard().pieceAt(sq("e5")));
    }

    @Test
    void invalidUserMove_DoesNotTriggerTheAiReply() {
        ScriptedOpponent opponent = new ScriptedOpponent();
        Tournament tournament = tournament(() -> gameWith(new StandardSetup().createBoard(), Color.WHITE), opponent, Color.WHITE);
        tournament.start();

        TurnReport report = tournament.play(sq("e2"), sq("e5"));

        assertEquals(MoveResult.INVALID_PIECE_MOVE, report.userResult());
        assertNull(report.aiReply());
        assertEquals(0, opponent.movesPlayed);
    }

    @Test
    void userPlayingBlack_TheAiOpensEachRound() {
        Tournament tournament = tournament(() -> gameWith(new StandardSetup().createBoard(), Color.WHITE),
                new ScriptedOpponent(new Move(sq("e2"), sq("e4"))), Color.BLACK);

        tournament.start();

        assertNotNull(tournament.getBoard().pieceAt(sq("e4")));
        assertEquals(MoveResult.SUCCESS, tournament.play(sq("e7"), sq("e5")).userResult());
    }

    @Test
    void userMoveAlone_LeavesTheTournamentWaitingForTheAi_AndBlocksAnotherUserMove() {
        ScriptedOpponent opponent = new ScriptedOpponent(new Move(sq("e7"), sq("e5")));
        Tournament tournament = tournament(() -> gameWith(new StandardSetup().createBoard(), Color.WHITE), opponent, Color.WHITE);
        tournament.start();

        TurnReport userTurn = tournament.playUserMove(sq("e2"), sq("e4"));
        TurnReport secondTry = tournament.playUserMove(sq("d2"), sq("d4"));

        assertEquals(MoveResult.SUCCESS, userTurn.userResult());
        assertNull(userTurn.aiReply());
        assertTrue(tournament.isAwaitingOpponent());
        assertEquals(0, opponent.movesPlayed, "la IA todavía no jugó");
        assertEquals(MoveResult.OPPONENT_PIECE, secondTry.userResult(), "no se puede jugar dos veces seguidas");

        TurnReport aiTurn = tournament.playOpponentMove();

        assertEquals(new Move(sq("e7"), sq("e5")), aiTurn.aiReply());
        assertFalse(tournament.isAwaitingOpponent());
    }

    @Test
    void opponentMove_WhenItIsTheUsersTurn_IsRejected() {
        Tournament tournament = tournament(() -> gameWith(new StandardSetup().createBoard(), Color.WHITE),
                new ScriptedOpponent(), Color.WHITE);
        tournament.start();

        assertThrows(IllegalStateException.class, tournament::playOpponentMove);
    }

    @Test
    void userMoveThatEndsTheRound_DoesNotWaitForTheAi() {
        Tournament tournament = tournament(TournamentTest::userMatesInOne, new ScriptedOpponent(), Color.WHITE);
        tournament.start();

        TurnReport report = tournament.playUserMove(sq("a1"), sq("a8"));

        assertEquals(RoundOutcome.WON, report.outcome());
        assertFalse(tournament.isAwaitingOpponent(), "la ronda 2 empieza con el turno del usuario");
    }

    @Test
    void beforeStart_StatusIsNotStarted() {
        Tournament tournament = tournament(TournamentTest::userMatesInOne, new ScriptedOpponent(), Color.WHITE);

        assertEquals(TournamentStatus.NOT_STARTED, tournament.getStatus());
        assertThrows(IllegalStateException.class, tournament::getBoard);
    }
}
