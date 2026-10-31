package ar.edu.uade.chess.core.auth;

import java.util.Locale;

/**
 * Un usuario que ya inició sesión. No tiene la contraseña ni su hash:
 * es lo único que el resto del programa necesita saber de él.
 *
 * @param username    el nombre con el que inicia sesión (puede ser un mail)
 * @param displayName cómo quiere que lo llamen; null si no lo dijo (o es una cuenta vieja)
 */
public record User(String username, String displayName) {

    public User {
        displayName = displayName == null || displayName.isBlank() ? null : displayName.strip();
    }

    /** Un usuario sin nombre visible elegido. */
    public User(String username) {
        this(username, null);
    }

    /**
     * El nombre para saludarlo en todas las pantallas. Es el ÚNICO lugar donde se calcula:
     * el que eligió, o si no eligió ninguno, el usuario sin la parte del mail ("lucavidal26@gmail.com"
     * -> "Lucavidal26"), con la primera letra en mayúscula. Así nunca se muestra el mail completo.
     */
    public String visibleName() {
        if (displayName != null) {
            return displayName;
        }
        int at = username.indexOf('@');
        String name = (at >= 0 ? username.substring(0, at) : username).strip();
        if (name.isEmpty()) {
            return username.strip();
        }
        int first = name.offsetByCodePoints(0, 1);
        return name.substring(0, first).toUpperCase(Locale.ROOT) + name.substring(first);
    }
}
