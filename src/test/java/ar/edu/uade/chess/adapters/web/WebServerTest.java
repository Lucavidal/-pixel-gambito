package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.core.auth.AuthService;
import ar.edu.uade.chess.core.auth.IAuthService;
import ar.edu.uade.chess.core.auth.InMemoryUserRepository;
import ar.edu.uade.chess.core.auth.Pbkdf2PasswordHasher;
import ar.edu.uade.chess.core.ai.IMoveChooser;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.history.HistoryService;
import ar.edu.uade.chess.core.history.InMemoryMatchHistoryRepository;
import ar.edu.uade.chess.core.history.RoundRecord;
import ar.edu.uade.chess.core.tournament.RoundOutcome;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.StandardSetup;
import ar.edu.uade.chess.core.tournament.HistoricalPlayerFactory;
import ar.edu.uade.chess.core.tournament.Tournament;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.function.Supplier;

import static ar.edu.uade.chess.core.TestHelper.sq;
import static org.junit.jupiter.api.Assertions.*;

/** Levanta el servidor real en un puerto libre y lo usa como lo haría el front. */
class WebServerTest {

    private WebServer server;
    private IAuthService auth;
    private final InMemoryMatchHistoryRepository historyRepository = new InMemoryMatchHistoryRepository();
    private final HttpClient http = HttpClient.newHttpClient();

    /** Cookie de sesión del "navegador" del test (null = sin sesión). */
    private String cookie;

    @BeforeEach
    void start() {
        CheckDetector detector = new CheckDetector();
        MoveValidator validator = new MoveValidator(detector);
        auth = new AuthService(new InMemoryUserRepository(), new Pbkdf2PasswordHasher(1_000));
        auth.register("luca", "1234");
        Supplier<Game> games = () -> new Game(new StandardSetup().createBoard(), validator, detector, new CommandHistory());
        // Rival de prueba: siempre contesta e7 e5 (alcanza para probar el ida y vuelta por HTTP).
        IMoveChooser opponent = game -> new Move(sq("e7"), sq("e5"));
        WebSessions sessions = new WebSessions(user -> new UserSession(user,
                new WebGameSession(games::get, new AlgebraicNotation(), new PieceTypes()),
                new WebTournamentSession(level -> new Tournament(HistoricalPlayerFactory.createLadder(level, new Random(1)),
                        games::get, rival -> opponent, Color.WHITE), HistoricalPlayerFactory.allPlayers(),
                        new AlgebraicNotation(), new PieceTypes())));
        server = new WebServer(auth, sessions, new WebHistory(new HistoryService(historyRepository))).start(0);
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    // --- partida libre (con sesión) ---

    @Test
    void stateAndLegalMoves_AreServedAsJson() throws Exception {
        login("luca", "1234");
        HttpResponse<String> state = get("/api/game/state");
        HttpResponse<String> moves = get("/api/game/moves?from=e2");

        assertEquals(200, state.statusCode());
        assertTrue(state.body().contains("\"turn\":\"white\""));
        assertTrue(moves.body().contains("{\"to\":\"e4\",\"capture\":false}"));
    }

    @Test
    void move_ReturnsTheCoreResult() throws Exception {
        login("luca", "1234");
        HttpResponse<String> ok = post("/api/game/move", "{\"from\":\"e2\",\"to\":\"e4\"}");
        HttpResponse<String> rejected = post("/api/game/move", "{\"from\":\"e4\",\"to\":\"e5\"}");

        assertTrue(ok.body().contains("\"result\":\"SUCCESS\""));
        assertTrue(ok.body().contains("\"turn\":\"black\""));
        assertTrue(rejected.body().contains("\"result\":\"OPPONENT_PIECE\""));
    }

    @Test
    void undoAndReset_Work() throws Exception {
        login("luca", "1234");
        post("/api/game/move", "{\"from\":\"e2\",\"to\":\"e4\"}");
        assertTrue(post("/api/game/undo", "").body().contains("\"turn\":\"white\""));
        assertTrue(post("/api/game/reset", "").body().contains("\"success\":true"));
    }

    @Test
    void malformedSquare_Is400() throws Exception {
        login("luca", "1234");
        assertEquals(400, get("/api/game/moves?from=").statusCode());
        assertEquals(400, get("/api/game/moves?from=99").statusCode());
    }

    @Test
    void eachUser_PlaysOnTheirOwnBoard() throws Exception {
        auth.register("ana", "abcd");
        login("luca", "1234");
        post("/api/game/move", "{\"from\":\"e2\",\"to\":\"e4\"}");

        login("ana", "abcd");
        assertTrue(get("/api/game/state").body().contains("\"turn\":\"white\""));
    }

    // --- torneo ---

    @Test
    void tournament_TurnIsUserMoveThenOpponentMove() throws Exception {
        login("luca", "1234");
        assertTrue(get("/api/tournament/state").body().contains("\"status\":\"NOT_STARTED\""));
        assertTrue(post("/api/tournament/start", "").body().contains("\"status\":\"IN_PROGRESS\""));

        HttpResponse<String> user = post("/api/tournament/move", "{\"from\":\"e2\",\"to\":\"e4\"}");
        HttpResponse<String> ai = post("/api/tournament/opponent", "");

        assertTrue(user.body().contains("\"awaitingOpponent\":true"));
        assertEquals(200, ai.statusCode());
        assertTrue(ai.body().contains("\"from\":\"e7\",\"to\":\"e5\""));
        assertTrue(ai.body().contains("\"awaitingOpponent\":false"));
    }

    @Test
    void tournament_OpponentOutOfTurn_Is409() throws Exception {
        login("luca", "1234");
        assertEquals(409, post("/api/tournament/move", "{\"from\":\"e2\",\"to\":\"e4\"}").statusCode(),
                "sin empezar el torneo no se juega");

        post("/api/tournament/start", "");
        HttpResponse<String> response = post("/api/tournament/opponent", "");
        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("No es el turno de la IA"));
    }

    @Test
    void tournament_StartsFromTheChosenLevel() throws Exception {
        login("luca", "1234");

        HttpResponse<String> hard = post("/api/tournament/start", "{\"level\":\"HARD\"}");
        HttpResponse<String> again = post("/api/tournament/start", "");
        HttpResponse<String> unknown = post("/api/tournament/start", "{\"level\":\"IMPOSIBLE\"}");

        assertTrue(hard.body().contains("\"level\":\"HARD\""));
        assertTrue(hard.body().contains("\"totalRounds\":4"));
        assertTrue(again.body().contains("\"level\":\"HARD\""), "sin level se repite la última");
        assertEquals(400, unknown.statusCode());
    }

    @Test
    void tournament_WithoutSession_Is401() throws Exception {
        assertEquals(401, get("/api/tournament/state").statusCode());
    }

    // --- historial ---

    @Test
    void history_ShowsOnlyTheLoggedUsersRounds() throws Exception {
        historyRepository.save(new RoundRecord("luca", "Tigran Petrosian", 1, 3, RoundOutcome.WON, 12,
                LocalDateTime.of(2026, 10, 22, 10, 42)));
        auth.register("ana", "abcd");

        login("luca", "1234");
        HttpResponse<String> mine = get("/api/history");
        login("ana", "abcd");
        HttpResponse<String> hers = get("/api/history?limit=10");

        assertEquals(200, mine.statusCode());
        assertTrue(mine.body().contains("\"rival\":\"Tigran Petrosian\""));
        assertTrue(mine.body().contains("\"outcomeText\":\"Ganada\""));
        assertTrue(hers.body().contains("\"roundsPlayed\":0"));
    }

    @Test
    void history_BadLimit_Is400_AndWithoutSession_Is401() throws Exception {
        assertEquals(401, get("/api/history").statusCode());
        login("luca", "1234");
        assertEquals(400, get("/api/history?limit=muchas").statusCode());
        assertEquals(400, get("/api/history?limit=0").statusCode());
    }

    // --- sesión ---

    @Test
    void withoutSession_GameIs401() throws Exception {
        assertEquals(401, get("/api/game/state").statusCode());
        assertEquals(401, post("/api/game/move", "{\"from\":\"e2\",\"to\":\"e4\"}").statusCode());
        assertEquals(401, get("/api/auth/me").statusCode());

        cookie = WebServer.SESSION_COOKIE + "=token-inventado";
        assertEquals(401, get("/api/game/state").statusCode());
    }

    @Test
    void login_SetsAnHttpOnlyCookie_AndMeReturnsTheUser() throws Exception {
        HttpResponse<String> response = post("/api/auth/login", credentials("luca", "1234"));

        String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(response.body().contains("\"success\":true"));
        assertTrue(setCookie.startsWith(WebServer.SESSION_COOKIE + "="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));

        cookie = setCookie.split(";")[0];
        assertTrue(get("/api/auth/me").body().contains("\"username\":\"luca\""));
    }

    @Test
    void failedLogin_ExplainsWhy_AndOpensNoSession() throws Exception {
        HttpResponse<String> wrongPassword = post("/api/auth/login", credentials("luca", "mala"));
        HttpResponse<String> unknownUser = post("/api/auth/login", credentials("nadie", "1234"));

        assertEquals(200, wrongPassword.statusCode());
        assertTrue(wrongPassword.body().contains("\"result\":\"WRONG_PASSWORD\""));
        assertTrue(unknownUser.body().contains("\"result\":\"UNKNOWN_USER\""));
        assertTrue(wrongPassword.headers().firstValue("Set-Cookie").isEmpty());
    }

    @Test
    void register_CreatesTheAccountAndLogsIn() throws Exception {
        HttpResponse<String> response = post("/api/auth/register", credentials("nueva", "abcd"));
        cookie = response.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];

        assertTrue(response.body().contains("\"username\":\"nueva\""));
        assertTrue(auth.isRegistered("nueva"));
        assertEquals(200, get("/api/game/state").statusCode());
    }

    @Test
    void register_RejectedByTheCore_ReturnsItsMessage() throws Exception {
        HttpResponse<String> taken = post("/api/auth/register", credentials("luca", "otra1"));
        HttpResponse<String> shortPassword = post("/api/auth/register", credentials("otro", "12"));

        assertTrue(taken.body().contains("\"result\":\"USERNAME_TAKEN\""));
        assertTrue(shortPassword.body().contains("\"result\":\"PASSWORD_TOO_SHORT\""));
        assertTrue(taken.headers().firstValue("Set-Cookie").isEmpty());
    }

    @Test
    void logout_ClosesTheSession() throws Exception {
        login("luca", "1234");
        String oldCookie = cookie;

        HttpResponse<String> response = post("/api/auth/logout", "");

        assertTrue(response.headers().firstValue("Set-Cookie").orElseThrow().contains("Max-Age=0"));
        cookie = oldCookie; // aunque el navegador no la borre, el token ya no sirve
        assertEquals(401, get("/api/game/state").statusCode());
    }

    @Test
    void missingBody_Is400() throws Exception {
        assertEquals(400, post("/api/auth/login", "").statusCode());
        assertEquals(400, post("/api/auth/login", "no es json").statusCode());
    }

    @Test
    void emptyFields_AreJudgedByTheCore() throws Exception {
        HttpResponse<String> noPassword = post("/api/auth/login", "{\"username\":\"luca\"}");
        HttpResponse<String> emptyRegister = post("/api/auth/register", credentials("", ""));

        assertEquals(200, noPassword.statusCode());
        assertTrue(noPassword.body().contains("\"result\":\"WRONG_PASSWORD\""));
        assertTrue(emptyRegister.body().contains("\"result\":\"INVALID_USERNAME\""));
    }

    @Test
    void frontIsServedFromTheClasspath_WithoutSession() throws Exception {
        HttpResponse<String> page = get("/");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("<canvas") || page.body().contains("app"));
        assertEquals("no-cache", page.headers().firstValue("Cache-Control").orElse(""));
    }

    private void login(String username, String password) throws Exception {
        cookie = null;
        HttpResponse<String> response = post("/api/auth/login", credentials(username, password));
        cookie = response.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
    }

    private static String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(withCookie(HttpRequest.newBuilder(uri(path))).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(withCookie(HttpRequest.newBuilder(uri(path)))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder withCookie(HttpRequest.Builder builder) {
        return cookie == null ? builder : builder.header("Cookie", cookie);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.port() + path);
    }
}
