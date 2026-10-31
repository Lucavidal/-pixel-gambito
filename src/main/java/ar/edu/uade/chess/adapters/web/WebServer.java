package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.web.WebDtos.AuthResponse;
import ar.edu.uade.chess.adapters.web.WebDtos.CredentialsRequest;
import ar.edu.uade.chess.adapters.web.WebDtos.ErrorDto;
import ar.edu.uade.chess.adapters.web.WebDtos.MoveRequest;
import ar.edu.uade.chess.adapters.web.WebDtos.StartRequest;
import ar.edu.uade.chess.adapters.web.WebDtos.UserDto;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.AuthResult;
import ar.edu.uade.chess.core.auth.IAuthService;
import ar.edu.uade.chess.core.auth.User;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ADAPTADOR WEB: expone el juego por HTTP/JSON con Javalin y sirve el front 3D
 * (src/main/resources/web). Es el único lugar del programa que conoce Javalin.
 *
 * Endpoints (todas las casillas en notación "e2"):
 *   POST /api/auth/register {username,password,displayName?} -> crea la cuenta y abre la sesión
 *   POST /api/auth/login    {username,password} -> abre la sesión (cookie HttpOnly)
 *   POST /api/auth/logout                       -> cierra la sesión
 *   GET  /api/auth/me                           -> el usuario logueado ({username, displayName}), o 401
 *
 *   Con sesión iniciada (si no, 401), la partida libre de ESE usuario:
 *   GET  /api/game/state           -> foto de la partida (StateDto)
 *   GET  /api/game/moves?from=e2   -> destinos legales de esa pieza
 *   POST /api/game/move  {from,to} -> intenta la jugada; responde el MoveResult y el estado nuevo
 *   POST /api/game/undo            -> deshace (MoveCommand del núcleo)
 *   POST /api/game/redo            -> rehace
 *   POST /api/game/reset           -> partida nueva
 *
 *   El torneo escalera de ESE usuario (cada turno son dos pedidos: move y después opponent):
 *   GET  /api/tournament/state          -> escalera, ronda, estado y tablero (TournamentStateDto)
 *   POST /api/tournament/start {level}  -> torneo nuevo en la ronda 1, con la escalera desde esa
 *                                          dificultad (EASY / MEDIUM / HARD; sin level, la última)
 *   GET  /api/tournament/moves?from=e2  -> destinos legales de una pieza del usuario
 *   POST /api/tournament/move {from,to} -> SOLO la jugada del usuario
 *   POST /api/tournament/opponent       -> la respuesta de la IA (409 si no le toca)
 *
 *   GET  /api/history?limit=50&difficulty=EASY -> "Mi historial": totales y últimas rondas de ESE usuario
 *                                                 (difficulty opcional: filtra las rondas y los totales)
 */
public class WebServer {

    /** Nombre de la cookie que guarda el token de la sesión. */
    static final String SESSION_COOKIE = "chess_session";

    /**
     * Atributos de la cookie de sesión. Detrás de HTTPS se agrega Secure (-Dchess.secureCookie=true):
     * así el navegador nunca la manda sin cifrar. Con HTTP común (localhost, IP:puerto) queda sin Secure.
     */
    private static final String COOKIE_FLAGS = "; Path=/; HttpOnly; SameSite=Strict"
            + (Boolean.getBoolean("chess.secureCookie") ? "; Secure" : "");

    /** Clave con la que el filtro deja la UserSession en el pedido, para los handlers. */
    private static final String SESSION_ATTRIBUTE = "userSession";

    private final IAuthService auth;
    private final WebSessions sessions;
    private final WebHistory history;
    private Javalin app;

    public WebServer(IAuthService auth, WebSessions sessions, WebHistory history) {
        this.auth = auth;
        this.sessions = sessions;
        this.history = history;
    }

    /** Arma la aplicación sin arrancarla (los tests la arrancan en un puerto libre). */
    public Javalin create() {
        Javalin javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add(files -> {
                files.hostedPath = "/";
                files.directory = "/web";
                files.location = Location.CLASSPATH;
                // Que el navegador pregunte siempre si cambió el front: si no, después de
                // recompilar puede seguir usando los .js viejos hasta forzar la recarga.
                files.headers = Map.of("Cache-Control", "no-cache");
            });
        });

        javalin.post("/api/auth/register", this::register);
        javalin.post("/api/auth/login", this::login);
        javalin.post("/api/auth/logout", this::logout);
        javalin.get("/api/auth/me", ctx -> {
            User user = currentSession(ctx).user();
            ctx.json(new UserDto(user.username(), user.visibleName()));
        });

        // Todo lo que no es /api/auth necesita una sesión: el filtro la busca una vez y la deja en el pedido.
        javalin.before("/api/*", ctx -> {
            if (!ctx.path().startsWith("/api/auth/")) {
                ctx.attribute(SESSION_ATTRIBUTE, currentSession(ctx));
            }
        });

        javalin.get("/api/game/state", ctx -> ctx.json(game(ctx).state()));
        javalin.get("/api/game/moves", ctx -> ctx.json(game(ctx).legalMoves(required(ctx.queryParam("from"), "from"))));
        javalin.post("/api/game/move", ctx -> {
            MoveRequest request = readBody(ctx, MoveRequest.class, "{\"from\":\"e2\",\"to\":\"e4\"}");
            ctx.json(game(ctx).move(required(request.from(), "from"), required(request.to(), "to")));
        });
        javalin.post("/api/game/undo", ctx -> ctx.json(game(ctx).undo()));
        javalin.post("/api/game/redo", ctx -> ctx.json(game(ctx).redo()));
        javalin.post("/api/game/reset", ctx -> ctx.json(game(ctx).reset()));

        javalin.get("/api/tournament/state", ctx -> ctx.json(tournament(ctx).state()));
        javalin.post("/api/tournament/start", ctx -> ctx.json(tournament(ctx).start(readLevel(ctx))));
        javalin.get("/api/tournament/moves",
                ctx -> ctx.json(tournament(ctx).legalMoves(required(ctx.queryParam("from"), "from"))));
        javalin.post("/api/tournament/move", ctx -> {
            MoveRequest request = readBody(ctx, MoveRequest.class, "{\"from\":\"e2\",\"to\":\"e4\"}");
            ctx.json(tournament(ctx).move(required(request.from(), "from"), required(request.to(), "to")));
        });
        javalin.post("/api/tournament/opponent", ctx -> ctx.json(tournament(ctx).opponentMove()));

        javalin.get("/api/history", ctx -> {
            UserSession session = ctx.attribute(SESSION_ATTRIBUTE);
            ctx.json(history.of(session.user(), limit(ctx.queryParam("limit")),
                    WebHistory.parseDifficulty(ctx.queryParam("difficulty"))));
        });

        // Una casilla mal escrita ("z9", "") no es un error del servidor: es un pedido inválido.
        javalin.exception(IllegalArgumentException.class, (error, ctx) -> badRequest(ctx, error.getMessage()));
        // Pedir algo que no corresponde en este momento (la IA sin que le toque, el torneo sin empezar): 409.
        javalin.exception(IllegalStateException.class, (error, ctx) ->
                ctx.status(HttpStatus.CONFLICT).json(new ErrorDto(error.getMessage())));
        javalin.exception(NotLoggedInException.class, (error, ctx) ->
                ctx.status(HttpStatus.UNAUTHORIZED).json(new ErrorDto(error.getMessage())));
        return javalin;
    }

    public WebServer start(int port) {
        app = create().start(port);
        return this;
    }

    public int port() {
        return app.port();
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    /** Igual que en la ventana: si sale bien, la cuenta queda creada y la sesión abierta. */
    private void register(Context ctx) {
        CredentialsRequest request = readCredentials(ctx);
        AuthResult result = auth.register(request.username(), request.password(), request.displayName());
        if (!result.isSuccess()) {
            ctx.json(new AuthResponse(false, result.name(), result.getMessage(), null, null));
            return;
        }
        auth.login(request.username(), request.password())
                .ifPresentOrElse(user -> openSession(ctx, user, result),
                        () -> ctx.json(new AuthResponse(false, AuthResult.WRONG_PASSWORD.name(),
                                AuthResult.WRONG_PASSWORD.getMessage(), null, null)));
    }

    private void login(Context ctx) {
        CredentialsRequest request = readCredentials(ctx);
        Optional<User> user = auth.login(request.username(), request.password());
        if (user.isPresent()) {
            openSession(ctx, user.get(), AuthResult.SUCCESS);
            return;
        }
        // Mismo criterio que la ventana y la consola: explicar si falta la cuenta o falló la contraseña.
        AuthResult result = auth.isRegistered(request.username()) ? AuthResult.WRONG_PASSWORD : AuthResult.UNKNOWN_USER;
        ctx.json(new AuthResponse(false, result.name(), result.getMessage(), null, null));
    }

    private void logout(Context ctx) {
        sessions.close(ctx.cookie(SESSION_COOKIE));
        // Max-Age=0 le pide al navegador que borre la cookie.
        ctx.header("Set-Cookie", SESSION_COOKIE + "=; Max-Age=0" + COOKIE_FLAGS);
        ctx.json(new AuthResponse(true, AuthResult.SUCCESS.name(), "Sesión cerrada.", null, null));
    }

    private void openSession(Context ctx, User user, AuthResult result) {
        // Si ya había una sesión en este navegador, se cierra: no quedan sesiones huérfanas.
        sessions.close(ctx.cookie(SESSION_COOKIE));
        String token = sessions.open(user);
        // HttpOnly: el JavaScript de la página no puede leer el token. SameSite=Strict: otro sitio no puede usarlo.
        ctx.header("Set-Cookie", SESSION_COOKIE + "=" + token + COOKIE_FLAGS);
        ctx.json(new AuthResponse(true, result.name(), result.getMessage(), user.username(), user.visibleName()));
    }

    private UserSession currentSession(Context ctx) {
        return sessions.find(ctx.cookie(SESSION_COOKIE))
                .orElseThrow(() -> new NotLoggedInException("Iniciá sesión para jugar."));
    }

    private static WebGameSession game(Context ctx) {
        UserSession session = ctx.attribute(SESSION_ATTRIBUTE);
        return session.game();
    }

    private static WebTournamentSession tournament(Context ctx) {
        UserSession session = ctx.attribute(SESSION_ATTRIBUTE);
        return session.tournament();
    }

    /**
     * Un campo vacío o ausente no es un pedido mal armado: se manda "" y el núcleo decide
     * (usuario inválido, contraseña corta, usuario inexistente), igual que en la ventana.
     */
    private static CredentialsRequest readCredentials(Context ctx) {
        CredentialsRequest request = readBody(ctx, CredentialsRequest.class, "{\"username\":\"...\",\"password\":\"...\"}");
        return new CredentialsRequest(
                request.username() == null ? "" : request.username(),
                request.password() == null ? "" : request.password(),
                request.displayName());
    }

    /** Un cuerpo vacío o que no es JSON también es un pedido inválido (400), no un error del servidor. */
    private static <T> T readBody(Context ctx, Class<T> type, String example) {
        try {
            T request = ctx.bodyAsClass(type);
            if (request == null) {
                throw new IllegalArgumentException("Falta el cuerpo del pedido: " + example + ".");
            }
            return request;
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) { // Jackson puede tirar una excepción "checked" (Javalin está en Kotlin)
            throw new IllegalArgumentException("El cuerpo del pedido no es un JSON válido: " + example + ".");
        }
    }

    /** {"level":"MEDIUM"} -> Difficulty; cuerpo vacío o sin level -> null (se repite la última). */
    private static Difficulty readLevel(Context ctx) {
        if (ctx.body().isBlank()) {
            return null;
        }
        StartRequest request = readBody(ctx, StartRequest.class, "{\"level\":\"MEDIUM\"}");
        if (request.level() == null || request.level().isBlank()) {
            return null;
        }
        try {
            return Difficulty.valueOf(request.level().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Dificultad desconocida: usá EASY, MEDIUM o HARD.");
        }
    }

    /** ?limit=20 (opcional); sin él, las mismas rondas que la ventana Swing. */
    private static int limit(String value) {
        if (value == null || value.isBlank()) {
            return WebHistory.DEFAULT_LIMIT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El límite tiene que ser un número: ?limit=20.");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta el campo \"" + field + "\".");
        }
        return value;
    }

    private static void badRequest(Context ctx, String message) {
        ctx.status(HttpStatus.BAD_REQUEST).json(new ErrorDto(message));
    }

    /** No hay sesión (o venció): el front tiene que mandar a la pantalla de inicio. Se responde 401. */
    private static final class NotLoggedInException extends RuntimeException {
        NotLoggedInException(String message) {
            super(message);
        }
    }
}
