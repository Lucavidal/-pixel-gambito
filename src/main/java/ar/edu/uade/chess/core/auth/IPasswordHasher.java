package ar.edu.uade.chess.core.auth;

/**
 * Convierte una contraseña en un hash que se puede guardar, y verifica una
 * contraseña contra ese hash. Es una interfaz para poder cambiar el algoritmo
 * (y para que los tests usen uno rápido).
 */
public interface IPasswordHasher {

    String hash(String password);

    boolean matches(String password, String storedHash);
}
