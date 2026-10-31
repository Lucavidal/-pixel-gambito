package ar.edu.uade.chess.core.history;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.ai.IMoveChooser;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.setup.PieceFactory;
import ar.edu.uade.chess.core.setup.StandardSetup;
import ar.edu.uade.chess.core.tournament.HistoricalPlayerFactory;
import ar.edu.uade.chess.core.tournament.ITournament;
import ar.edu.uade.chess.core.tournament.RoundOutcome;
import ar.edu.uade.chess.core.tournament.Tournament;
import ar.edu.uade.chess.core.tournament.TournamentStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/** El Decorator que guarda el historial: el torneo se comporta igual y cada ronda terminada queda guardada. */
class RecordingTournamentTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC);
    private static final User LUCA = new User("luca");

    private final InMemoryMatchHistoryRepository history = new InMemoryMatchHistoryRepository();

    private ITournament recording(Supplier<IGame> games, IMoveChooser opponent) {
        Tournament real = new Tournament(HistoricalPlayerFactory.createRoster(), games::get, rival -> opponent, Color.WHITE);
        return new RecordingTournament(real, history, LUCA, FIXED);
    }

    /** Mate de pasillo: el usuario gana con a1 a8. */
    private static IGame userMatesInOne() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("g7"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("h7"));
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));
        board.placePiece(PieceFactory.king(Color.WHITE), sq("g1"));
        return gameWith(board, Color.WHITE);
    }

    /** Si el usuario juega b1 a3, la IA da mate con e8 e1. */
    private static IGame userGetsMated() {
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
    private static IGame userStalemates() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        board.placePiece(PieceFactory.king(Color.WHITE), sq("f7"));
        board.placePiece(PieceFactory.queen(Color.WHITE), sq("g5"));
        return gameWith(board, Color.WHITE);
    }

    @Test
    void wonRound_IsSavedWithRivalRoundAndDate() {
        ITournament tournament = recording(RecordingTournamentTest::userMatesInOne, game -> null);
        tournament.start();
        String firstRival = tournament.getCurrentRival().name();

        tournament.play(sq("a1"), sq("a8"));

        RoundRecord saved = history.all().get(0);
        assertEquals(1, history.all().size());
        assertEquals("luca", saved.username());
        assertEquals(firstRival, saved.rivalName(), "se guarda el rival vencido, no el siguiente");
        assertEquals(1, saved.round());
        assertEquals(3, saved.totalRounds());
        assertEquals(RoundOutcome.WON, saved.outcome());
        assertEquals(1, saved.userMoves());
        assertEquals(LocalDateTime.of(2026, 10, 22, 12, 0), saved.playedAt());
    }

    @Test
    void lostRound_IsSavedAsLost_UsingTheTwoStepTurn() {
        ITournament tournament = recording(RecordingTournamentTest::userGetsMated, game -> new Move(sq("e8"), sq("e1")));
        tournament.start();

        tournament.playUserMove(sq("b1"), sq("a3"));
        tournament.playOpponentMove();

        assertEquals(TournamentStatus.ELIMINATED, tournament.getStatus());
        assertEquals(1, history.all().size());
        assertEquals(RoundOutcome.LOST, history.all().get(0).outcome());
    }

    @Test
    void drawnRound_IsSavedAsDrawn() {
        ITournament tournament = recording(RecordingTournamentTest::userStalemates, game -> null);
        tournament.start();

        tournament.play(sq("g5"), sq("g6"));

        assertEquals(RoundOutcome.DRAWN, history.all().get(0).outcome());
    }

    @Test
    void roundsInProgressAndInvalidMoves_AreNotSaved() {
        ITournament tournament = recording(() -> gameWith(new StandardSetup().createBoard(), Color.WHITE),
                game -> new Move(sq("e7"), sq("e5")));
        tournament.start();

        assertEquals(MoveResult.INVALID_PIECE_MOVE, tournament.play(sq("e2"), sq("e5")).userResult());
        tournament.play(sq("e2"), sq("e4"));

        assertTrue(history.all().isEmpty());
    }

    @Test
    void winningTheWholeTournament_LeavesOneRecordPerRound_AndOneTitle() {
        ITournament tournament = recording(RecordingTournamentTest::userMatesInOne, game -> null);
        tournament.start();

        for (int round = 1; round <= 3; round++) {
            tournament.play(sq("a1"), sq("a8"));
        }

        assertEquals(TournamentStatus.CHAMPION, tournament.getStatus());
        assertEquals(3, history.all().size());
        assertEquals(new HistorySummary(3, 3, 0, 0, 1), new HistoryService(history).summary(LUCA));
    }

    @Test
    void roundRecord_KeepsTheDifficulty_AndWithoutOneItIsNull() {
        LocalDateTime when = LocalDateTime.of(2026, 10, 22, 12, 0);

        RoundRecord chosen = new RoundRecord("luca", "Tal", 1, 4, RoundOutcome.WON, 9, when, Difficulty.HARD);
        RoundRecord old = new RoundRecord("luca", "Tal", 1, 4, RoundOutcome.WON, 9, when);

        assertEquals(Difficulty.HARD, chosen.difficulty());
        assertNull(old.difficulty(), "una ronda sin dificultad elegida (o guardada antes) queda en null");
    }

    @Test
    void recordingTournament_SavesTheDifficultyTheUserChose() {
        Tournament real = new Tournament(HistoricalPlayerFactory.createRoster(),
                RecordingTournamentTest::userMatesInOne, rival -> game -> null, Color.WHITE);
        ITournament tournament = new RecordingTournament(real, history, LUCA, FIXED, Difficulty.MEDIUM);
        tournament.start();

        tournament.play(sq("a1"), sq("a8"));   // gana la ronda 1
        tournament.play(sq("a1"), sq("a8"));   // y la 2

        assertEquals(2, history.all().size());
        assertTrue(history.all().stream().allMatch(r -> r.difficulty() == Difficulty.MEDIUM),
                "todas las rondas del torneo llevan la dificultad elegida, no la de cada rival");
    }

    @Test
    void recordingTournament_WithoutAChoice_SavesNoDifficulty() {
        ITournament tournament = recording(RecordingTournamentTest::userMatesInOne, game -> null);
        tournament.start();

        tournament.play(sq("a1"), sq("a8"));

        assertNull(history.all().get(0).difficulty(), "la escalera clásica no pide dificultad");
    }
}
