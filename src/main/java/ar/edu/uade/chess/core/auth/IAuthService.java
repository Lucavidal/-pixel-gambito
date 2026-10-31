package ar.edu.uade.chess.core.auth;

import java.util.Optional;

/**
 * PUERTO DE ENTRADA para crear cuentas e iniciar sesión.
 * Lo usan los adaptadores (pantalla de inicio en consola o en ventana).
 */
public interface IAuthService {

    /** Crea la cuenta. displayName es opcional (null o vacío = se usa el usuario, ver User.visibleName()). */
    AuthResult register(String username, String password, String displayName);

    /** Crea una cuenta sin nombre visible. */
    default AuthResult register(String username, String password) {
        return register(username, password, null);
    }

    /** Devuelve el usuario si el nombre y la contraseña son correctos. */
    Optional<User> login(String username, String password);

    /**
     * ¿Existe una cuenta con ese nombre? Sirve para explicar por qué falló un inicio de
     * sesión ("no existe, creá una cuenta" vs. "contraseña incorrecta"). Revela qué
     * usuarios existen, algo aceptable en una aplicación local de un solo equipo.
     */
    boolean isRegistered(String username);
}
