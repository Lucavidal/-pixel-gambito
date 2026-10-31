package ar.edu.uade.chess.adapters.persistence;

import ar.edu.uade.chess.core.auth.IUserRepository;
import ar.edu.uade.chess.core.auth.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Implementación del puerto IUserRepository con SQL. Siempre con PreparedStatement
 * (parámetros "?"), nunca concatenando texto: así no hay inyección SQL.
 */
public class JdbcUserRepository implements IUserRepository {

    private final SqliteDatabase database;

    public JdbcUserRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        String sql = "SELECT username, password_hash, display_name FROM users WHERE username = ?";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet row = statement.executeQuery()) {
                return row.next()
                        ? Optional.of(new UserAccount(row.getString("username"), row.getString("password_hash"),
                                row.getString("display_name")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo leer el usuario", e);
        }
    }

    @Override
    public void save(UserAccount account) {
        String sql = "INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?)";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.username());
            statement.setString(2, account.passwordHash());
            statement.setString(3, account.displayName()); // null = no eligió nombre
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo guardar el usuario", e);
        }
    }
}
