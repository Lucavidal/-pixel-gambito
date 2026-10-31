package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.core.auth.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Las sesiones se prueban sin HTTP: la partida y el torneo no importan acá, se usa null. */
class WebSessionsTest {

    private final WebSessions sessions = new WebSessions(user -> new UserSession(user, null, null));

    @Test
    void open_ThenFind_ReturnsTheSameUser() {
        String token = sessions.open(new User("luca"));

        assertEquals(Optional.of(new User("luca")), sessions.find(token).map(UserSession::user));
    }

    @Test
    void eachLogin_GetsADifferentToken() {
        assertNotEquals(sessions.open(new User("luca")), sessions.open(new User("luca")));
    }

    @Test
    void unknownOrNullToken_IsNoSession() {
        assertTrue(sessions.find("inventado").isEmpty());
        assertTrue(sessions.find(null).isEmpty());
    }

    @Test
    void close_ForgetsTheSession() {
        String token = sessions.open(new User("luca"));

        sessions.close(token);
        sessions.close(null); // no falla

        assertTrue(sessions.find(token).isEmpty());
    }
}
