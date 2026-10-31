package ar.edu.uade.chess.adapters.persistence;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.AuthService;
import ar.edu.uade.chess.core.auth.Pbkdf2PasswordHasher;
import ar.edu.uade.chess.core.auth.UserAccount;
import ar.edu.uade.chess.core.history.RoundRecord;
import ar.edu.uade.chess.core.tournament.RoundOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de INTEGRACIÓN del adaptador de persistencia contra un SQLite real,
 * en un archivo temporal que JUnit borra al terminar. Es el único test que toca una base.
 */
class SqliteRepositoriesTest {

    @TempDir
    Path folder;

    private SqliteDatabase database;
    private JdbcUserRepository users;
    private JdbcMatchHistoryRepository history;

    @BeforeEach
    void freshDatabase() {
        database = new SqliteDatabase(folder.resolve("test.db"));
        database.createSchema();
        users = new JdbcUserRepository(database);
        history = new JdbcMatchHistoryRepository(database);
    }

    @Test
    void savedUser_IsFoundByUsername() {
        users.save(new UserAccount("luca", "hash-de-prueba"));

        assertEquals(new UserAccount("luca", "hash-de-prueba"), users.findByUsername("luca").orElseThrow());
        assertTrue(users.findByUsername("otro").isEmpty());
    }

    @Test
    void duplicatedUsername_IsRejectedByTheDatabase() {
        users.save(new UserAccount("luca", "h1"));

        assertThrows(DatabaseException.class, () -> users.save(new UserAccount("luca", "h2")));
    }

    @Test
    void historyRecords_AreReadBackNewestFirst() {
        users.save(new UserAccount("luca", "h"));
        history.save(new RoundRecord("luca", "Tigran Petrosian", 1, 3, RoundOutcome.WON, 12,
                LocalDateTime.of(2026, 10, 22, 10, 0)));
        history.save(new RoundRecord("luca", "José Raúl Capablanca", 2, 3, RoundOutcome.LOST, 30,
                LocalDateTime.of(2026, 10, 22, 11, 0)));

        List<RoundRecord> rounds = history.findByUsername("luca");

        assertEquals(2, rounds.size());
        assertEquals("José Raúl Capablanca", rounds.get(0).rivalName());
        assertEquals(RoundOutcome.LOST, rounds.get(0).outcome());
        assertEquals(30, rounds.get(0).userMoves());
        assertEquals(LocalDateTime.of(2026, 10, 22, 10, 0), rounds.get(1).playedAt());
    }

    @Test
    void historyOfAnUnknownUser_IsRejected_ByTheForeignKey() {
        assertThrows(DatabaseException.class, () -> history.save(new RoundRecord("nadie", "X", 1, 3,
                RoundOutcome.WON, 1, LocalDateTime.of(2026, 1, 1, 0, 0))));
    }

    @Test
    void dataSurvivesReopeningTheDatabase_AndCreatingTheSchemaAgain() {
        new AuthService(users, new Pbkdf2PasswordHasher(1_000)).register("luca", "clave123");

        SqliteDatabase reopened = new SqliteDatabase(folder.resolve("test.db"));
        reopened.createSchema(); // no debe borrar nada

        AuthService auth = new AuthService(new JdbcUserRepository(reopened), new Pbkdf2PasswordHasher(1_000));
        assertTrue(auth.login("luca", "clave123").isPresent());
    }

    @Test
    void usernameWithSqlCharacters_IsTreatedAsPlainText() {
        users.save(new UserAccount("x' OR '1'='1", "h"));

        assertTrue(users.findByUsername("luca' OR '1'='1").isEmpty(), "los parámetros '?' evitan la inyección SQL");
        assertTrue(users.findByUsername("x' OR '1'='1").isPresent());
    }

    @Test
    void difficulty_IsSavedAndReadBack_AndMissingOneStaysNull() {
        users.save(new UserAccount("luca", "h"));
        history.save(new RoundRecord("luca", "Mikhail Tal", 3, 4, RoundOutcome.WON, 20,
                LocalDateTime.of(2026, 10, 22, 12, 0), Difficulty.HARD));
        history.save(new RoundRecord("luca", "Tigran Petrosian", 1, 3, RoundOutcome.LOST, 5,
                LocalDateTime.of(2026, 10, 22, 11, 0)));

        List<RoundRecord> rounds = history.findByUsername("luca");

        assertEquals(Difficulty.HARD, rounds.get(0).difficulty());
        assertNull(rounds.get(1).difficulty());
    }

    @Test
    void displayName_IsSavedAndReadBack_AndMissingOneStaysNull() {
        users.save(new UserAccount("lucavidal26@gmail.com", "h", "Luca"));
        users.save(new UserAccount("ana", "h"));

        assertEquals("Luca", users.findByUsername("lucavidal26@gmail.com").orElseThrow().displayName());
        assertNull(users.findByUsername("ana").orElseThrow().displayName());
    }

    /**
     * Una base creada con el esquema de ANTES (sin display_name ni difficulty), como el ajedrez.db
     * que ya tiene el usuario: createSchema() le agrega las columnas sin perder nada.
     */
    @Test
    void oldDatabase_IsMigrated_WithoutLosingData_AndOldRowsAreNull() throws Exception {
        Path file = folder.resolve("vieja.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (username TEXT PRIMARY KEY, password_hash TEXT NOT NULL)");
            statement.execute("""
                    CREATE TABLE round_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL REFERENCES users(username),
                        rival TEXT NOT NULL, round INTEGER NOT NULL, total_rounds INTEGER NOT NULL,
                        outcome TEXT NOT NULL, user_moves INTEGER NOT NULL, played_at TEXT NOT NULL)""");
            statement.execute("INSERT INTO users VALUES ('lucavidal26@gmail.com', 'hash-viejo')");
            statement.execute("INSERT INTO round_results (username, rival, round, total_rounds, outcome, user_moves, played_at) "
                    + "VALUES ('lucavidal26@gmail.com', 'Mikhail Tal', 3, 3, 'LOST', 17, '2026-10-20T18:30')");
        }

        SqliteDatabase old = new SqliteDatabase(file);
        old.createSchema();
        old.createSchema(); // correrla otra vez no hace nada (la columna ya existe)
        JdbcUserRepository oldUsers = new JdbcUserRepository(old);
        JdbcMatchHistoryRepository oldHistory = new JdbcMatchHistoryRepository(old);

        assertEquals(new UserAccount("lucavidal26@gmail.com", "hash-viejo", null),
                oldUsers.findByUsername("lucavidal26@gmail.com").orElseThrow());
        RoundRecord oldRound = oldHistory.findByUsername("lucavidal26@gmail.com").get(0);
        assertEquals("Mikhail Tal", oldRound.rivalName());
        assertEquals(RoundOutcome.LOST, oldRound.outcome());
        assertEquals(17, oldRound.userMoves());
        assertNull(oldRound.difficulty(), "las filas de antes de la migración quedan en NULL");

        // Y desde ahora se guardan las columnas nuevas
        oldHistory.save(new RoundRecord("lucavidal26@gmail.com", "Bobby Fischer", 1, 4, RoundOutcome.WON, 22,
                LocalDateTime.of(2026, 10, 30, 19, 0), Difficulty.MEDIUM));
        assertEquals(Difficulty.MEDIUM, oldHistory.findByUsername("lucavidal26@gmail.com").get(0).difficulty());
        assertEquals(2, oldHistory.findByUsername("lucavidal26@gmail.com").size());
        try (Connection connection = old.connect(); Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(users)")) {
            int displayNameColumns = 0;
            while (columns.next()) {
                if (columns.getString("name").equals("display_name")) {
                    displayNameColumns++;
                }
            }
            assertEquals(1, displayNameColumns);
        }
    }
}
