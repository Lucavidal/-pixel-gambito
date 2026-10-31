package ar.edu.uade.chess.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El hash de contraseñas. */
class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);

    @Test
    void correctPassword_Matches_AndWrongOneDoesNot() {
        String stored = hasher.hash("clave123");

        assertTrue(hasher.matches("clave123", stored));
        assertFalse(hasher.matches("clave124", stored));
    }

    @Test
    void samePasswordTwice_GivesDifferentHashes_BecauseOfTheSalt() {
        assertNotEquals(hasher.hash("clave123"), hasher.hash("clave123"));
    }

    @Test
    void hashMadeWithOtherIterations_StillVerifies() {
        String stored = new Pbkdf2PasswordHasher(2_000).hash("clave123");

        assertTrue(hasher.matches("clave123", stored), "las iteraciones viajan dentro del hash");
    }

    @Test
    void malformedHash_DoesNotMatch_AndDoesNotThrow() {
        assertFalse(hasher.matches("clave123", "cualquier-cosa"));
        assertFalse(hasher.matches("clave123", "pbkdf2_sha256$x$y$z"));
        assertFalse(hasher.matches("clave123", null));
    }
}
