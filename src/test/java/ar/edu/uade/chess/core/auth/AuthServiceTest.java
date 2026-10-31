package ar.edu.uade.chess.core.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas de las cuentas, con un repositorio en memoria y un hash rápido (pocas iteraciones). */
class AuthServiceTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final AuthService auth = new AuthService(users, new Pbkdf2PasswordHasher(1_000));

    @Test
    void registeredUser_CanLogIn() {
        AuthResult result = auth.register("luca", "clave123");

        Optional<User> user = auth.login("luca", "clave123");

        assertEquals(AuthResult.SUCCESS, result);
        assertEquals(Optional.of(new User("luca")), user);
    }

    @Test
    void wrongPassword_DoesNotLogIn() {
        auth.register("luca", "clave123");

        assertTrue(auth.login("luca", "otra").isEmpty());
    }

    @Test
    void unknownUser_DoesNotLogIn() {
        assertTrue(auth.login("nadie", "clave123").isEmpty());
    }

    @Test
    void sameUsernameTwice_IsRejected_EvenWithDifferentCase() {
        auth.register("luca", "clave123");

        assertEquals(AuthResult.USERNAME_TAKEN, auth.register("LUCA", "otraClave"));
        assertTrue(auth.login("Luca", "clave123").isPresent(), "el nombre no distingue mayúsculas");
    }

    @Test
    void invalidUsernames_AreRejected() {
        assertEquals(AuthResult.INVALID_USERNAME, auth.register("ab", "clave123"));
        assertEquals(AuthResult.INVALID_USERNAME, auth.register("x".repeat(31), "clave123"));
        assertEquals(AuthResult.INVALID_USERNAME, auth.register("con'comilla", "clave123"));
        assertEquals(AuthResult.INVALID_USERNAME, auth.register("   ", "clave123"));
    }

    @Test
    void fullNamesAndEmails_AreValidUsernames() {
        assertEquals(AuthResult.SUCCESS, auth.register("Luca Vidal", "clave123"));
        assertEquals(AuthResult.SUCCESS, auth.register("lucavidal26@gmail.com", "clave123"));
        assertEquals(AuthResult.SUCCESS, auth.register("José_Raúl-2", "clave123"));

        assertTrue(auth.login("  luca vidal ", "clave123").isPresent(), "se recortan los extremos y no distingue mayúsculas");
    }

    @Test
    void passwordIsUsedExactlyAsTyped() {
        auth.register("luca", " Clave123 ");

        assertTrue(auth.login("luca", " Clave123 ").isPresent());
        assertTrue(auth.login("luca", "Clave123").isEmpty(), "no se recortan los espacios de la contraseña");
        assertTrue(auth.login("luca", " clave123 ").isEmpty(), "distingue mayúsculas");
    }

    @Test
    void isRegistered_TellsWhetherTheAccountExists() {
        auth.register("luca", "clave123");

        assertTrue(auth.isRegistered("LUCA"));
        assertFalse(auth.isRegistered("ana"));
    }

    @Test
    void shortPassword_IsRejected() {
        assertEquals(AuthResult.PASSWORD_TOO_SHORT, auth.register("luca", "123"));
        assertTrue(users.findByUsername("luca").isEmpty());
    }

    @Test
    void passwordIsNeverStoredInPlainText() {
        auth.register("luca", "clave123");

        String stored = users.findByUsername("luca").orElseThrow().passwordHash();

        assertFalse(stored.contains("clave123"));
        assertTrue(stored.startsWith("pbkdf2_sha256$"));
    }

    @Test
    void displayName_IsSavedAndComesBackOnLogin_Trimmed() {
        assertEquals(AuthResult.SUCCESS, auth.register("lucavidal26@gmail.com", "clave123", "  Luca  "));

        User user = auth.login("lucavidal26@gmail.com", "clave123").orElseThrow();

        assertEquals("Luca", user.displayName());
        assertEquals("Luca", user.visibleName());
    }

    @Test
    void blankDisplayName_IsSavedAsNone() {
        auth.register("ana@mail.com", "clave123", "   ");

        assertNull(users.findByUsername("ana@mail.com").orElseThrow().displayName());
        assertEquals("Ana", auth.login("ana@mail.com", "clave123").orElseThrow().visibleName());
    }

    @Test
    void displayNameLongerThan30_IsRejected_AndNoAccountIsCreated() {
        String name = "x".repeat(AuthService.MAX_DISPLAY_NAME_LENGTH + 1);

        assertEquals(AuthResult.DISPLAY_NAME_TOO_LONG, auth.register("luca", "clave123", name));
        assertFalse(auth.isRegistered("luca"));
        assertEquals(AuthResult.SUCCESS, auth.register("luca", "clave123", "x".repeat(AuthService.MAX_DISPLAY_NAME_LENGTH)));
    }
}
