package ar.edu.uade.chess.core.auth;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reglas de las cuentas: qué nombre y qué contraseña son válidos, cómo se
 * registra alguien y cómo se verifica su contraseña.
 *
 * Recibe por constructor dónde se guardan las cuentas (IUserRepository) y cómo
 * se hashean las contraseñas (IPasswordHasher). No sabe nada de bases de datos.
 *
 * Los nombres de usuario no distinguen mayúsculas ("Luca" y "luca" son la misma cuenta)
 * y se les sacan los espacios de los extremos. Pueden tener letras (con acentos),
 * números, espacios y . _ - @ (así sirve un nombre completo o un mail).
 * La contraseña se usa tal cual: no se recorta ni se pasa a minúsculas.
 * El nombre visible es opcional: se le sacan los espacios de los extremos y puede tener
 * hasta MAX_DISPLAY_NAME_LENGTH caracteres.
 */
public class AuthService implements IAuthService {

    public static final int MIN_PASSWORD_LENGTH = 4;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 30;
    public static final int MAX_DISPLAY_NAME_LENGTH = 30;
    private static final Pattern VALID_USERNAME = Pattern.compile(
            "[\\p{L}\\p{N} ._@-]{" + MIN_USERNAME_LENGTH + "," + MAX_USERNAME_LENGTH + "}");

    private final IUserRepository users;
    private final IPasswordHasher hasher;

    public AuthService(IUserRepository users, IPasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    @Override
    public AuthResult register(String username, String password, String displayName) {
        String name = normalize(username);
        if (!VALID_USERNAME.matcher(name).matches()) {
            return AuthResult.INVALID_USERNAME;
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return AuthResult.PASSWORD_TOO_SHORT;
        }
        String shown = displayName == null || displayName.isBlank() ? null : displayName.strip();
        if (shown != null && shown.codePointCount(0, shown.length()) > MAX_DISPLAY_NAME_LENGTH) {
            return AuthResult.DISPLAY_NAME_TOO_LONG;
        }
        if (users.findByUsername(name).isPresent()) {
            return AuthResult.USERNAME_TAKEN;
        }
        users.save(new UserAccount(name, hasher.hash(password), shown));
        return AuthResult.SUCCESS;
    }

    @Override
    public Optional<User> login(String username, String password) {
        String name = normalize(username);
        if (password == null) {
            return Optional.empty();
        }
        return users.findByUsername(name)
                .filter(account -> hasher.matches(password, account.passwordHash()))
                .map(account -> new User(account.username(), account.displayName()));
    }

    @Override
    public boolean isRegistered(String username) {
        return users.findByUsername(normalize(username)).isPresent();
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
