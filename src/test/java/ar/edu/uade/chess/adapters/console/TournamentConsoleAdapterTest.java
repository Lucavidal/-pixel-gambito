package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.ai.ComputerPlayer;
import ar.edu.uade.chess.core.ai.MoveAnalyzer;
import ar.edu.uade.chess.core.ai.PieceValue;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.history.HistoryService;
import ar.edu.uade.chess.core.history.InMemoryMatchHistoryRepository;
import ar.edu.uade.chess.core.history.RecordingTournament;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.model.Board;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.PieceFactory;
import ar.edu.uade.chess.core.setup.StandardSetup;
import ar.edu.uade.chess.core.tournament.BoardSetupGameFactory;
import ar.edu.uade.chess.core.tournament.HistoricalPlayerFactory;
import ar.edu.uade.chess.core.tournament.IGameFactory;
import ar.edu.uade.chess.core.tournament.Tournament;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de INTEGRACIÓN: adaptador de consola del torneo + núcleo real + IA real,
 * con la entrada simulada por texto y una semilla fija.
 */
class TournamentConsoleAdapterTest {

    private String play(IGameFactory games, String lines) throws Exception {
        return play(games, lines, 0);
    }

    private String play(IGameFactory games, String lines, int delayMillis) throws Exception {
        MoveAnalyzer analyzer = new MoveAnalyzer(new PieceValue());
        Random random = new Random(1);
        Tournament tournament = new Tournament(HistoricalPlayerFactory.createRoster(), games,
                rival -> new ComputerPlayer(rival.style(), rival.difficulty(), analyzer, random), Color.WHITE);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new TournamentConsoleAdapter(tournament, new BufferedReader(new StringReader(lines)), output,
                new AlgebraicNotation(), new TextRenderer(), delayMillis, null).play();

        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void showsRoundAndRival_AndTheAiAnswers() throws Exception {
        CheckDetector detector = new CheckDetector();
        IGameFactory standard = new BoardSetupGameFactory(new StandardSetup(), new MoveValidator(detector), detector);

        String output = play(standard, "e2 e4\nsalir\n");

        assertTrue(output.contains("Ronda 1 de 3"));
        assertTrue(output.contains("Rival: Tigran Petrosian | Estilo: Defensivo | Dificultad: Fácil"));
        assertTrue(output.contains("Tigran Petrosian juega "));
        assertTrue(output.contains("Abandonaste el torneo."));
    }

    @Test
    void withADelay_TheAiSaysItIsThinkingBeforeAnswering() throws Exception {
        CheckDetector detector = new CheckDetector();
        IGameFactory standard = new BoardSetupGameFactory(new StandardSetup(), new MoveValidator(detector), detector);

        String output = play(standard, "e2 e4\nsalir\n", 1);

        int thinking = output.indexOf("Tigran Petrosian está pensando...");
        int answer = output.indexOf("Tigran Petrosian juega ");
        assertTrue(thinking >= 0 && answer > thinking, "primero 'pensando', después la jugada");
    }

    @Test
    void historyCommand_ShowsTheRoundsSavedWhilePlaying() throws Exception {
        InMemoryMatchHistoryRepository repository = new InMemoryMatchHistoryRepository();
        User luca = new User("luca");
        CheckDetector detector = new CheckDetector();
        IGameFactory mateInOne = () -> {
            Board board = new Board(8, 8);
            board.placePiece(PieceFactory.king(Color.BLACK), new Position(7, 7));
            board.placePiece(PieceFactory.pawn(Color.BLACK), new Position(6, 6));
            board.placePiece(PieceFactory.pawn(Color.BLACK), new Position(6, 7));
            board.placePiece(PieceFactory.rook(Color.WHITE), new Position(0, 0));
            board.placePiece(PieceFactory.king(Color.WHITE), new Position(0, 6));
            return new Game(board, new MoveValidator(detector), detector, new CommandHistory());
        };
        Tournament real = new Tournament(HistoricalPlayerFactory.createRoster(), mateInOne, rival -> game -> null, Color.WHITE);
        RecordingTournament tournament = new RecordingTournament(real, repository, luca, Clock.systemDefaultZone());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        new TournamentConsoleAdapter(tournament, new BufferedReader(new StringReader("historial\na1 a8\nhistorial\nsalir\n")),
                new PrintStream(buffer, true, StandardCharsets.UTF_8), new AlgebraicNotation(), new TextRenderer(), 0,
                new HistoryPrinter(new HistoryService(repository), luca)).play();
        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("Todavía no jugaste ninguna ronda."), "antes de jugar, vacío");
        assertTrue(output.contains("Rondas: 1 | Ganadas: 1"));
        assertTrue(output.contains("vs Tigran Petrosian  GANADA  (1 jugadas)"));
    }

    @Test
    void winningEveryRound_AnnouncesTheChampion() throws Exception {
        // En cada ronda el usuario tiene un mate de pasillo en una jugada: a1 a8.
        IGameFactory mateInOne = () -> {
            Board board = new Board(8, 8);
            board.placePiece(PieceFactory.king(Color.BLACK), new Position(7, 7));
            board.placePiece(PieceFactory.pawn(Color.BLACK), new Position(6, 6));
            board.placePiece(PieceFactory.pawn(Color.BLACK), new Position(6, 7));
            board.placePiece(PieceFactory.rook(Color.WHITE), new Position(0, 0));
            board.placePiece(PieceFactory.king(Color.WHITE), new Position(0, 6));
            CheckDetector detector = new CheckDetector();
            return new Game(board, new MoveValidator(detector), detector, new CommandHistory());
        };

        String output = play(mateInOne, "a1 a8\na1 a8\na1 a8\n");

        assertTrue(output.contains("Le ganaste a Tigran Petrosian"));
        assertTrue(output.contains("Ronda 3 de 3"));
        assertTrue(output.contains("Torneo terminado: Campeón."));
    }
}
