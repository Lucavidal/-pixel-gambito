package ar.edu.uade.chess.core.auth;

import java.util.Optional;

/**
 * PUERTO DE SALIDA: dónde se guardan las cuentas.
 *
 * El núcleo no sabe si es una base de datos, un archivo o una lista en memoria.
 * La implementación real (SQLite) vive en adapters/persistence; los tests usan una en memoria.
 */
public interface IUserRepository {

    Optional<UserAccount> findByUsername(String username);

    /** Guarda una cuenta nueva. El nombre de usuario no debe existir. */
    void save(UserAccount account);
}
