package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.web.WebDtos.HistoryDto;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.history.HistoryService;
import ar.edu.uade.chess.core.history.InMemoryMatchHistoryRepository;
import ar.edu.uade.chess.core.history.RoundRecord;
import ar.edu.uade.chess.core.tournament.RoundOutcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** "Mi historial" tal como lo ve el navegador: mismos textos que la ventana Swing. */
class WebHistoryTest {

    private final InMemoryMatchHistoryRepository repository = new InMemoryMatchHistoryRepository();
    private final WebHistory history = new WebHistory(new HistoryService(repository));
    private final User luca = new User("luca");

    private void save(String rival, int round, RoundOutcome outcome, int moves, LocalDateTime when) {
        repository.save(new RoundRecord("luca", rival, round, 3, outcome, moves, when));
    }

    @Test
    void withoutRounds_SaysSo() {
        HistoryDto dto = history.of(luca, 50);

        assertEquals(0, dto.summary().roundsPlayed());
        assertEquals("Sin rondas jugadas.", dto.summary().text());
        assertEquals("Todavía no jugaste el torneo.", dto.summary().record());
        assertTrue(dto.rounds().isEmpty());
    }

    @Test
    void summaryAndRounds_UseTheSameTextsAsTheWindow() {
        save("Tigran Petrosian", 1, RoundOutcome.WON, 12, LocalDateTime.of(2026, 10, 22, 10, 42));
        save("José Raúl Capablanca", 2, RoundOutcome.DRAWN, 30, LocalDateTime.of(2026, 10, 22, 11, 5));
        save("José Raúl Capablanca", 2, RoundOutcome.WON, 25, LocalDateTime.of(2026, 10, 22, 11, 40));
        save("Mikhail Tal", 3, RoundOutcome.WON, 40, LocalDateTime.of(2026, 10, 22, 12, 30));

        HistoryDto dto = history.of(luca, 50);

        assertEquals("4 rondas jugadas: 3 ganadas, 0 perdidas y 1 tablas. Ganaste 1 torneo.", dto.summary().text());
        assertEquals("Tu récord en el torneo: 3 ganadas · 1 tablas · 0 perdidas · 1 título", dto.summary().record());
        assertEquals(4, dto.rounds().size());
        List<String> outcomes = dto.rounds().stream().map(WebDtos.RoundRecordDto::outcome).toList();
        assertTrue(outcomes.contains("CHAMPION"), "ganar la última ronda es salir campeón: " + outcomes);
        assertTrue(outcomes.contains("DRAWN"));
    }

    @Test
    void round_HasTheDateFormattedAndInIso() {
        save("Tigran Petrosian", 1, RoundOutcome.LOST, 9, LocalDateTime.of(2026, 10, 22, 10, 42));

        WebDtos.RoundRecordDto round = history.of(luca, 50).rounds().get(0);

        assertEquals("2026-10-22T10:42", round.playedAt());
        assertTrue(round.date().startsWith("22 oct"), round.date());
        assertTrue(round.date().endsWith("2026, 10:42"), round.date());
        assertEquals("Perdida", round.outcomeText());
        assertEquals(9, round.userMoves());
    }

    @Test
    void limit_CutsTheList_AndMustBePositive() {
        for (int i = 0; i < 5; i++) {
            save("Tigran Petrosian", 1, RoundOutcome.LOST, i, LocalDateTime.of(2026, 10, 22, 10, i));
        }

        assertEquals(2, history.of(luca, 2).rounds().size());
        assertEquals(5, history.of(luca, 2).summary().roundsPlayed(), "los totales cuentan todas las rondas");
        assertThrows(IllegalArgumentException.class, () -> history.of(luca, 0));
    }

    @Test
    void eachUser_SeesOnlyTheirRounds() {
        save("Tigran Petrosian", 1, RoundOutcome.WON, 12, LocalDateTime.of(2026, 10, 22, 10, 42));

        assertEquals(0, history.of(new User("ana"), 50).summary().roundsPlayed());
    }

    @Test
    void round_CarriesItsDifficulty_InSpanish_OrADash() {
        repository.save(new RoundRecord("luca", "Mikhail Tal", 3, 4, RoundOutcome.WON, 20,
                LocalDateTime.of(2026, 10, 22, 12, 0), Difficulty.HARD));
        save("Tigran Petrosian", 1, RoundOutcome.LOST, 9, LocalDateTime.of(2026, 10, 22, 10, 42));

        List<WebDtos.RoundRecordDto> rounds = history.of(luca, 50).rounds();

        assertEquals("HARD", rounds.get(0).difficulty());
        assertEquals("Difícil", rounds.get(0).difficultyText());
        assertNull(rounds.get(1).difficulty());
        assertEquals("—", rounds.get(1).difficultyText());
    }

    @Test
    void filterByDifficulty_AppliesToRoundsAndTotals() {
        repository.save(new RoundRecord("luca", "Paul Morphy", 1, 4, RoundOutcome.WON, 10,
                LocalDateTime.of(2026, 10, 22, 10, 0), Difficulty.EASY));
        repository.save(new RoundRecord("luca", "Max Euwe", 2, 4, RoundOutcome.LOST, 15,
                LocalDateTime.of(2026, 10, 22, 11, 0), Difficulty.EASY));
        repository.save(new RoundRecord("luca", "Mikhail Tal", 1, 4, RoundOutcome.WON, 20,
                LocalDateTime.of(2026, 10, 22, 12, 0), Difficulty.HARD));

        HistoryDto easy = history.of(luca, 50, WebHistory.parseDifficulty("easy"));

        assertEquals("EASY", easy.difficulty());
        assertEquals(2, easy.rounds().size());
        assertEquals(2, easy.summary().roundsPlayed());
        assertEquals(1, easy.summary().wins());
        assertEquals(1, easy.summary().losses());
        assertEquals(3, history.of(luca, 50).summary().roundsPlayed(), "sin filtro, todas");
        assertEquals(List.of("EASY", "MEDIUM", "HARD"),
                easy.difficulties().stream().map(WebDtos.DifficultyOptionDto::id).toList());
        assertNull(WebHistory.parseDifficulty(""));
        assertThrows(IllegalArgumentException.class, () -> WebHistory.parseDifficulty("imposible"));
    }

    @Test
    void history_GreetsWithTheVisibleName_NotTheMail() {
        assertEquals("Lucavidal26", history.of(new User("lucavidal26@gmail.com"), 50).displayName());
    }
}
