package ar.edu.uade.chess.core.auth;

/**
 * Una cuenta tal como se guarda: nombre de usuario, HASH de la contraseña y el nombre
 * visible que eligió (null si no eligió ninguno).
 * La contraseña en texto plano nunca se guarda ni sale de AuthService.
 */
public record UserAccount(String username, String passwordHash, String displayName) {

    /** Una cuenta sin nombre visible (las creadas antes de que existiera). */
    public UserAccount(String username, String passwordHash) {
        this(username, passwordHash, null);
    }
}
