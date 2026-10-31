package ar.edu.uade.chess.adapters.persistence;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ADAPTADOR DE SALIDA: la base de datos SQLite (un único archivo, sin servidor).
 *
 * Abre una conexión por operación (try-with-resources en los repositorios): para
 * una aplicación de un solo usuario es lo más simple y no deja conexiones colgadas.
 * Crea las tablas la primera vez; si ya existen, no las toca. Las columnas que se agregaron
 * después (MIGRACIONES) se suman con ALTER TABLE ... ADD COLUMN sobre la tabla existente:
 * nunca se borra ni se recrea una tabla, así no se pierde lo que ya estaba guardado.
 */
public class SqliteDatabase {

    private final String url;

    public SqliteDatabase(Path file) {
        this.url = "jdbc:sqlite:" + file.toAbsolutePath();
    }

    public Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON"); // SQLite no controla las FK si no se le pide
        }
        return connection;
    }

    /** Crea las tablas si no existen y agrega las columnas nuevas. Se llama una vez al arrancar el programa. */
    public void createSchema() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        username      TEXT PRIMARY KEY,
                        password_hash TEXT NOT NULL
                    )""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS round_results (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        username     TEXT    NOT NULL REFERENCES users(username),
                        rival        TEXT    NOT NULL,
                        round        INTEGER NOT NULL,
                        total_rounds INTEGER NOT NULL,
                        outcome      TEXT    NOT NULL,
                        user_moves   INTEGER NOT NULL,
                        played_at    TEXT    NOT NULL
                    )""");
            // Migraciones: en una base vieja las filas que ya estaban quedan en NULL
            addColumnIfMissing(connection, "users", "display_name", "TEXT");
            addColumnIfMissing(connection, "round_results", "difficulty", "TEXT");
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo crear la base de datos", e);
        }
    }

    /** ALTER TABLE ... ADD COLUMN solo si la columna no existe (SQLite no tiene "ADD COLUMN IF NOT EXISTS"). */
    private static void addColumnIfMissing(Connection connection, String table, String column, String type)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (columns.getString("name").equalsIgnoreCase(column)) {
                    return;
                }
            }
        }
        // Los nombres son constantes de esta clase, nunca vienen del usuario: no hay inyección posible
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }
}
