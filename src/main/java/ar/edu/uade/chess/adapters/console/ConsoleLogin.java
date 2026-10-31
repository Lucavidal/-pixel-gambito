package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.auth.AuthResult;
import ar.edu.uade.chess.core.auth.IAuthService;
import ar.edu.uade.chess.core.auth.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

/**
 * ADAPTADOR DE ENTRADA: pantalla de inicio de sesión en la consola.
 * Traduce lo que escribe la persona a llamadas al puerto IAuthService.
 *
 * Después de MAX_FAILED_LOGINS intentos fallidos, termina sin usuario.
 * Nota: la consola muestra la contraseña mientras se escribe (la entrada llega
 * como BufferedReader para poder testearla; ver docs/CAMBIOS_TORNEO.md).
 */
public class ConsoleLogin {

    public static final int MAX_FAILED_LOGINS = 3;

    private final IAuthService auth;
    private final BufferedReader input;
    private final PrintStream output;

    public ConsoleLogin(IAuthService auth, BufferedReader input, PrintStream output) {
        this.auth = auth;
        this.input = input;
        this.output = output;
    }

    /** Devuelve el usuario que inició sesión, o vacío si la persona salió o falló demasiadas veces. */
    public Optional<User> run() throws IOException {
        output.println("=== Inicio de sesión ===");
        int failures = 0;
        while (failures < MAX_FAILED_LOGINS) {
            output.print("1 = iniciar sesión | 2 = crear cuenta | salir > ");
            String choice = input.readLine();
            if (choice == null || choice.trim().equalsIgnoreCase("salir")) {
                return Optional.empty();
            }
            switch (choice.trim()) {
                case "1" -> {
                    Optional<User> user = login();
                    if (user.isPresent()) {
                        return user;
                    }
                    failures++;
                }
                case "2" -> {
                    Optional<User> user = register();
                    if (user.isPresent()) {
                        return user;
                    }
                }
                default -> output.println("Opción inválida.");
            }
        }
        output.println("Demasiados intentos fallidos.");
        return Optional.empty();
    }

    private Optional<User> login() throws IOException {
        String username = ask("Usuario: ");
        String password = askPassword("Contraseña: ");
        Optional<User> user = auth.login(username, password);
        if (user.isPresent()) {
            output.println("¡Hola, " + user.get().visibleName() + "!");
        } else {
            output.println(auth.isRegistered(username)
                    ? AuthResult.WRONG_PASSWORD.getMessage()
                    : AuthResult.UNKNOWN_USER.getMessage() + " (opción 2)");
        }
        return user;
    }

    private Optional<User> register() throws IOException {
        String username = ask("Nuevo usuario: ");
        String password = askPassword("Contraseña: ");
        String repeated = askPassword("Repetí la contraseña: ");
        if (!password.equals(repeated)) {
            output.println("Las contraseñas no coinciden.");
            return Optional.empty();
        }
        String displayName = ask("¿Cómo te llamamos? (opcional, Enter para usar tu usuario): ");
        AuthResult result = auth.register(username, password, displayName);
        if (!result.isSuccess()) {
            output.println(result.getMessage());
            return Optional.empty();
        }
        output.println("Cuenta creada.");
        return auth.login(username, password).map(u -> {
            output.println("¡Hola, " + u.visibleName() + "!");
            return u;
        });
    }

    private String ask(String prompt) throws IOException {
        output.print(prompt);
        String line = input.readLine();
        return line == null ? "" : line.trim();
    }

    /** La contraseña se usa tal cual se escribió: sin recortar espacios (igual que en la ventana). */
    private String askPassword(String prompt) throws IOException {
        output.print(prompt);
        String line = input.readLine();
        return line == null ? "" : line;
    }
}
