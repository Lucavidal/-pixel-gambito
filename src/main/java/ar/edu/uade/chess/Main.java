package ar.edu.uade.chess;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.adapters.console.ConsoleAdapter;
import ar.edu.uade.chess.adapters.console.ConsoleLogin;
import ar.edu.uade.chess.adapters.console.HistoryPrinter;
import ar.edu.uade.chess.adapters.console.TextRenderer;
import ar.edu.uade.chess.adapters.console.TournamentConsoleAdapter;
import ar.edu.uade.chess.adapters.gui.GuiApplication;
import ar.edu.uade.chess.adapters.persistence.JdbcMatchHistoryRepository;
import ar.edu.uade.chess.adapters.persistence.JdbcUserRepository;
import ar.edu.uade.chess.adapters.persistence.SqliteDatabase;
import ar.edu.uade.chess.adapters.web.PieceTypes;
import ar.edu.uade.chess.adapters.web.UserSession;
import ar.edu.uade.chess.adapters.web.WebGameSession;
import ar.edu.uade.chess.adapters.web.WebHistory;
import ar.edu.uade.chess.adapters.web.WebServer;
import ar.edu.uade.chess.adapters.web.WebSessions;
import ar.edu.uade.chess.adapters.web.WebTournamentSession;
import ar.edu.uade.chess.core.ai.ComputerPlayer;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.AuthService;
import ar.edu.uade.chess.core.auth.IAuthService;
import ar.edu.uade.chess.core.auth.Pbkdf2PasswordHasher;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.history.HistoryService;
import ar.edu.uade.chess.core.history.IHistoryService;
import ar.edu.uade.chess.core.history.IMatchHistoryRepository;
import ar.edu.uade.chess.core.history.RecordingTournament;
import ar.edu.uade.chess.core.ai.MoveAnalyzer;
import ar.edu.uade.chess.core.ai.PieceValue;
import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.IBoardSetup;
import ar.edu.uade.chess.core.setup.StandardSetup;
import ar.edu.uade.chess.core.tournament.BoardSetupGameFactory;
import ar.edu.uade.chess.core.tournament.HistoricalPlayer;
import ar.edu.uade.chess.core.tournament.HistoricalPlayerFactory;
import ar.edu.uade.chess.core.tournament.ITournament;
import ar.edu.uade.chess.core.tournament.Tournament;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.Random;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * El lugar donde se inyectan las dependencias: el único lugar del programa
 * donde se eligen las implementaciones concretas y se conectan entre sí.
 *
 * Cambiar la consola por una interfaz gráfica, o el tablero estándar por
 * otro, se hace acá; el núcleo no se entera.
 *
 * Modos: sin argumentos arranca la versión web (se abre el navegador). Con un argumento:
 * "consola" (partida libre), "torneo" (torneo en consola), "gui" / "gui-torneo" (ventana
 * Swing), "web", o "menu" para elegir el modo al arrancar. Todos usan el mismo núcleo:
 * solo cambia el adaptador.
 *
 * Antes de jugar se inicia sesión (en consola o en ventana, según el modo).
 * Las cuentas y el historial del torneo se guardan en SQLite (archivo DATABASE_FILE).
 */
public final class Main {

    /** El usuario juega con blancas en el torneo (decisión por defecto, ver docs/CAMBIOS_TORNEO.md). */
    private static final Color TOURNAMENT_USER_COLOR = Color.WHITE;

    /** Cuánto "piensa" la IA antes de mostrar su jugada, en milisegundos (0 = responde en el acto). */
    private static final int AI_THINKING_DELAY_MILLIS = 900;

    /** Archivo de la base SQLite (usuarios e historial). Se crea en la carpeta desde donde se ejecuta. */
    private static final String DATABASE_FILE = "ajedrez.db";

    /** Sorteo de los rivales de cada escalera de la web (uno solo para todo el programa). */
    private static final Random LADDER_DRAW = new Random();

    /** Puerto del servidor web del tablero 3D (modo "web"). */
    private static final int WEB_PORT = Integer.getInteger("chess.port", 7070); // otro puerto: -Dchess.port=7071

    public static void main(String[] args) throws IOException {
        IBoardSetup setup = new StandardSetup();
        CheckDetector detector = new CheckDetector();
        MoveValidator validator = new MoveValidator(detector);

        // Persistencia: adaptadores de salida (SQLite) detrás de puertos del núcleo.
        SqliteDatabase database = new SqliteDatabase(Path.of(DATABASE_FILE));
        database.createSchema();
        IAuthService auth = new AuthService(new JdbcUserRepository(database), new Pbkdf2PasswordHasher());
        IMatchHistoryRepository historyRepository = new JdbcMatchHistoryRepository(database);
        IHistoryService history = new HistoryService(historyRepository);

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintStream output = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

        String mode = chooseMode(args, input, output);
        if (mode.equals("5")) {
            startWeb(auth, history, () -> new Game(setup.createBoard(), validator, detector, new CommandHistory()),
                    (user, level) -> recordedTournament(setup, validator, detector, historyRepository, user,
                            HistoricalPlayerFactory.createLadder(level, LADDER_DRAW), level), output);
            return;
        }
        switch (mode) {
            case "3", "4" -> new GuiApplication(auth, history,
                    () -> new Game(setup.createBoard(), validator, detector, new CommandHistory()),
                    user -> recordedTournament(setup, validator, detector, historyRepository, user),
                    HistoricalPlayerFactory.createRoster(),
                    AI_THINKING_DELAY_MILLIS).start(mode.equals("4"));
            default -> {
                Optional<User> user = new ConsoleLogin(auth, input, output).run();
                if (user.isEmpty()) {
                    output.println("Hasta luego.");
                    return;
                }
                if (mode.equals("2")) {
                    ITournament tournament = recordedTournament(setup, validator, detector, historyRepository, user.get());
                    new TournamentConsoleAdapter(tournament, input, output, new AlgebraicNotation(), new TextRenderer(),
                            AI_THINKING_DELAY_MILLIS, new HistoryPrinter(history, user.get())).play();
                } else {
                    IGame game = new Game(setup.createBoard(), validator, detector, new CommandHistory());
                    new ConsoleAdapter(game, input, output, new AlgebraicNotation(), new TextRenderer()).play();
                }
            }
        }
    }

    /**
     * El torneo clásico de 3 rondas (consola y ventana), envuelto para que se guarde en el historial.
     * Ahí no se elige dificultad (la escalera es siempre fácil, media, difícil): se guarda sin ella.
     */
    private static ITournament recordedTournament(IBoardSetup setup, MoveValidator validator, CheckDetector detector,
                                                  IMatchHistoryRepository historyRepository, User user) {
        return recordedTournament(setup, validator, detector, historyRepository, user,
                HistoricalPlayerFactory.createRoster(), null);
    }

    /**
     * El torneo real con esos rivales, envuelto (Decorator) para que cada ronda terminada se guarde en el historial
     * con la dificultad que eligió el usuario (null si no eligió).
     */
    private static ITournament recordedTournament(IBoardSetup setup, MoveValidator validator, CheckDetector detector,
                                                  IMatchHistoryRepository historyRepository, User user,
                                                  List<HistoricalPlayer> rivals, Difficulty chosenDifficulty) {
        MoveAnalyzer analyzer = new MoveAnalyzer(new PieceValue());
        Random random = new Random();
        ITournament tournament = new Tournament(
                rivals,
                new BoardSetupGameFactory(setup, validator, detector),
                rival -> new ComputerPlayer(rival.style(), rival.difficulty(), analyzer, random),
                TOURNAMENT_USER_COLOR);
        return new RecordingTournament(tournament, historyRepository, user, Clock.systemDefaultZone(), chosenDifficulty);
    }

    /**
     * Tablero 3D: el adaptador web expone la partida por HTTP y sirve el front (Three.js).
     * Cada usuario que inicia sesión en el navegador recibe su propia partida y su propio torneo
     * (que guarda sus rondas en el historial). En la web el usuario elige desde qué dificultad
     * empieza la escalera: cada torneo nuevo sortea sus rivales (HistoricalPlayerFactory.createLadder).
     * El servidor queda corriendo hasta cerrar el programa (Ctrl+C).
     */
    private static void startWeb(IAuthService auth, IHistoryService history, Supplier<IGame> games,
                                 BiFunction<User, Difficulty, ITournament> tournaments, PrintStream output) {
        WebSessions sessions = new WebSessions(user -> new UserSession(user,
                new WebGameSession(games, new AlgebraicNotation(), new PieceTypes()),
                new WebTournamentSession(level -> tournaments.apply(user, level), HistoricalPlayerFactory.allPlayers(),
                        new AlgebraicNotation(), new PieceTypes())));
        new WebServer(auth, sessions, new WebHistory(history)).start(WEB_PORT);
        String url = "http://localhost:" + WEB_PORT;
        output.println("Tablero 3D listo en " + url + "  (Ctrl+C para cerrar el servidor)");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // si no se puede abrir el navegador solo, el usuario abre la dirección a mano
        }
    }

    /** "1" consola libre, "2" consola torneo, "3" ventana libre, "4" ventana torneo, "5" tablero 3D (navegador). */
    private static String chooseMode(String[] args, BufferedReader input, PrintStream output) throws IOException {
        // Sin argumentos se abre la versión web (la interfaz principal del TPO)
        if (args.length == 0) {
            return "5";
        }
        String mode = switch (args[0].toLowerCase()) {
            case "consola" -> "1";
            case "torneo" -> "2";
            case "gui" -> "3";
            case "gui-torneo" -> "4";
            case "web" -> "5";
            case "menu" -> null;
            default -> "1";
        };
        if (mode != null) {
            return mode;
        }
        output.println("Elegí el modo:");
        output.println("  1 = partida libre (consola)     2 = torneo contra la IA (consola)");
        output.println("  3 = interfaz gráfica (menú)     4 = interfaz gráfica, directo al torneo");
        output.println("  5 = tablero 3D en el navegador");
        output.print("> ");
        String line = input.readLine();
        return line == null ? "1" : line.trim();
    }
}
