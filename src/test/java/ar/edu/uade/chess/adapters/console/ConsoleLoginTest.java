package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.auth.AuthService;
import ar.edu.uade.chess.core.auth.InMemoryUserRepository;
import ar.edu.uade.chess.core.auth.Pbkdf2PasswordHasher;
import ar.edu.uade.chess.core.auth.User;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Pantalla de inicio de sesión por consola, con la entrada simulada por texto. */
class ConsoleLoginTest {

    private final AuthService auth = new AuthService(new InMemoryUserRepository(), new Pbkdf2PasswordHasher(1_000));
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private Optional<User> run(String lines) throws Exception {
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        return new ConsoleLogin(auth, new BufferedReader(new StringReader(lines)), output).run();
    }

    private String output() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void creatingAnAccount_LogsTheUserIn() throws Exception {
        Optional<User> user = run("2\nluca\nclave123\nclave123\n");

        assertEquals(Optional.of(new User("luca")), user);
        assertTrue(output().contains("Cuenta creada."));
    }

    @Test
    void existingUser_LogsInWithTheRightPassword() throws Exception {
        auth.register("luca", "clave123");

        Optional<User> user = run("1\nluca\nclave123\n");

        assertEquals(Optional.of(new User("luca")), user);
        assertTrue(output().contains("¡Hola, Luca!"), "saluda con el nombre visible, no con el usuario tal cual");
    }

    @Test
    void mismatchedPasswords_DoNotCreateTheAccount() throws Exception {
        Optional<User> user = run("2\nluca\nclave123\nclave999\nsalir\n");

        assertTrue(user.isEmpty());
        assertTrue(output().contains("no coinciden"));
        assertTrue(auth.login("luca", "clave123").isEmpty());
    }

    @Test
    void tooManyWrongPasswords_EndsWithoutUser() throws Exception {
        auth.register("luca", "clave123");

        Optional<User> user = run("1\nluca\nmal\n1\nluca\nmal\n1\nluca\nmal\n1\nluca\nclave123\n");

        assertTrue(user.isEmpty(), "después de 3 fallos no deja seguir intentando");
        assertTrue(output().contains("Demasiados intentos fallidos."));
    }
}
