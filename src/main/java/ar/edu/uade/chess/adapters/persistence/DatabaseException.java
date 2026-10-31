package ar.edu.uade.chess.adapters.persistence;

/**
 * Error de acceso a la base. Envuelve SQLException (que es "checked") para que
 * los puertos del núcleo no tengan que declarar nada de SQL en sus firmas.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
