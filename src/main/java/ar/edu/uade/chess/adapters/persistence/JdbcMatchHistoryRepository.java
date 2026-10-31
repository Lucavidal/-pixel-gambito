package ar.edu.uade.chess.adapters.persistence;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.history.IMatchHistoryRepository;
import ar.edu.uade.chess.core.history.RoundRecord;
import ar.edu.uade.chess.core.tournament.RoundOutcome;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del puerto IMatchHistoryRepository con SQL.
 * El resultado se guarda como texto ("WON") y la fecha en formato ISO
 * ("2026-10-22T09:15:00"), que SQLite ordena bien como texto. La dificultad también va
 * como texto ("EASY"), o NULL si no se eligió (y en las filas de antes de la migración).
 */
public class JdbcMatchHistoryRepository implements IMatchHistoryRepository {

    private final SqliteDatabase database;

    public JdbcMatchHistoryRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public void save(RoundRecord record) {
        String sql = """
                INSERT INTO round_results (username, rival, round, total_rounds, outcome, user_moves, played_at, difficulty)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.username());
            statement.setString(2, record.rivalName());
            statement.setInt(3, record.round());
            statement.setInt(4, record.totalRounds());
            statement.setString(5, record.outcome().name());
            statement.setInt(6, record.userMoves());
            statement.setString(7, record.playedAt().toString());
            statement.setString(8, record.difficulty() == null ? null : record.difficulty().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo guardar la ronda en el historial", e);
        }
    }

    @Override
    public List<RoundRecord> findByUsername(String username) {
        String sql = """
                SELECT username, rival, round, total_rounds, outcome, user_moves, played_at, difficulty
                FROM round_results WHERE username = ? ORDER BY played_at DESC, id DESC""";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            List<RoundRecord> rounds = new ArrayList<>();
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    rounds.add(new RoundRecord(
                            row.getString("username"),
                            row.getString("rival"),
                            row.getInt("round"),
                            row.getInt("total_rounds"),
                            RoundOutcome.valueOf(row.getString("outcome")),
                            row.getInt("user_moves"),
                            LocalDateTime.parse(row.getString("played_at")),
                            difficulty(row.getString("difficulty"))));
                }
            }
            return rounds;
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo leer el historial", e);
        }
    }

    private static Difficulty difficulty(String stored) {
        return stored == null ? null : Difficulty.valueOf(stored);
    }
}
