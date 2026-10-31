package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.core.auth.User;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Las sesiones abiertas desde el navegador: token (el valor de la cookie) -> UserSession.
 *
 * Viven en memoria: si se reinicia el servidor, hay que volver a iniciar sesión
 * (las cuentas y el historial siguen en la base). El token es aleatorio y largo
 * (SecureRandom), así que no se puede adivinar el de otro usuario.
 *
 * No sabe nada de HTTP ni de cookies: eso es WebServer. Es un ConcurrentHashMap
 * porque Javalin atiende cada pedido en un hilo distinto.
 */
public class WebSessions {

    private static final int TOKEN_BYTES = 32;

    private final Function<User, UserSession> factory;
    private final Map<String, UserSession> byToken = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** @param factory arma la sesión de un usuario recién logueado (su partida, etc.) */
    public WebSessions(Function<User, UserSession> factory) {
        this.factory = factory;
    }

    /** Abre una sesión nueva para el usuario y devuelve su token. */
    public String open(User user) {
        String token = newToken();
        byToken.put(token, factory.apply(user));
        return token;
    }

    /** La sesión de ese token, si existe. Un token nulo o vencido no es un error: es "no hay sesión". */
    public Optional<UserSession> find(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(byToken.get(token));
    }

    public void close(String token) {
        if (token != null) {
            byToken.remove(token);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
