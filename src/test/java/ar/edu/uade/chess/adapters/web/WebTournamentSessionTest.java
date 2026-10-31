package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.adapters.web.WebDtos.TournamentMoveResponse;
import ar.edu.uade.chess.adapters.web.WebDtos.TournamentStateDto;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.ai.IMoveChooser;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.history.InMemoryMatchHistoryRepository;
import ar.edu.uade.chess.core.history.RecordingTournament;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.PieceFactory;
import ar.edu.uade.chess.core.setup.StandardSetup;
import ar.edu.uade.chess.core.tournament.HistoricalPlayerFactory;
import ar.edu.uade.chess.core.tournament.ITournament;
import ar.edu.uade.chess.core.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import static ar.edu.uade.chess.core.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * El torneo tal como lo ve el navegador, sin HTTP. Las partidas son posiciones armadas
 * y el rival juega jugadas fijas, así cada test controla cómo termina la ronda.
 */
class WebTournamentSessionTest {

    /** Rival "guionado": juega las jugadas que se le dieron, en orden. */
    private static IMoveChooser scripted(Move... moves) {
        Deque<Move> script = new ArrayDeque<>(List.of(moves));
        return game -> script.poll();
    }

    /** Sesión cuyo torneo es siempre ese (para los tests que no prueban la elección de dificultad). */
    private static WebTournamentSession session(ITournament tournament) {
        return session(level -> tournament);
    }

    private static WebTournamentSession session(Function<Difficulty, ITournament> tournaments) {
        return new WebTournamentSession(tournaments, HistoricalPlayerFactory.allPlayers(),
                new AlgebraicNotation(), new PieceTypes());
    }

    /** Torneo con la escalera real de la web (sorteo fijo), partidas estándar y un rival que no llega a jugar. */
    private static ITournament ladderTournament(Difficulty level) {
        return new Tournament(HistoricalPlayerFactory.createLadder(level, new Random(3)),
                WebTournamentSessionTest::standardGame, rival -> scripted(), Color.WHITE);
    }

    private static ITournament tournament(Supplier<IGame> games, IMoveChooser opponent) {
        return new Tournament(HistoricalPlayerFactory.createRoster(), games::get, rival -> opponent, Color.WHITE);
    }

    private static Game standardGame() {
        CheckDetector detector = new CheckDetector();
        return new Game(new StandardSetup().createBoard(), new MoveValidator(detector), detector, new CommandHistory());
    }

    /** Mate de pasillo: el usuario (blancas) gana con a1 a8. */
    private static Game userMatesInOne() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.BLACK), sq("h8"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("g7"));
        board.placePiece(PieceFactory.pawn(Color.BLACK), sq("h7"));
        board.placePiece(PieceFactory.rook(Color.WHITE), sq("a1"));
        board.placePiece(PieceFactory.king(Color.WHITE), sq("g1"));
        return gameWith(board, Color.WHITE);
    }

    /** Si el usuario juega b1 a3, el rival se come el alfil de e1 y da mate. */
    private static Game userGetsMatedAfterB1A3() {
        Board board = emptyBoard();
        board.placePiece(PieceFactory.king(Color.WHITE), sq("h1"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("g2"));
        board.placePiece(PieceFactory.pawn(Color.WHITE), sq("h2"));
        board.placePiece(PieceFactory.knight(Color.WHITE), sq("b1"));
        board.placePiece(PieceFactory.bishop(Color.WHITE), sq("e1"));
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

    @Test
    void beforeChoosingTheLevel_ThereIsNoBoardNorLadder_ButTheLevelsAndRosterAreListed() {
        WebTournamentSession session = session(WebTournamentSessionTest::ladderTournament);

        TournamentStateDto state = session.state();

        assertEquals("NOT_STARTED", state.status());
        assertNull(state.board());
        assertNull(state.level());
        assertTrue(state.rivals().isEmpty());
        assertEquals(List.of("EASY", "MEDIUM", "HARD"), state.levels().stream().map(WebDtos.LevelDto::id).toList());
        assertEquals(12, state.allRivals().size());
        assertThrows(IllegalStateException.class, () -> session.move("e2", "e4"));
        assertThrows(IllegalStateException.class, session::opponentMove);
    }

    @Test
    void levels_DescribeTheirLadder() {
        List<WebDtos.LevelDto> levels = session(WebTournamentSessionTest::ladderTournament).state().levels();

        assertEquals("4 rondas: 2 de dificultad fácil y 2 de dificultad media.", levels.get(0).description());
        assertEquals("4 rondas: 2 de dificultad media y 2 de dificultad difícil.", levels.get(1).description());
        assertEquals("4 rondas: 4 de dificultad difícil.", levels.get(2).description());
        assertEquals(4, levels.get(2).rounds());
        assertEquals(List.of(new WebDtos.LevelPartDto("EASY", 2), new WebDtos.LevelPartDto("MEDIUM", 2)),
                levels.get(0).parts(), "lo mismo en datos, para que el front lo escriba en su idioma");
    }

    @Test
    void start_WithALevel_PlaysTheLadderThatStartsThere() {
        WebTournamentSession session = session(WebTournamentSessionTest::ladderTournament);

        TournamentStateDto state = session.start(Difficulty.MEDIUM);

        assertEquals("IN_PROGRESS", state.status());
        assertEquals("MEDIUM", state.level());
        assertEquals("Media", state.levelName());
        assertEquals(4, state.totalRounds());
        assertEquals(List.of("Media", "Media", "Difícil", "Difícil"),
                state.rivals().stream().map(WebDtos.RivalDto::difficulty).toList());
        assertEquals("playing", state.rivals().get(0).standing());
        assertEquals(List.of("MEDIUM", "MEDIUM", "HARD", "HARD"),
                state.rivals().stream().map(WebDtos.RivalDto::difficultyId).toList());
        assertTrue(state.rivals().stream().allMatch(r -> List.of("AGGRESSIVE", "BALANCED", "DEFENSIVE").contains(r.styleId())),
                "cada rival trae el código de su estilo");
    }

    @Test
    void start_WithoutALevel_RepeatsTheLastOne_AndIsANewTournament() {
        List<Difficulty> asked = new ArrayList<>();
        WebTournamentSession session = session(level -> { asked.add(level); return ladderTournament(level); });
        asked.clear(); // describir los niveles también arma torneos (sin empezarlos)

        session.start(Difficulty.HARD);
        session.move("e2", "e4");
        TournamentStateDto restarted = session.start(null);

        assertEquals(List.of(Difficulty.HARD, Difficulty.HARD), asked, "cada start arma un torneo nuevo");
        assertEquals("HARD", restarted.level());
        assertTrue(restarted.board().history().isEmpty());
    }

    @Test
    void start_TheFirstTimeWithoutALevel_IsTheEasiest() {
        assertEquals("EASY", session(WebTournamentSessionTest::ladderTournament).start(null).level());
    }

    @Test
    void aTurnIsTwoSteps_UserMoveThenOpponentMove() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::standardGame,
                scripted(new Move(sq("e7"), sq("e5")))));
        session.start(Difficulty.EASY);

        TournamentMoveResponse user = session.move("e2", "e4");
        assertTrue(user.success());
        assertTrue(user.state().awaitingOpponent(), "la IA todavía no respondió");
        assertEquals("black", user.state().board().turn());
        assertNull(user.roundResult());

        TournamentMoveResponse ai = session.opponentMove();
        assertEquals("e7", ai.move().from());
        assertEquals("e5", ai.move().to());
        assertEquals("black", ai.move().color());
        assertFalse(ai.state().awaitingOpponent());
        assertEquals(2, ai.state().board().history().size());
        assertFalse(ai.state().board().canUndo(), "en el torneo no se deshace");
    }

    @Test
    void opponentMove_WhenItIsNotItsTurn_IsRejected() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::standardGame, scripted()));
        session.start(Difficulty.EASY);

        assertThrows(IllegalStateException.class, session::opponentMove);
    }

    @Test
    void invalidUserMove_ReturnsTheCoreReason_AndTheAiDoesNotPlay() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::standardGame, scripted()));
        session.start(Difficulty.EASY);

        TournamentMoveResponse response = session.move("e2", "e5");

        assertFalse(response.success());
        assertNotNull(response.message());
        assertNull(response.move());
        assertFalse(response.state().awaitingOpponent());
    }

    @Test
    void legalMoves_OnlyForTheUsersPieces() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::standardGame, scripted()));
        session.start(Difficulty.EASY);

        assertFalse(session.legalMoves("e2").moves().isEmpty());
        assertTrue(session.legalMoves("e7").moves().isEmpty(), "las piezas de la IA no se eligen");
    }

    @Test
    void winningARound_ReportsIt_AndTheNewRoundStartsWithAnEmptySheet() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::userMatesInOne, scripted()));
        String firstRival = session.start(Difficulty.EASY).rivals().get(0).name();

        TournamentMoveResponse response = session.move("a1", "a8");

        assertEquals("WON", response.roundResult().outcome());
        assertEquals(firstRival, response.roundResult().rival());
        assertTrue(response.roundResult().message().contains("Pasás a la ronda 2"));
        assertTrue(response.move().text().endsWith("#"));
        assertEquals(2, response.state().round());
        assertTrue(response.state().board().history().isEmpty(), "la planilla de la ronda nueva arranca vacía");
        assertEquals(List.of("won", "playing", "pending"),
                response.state().rivals().stream().map(WebDtos.RivalDto::standing).toList());
    }

    @Test
    void winningEveryRound_MakesTheUserChampion() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::userMatesInOne, scripted()));
        session.start(Difficulty.EASY);
        session.move("a1", "a8");
        session.move("a1", "a8");

        TournamentMoveResponse last = session.move("a1", "a8");

        assertEquals("CHAMPION", last.state().status());
        assertTrue(last.roundResult().message().contains("campeón"));
        assertTrue(last.state().board().gameOver());
        assertTrue(last.state().rivals().stream().allMatch(r -> r.standing().equals("won")));
    }

    @Test
    void losingARound_EliminatesTheUser_AndTheAiCaptureIsLogged() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::userGetsMatedAfterB1A3,
                scripted(new Move(sq("e8"), sq("e1")))));
        session.start(Difficulty.EASY);
        session.move("b1", "a3");

        TournamentMoveResponse ai = session.opponentMove();

        assertEquals("LOST", ai.roundResult().outcome());
        assertTrue(ai.move().text().contains("xe1"), "la IA se comió el alfil: " + ai.move().text());
        assertTrue(ai.move().text().endsWith("#"));
        assertEquals(List.of("bishop"), ai.state().board().capturedWhite());
        assertEquals("ELIMINATED", ai.state().status());
        assertEquals("h1", ai.state().board().checkedKing(), "el rey del usuario queda marcado en mate");
        assertEquals("lost", ai.state().rivals().get(0).standing());
    }

    @Test
    void stalemate_RepeatsTheRound() {
        WebTournamentSession session = session(tournament(WebTournamentSessionTest::userStalematesWithG5G6, scripted()));
        session.start(Difficulty.EASY);

        TournamentMoveResponse response = session.move("g5", "g6");

        assertEquals("DRAWN", response.roundResult().outcome());
        assertTrue(response.roundResult().message().contains("Se repite la ronda 1"));
        assertEquals(1, response.state().round());
        assertEquals("IN_PROGRESS", response.state().status());
    }

    @Test
    void finishedRounds_AreSavedInTheUsersHistory() {
        InMemoryMatchHistoryRepository history = new InMemoryMatchHistoryRepository();
        ITournament recorded = new RecordingTournament(
                tournament(WebTournamentSessionTest::userMatesInOne, scripted()), history, new User("luca"),
                Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC));
        WebTournamentSession session = session(recorded);
        session.start(Difficulty.EASY);

        session.move("a1", "a8");

        assertEquals(1, history.findByUsername("luca").size());
    }
}
