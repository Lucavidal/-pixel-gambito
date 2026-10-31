package ar.edu.uade.chess.core.history;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.tournament.RoundOutcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Totales e historial reciente a partir de lo guardado. */
class HistoryServiceTest {

    private final InMemoryMatchHistoryRepository repository = new InMemoryMatchHistoryRepository();
    private final HistoryService service = new HistoryService(repository);

    private void save(String user, int round, RoundOutcome outcome, int day) {
        repository.save(new RoundRecord(user, "Rival " + round, round, 3, outcome, 10,
                LocalDateTime.of(2026, 10, day, 10, 0)));
    }

    @Test
    void summary_CountsEachOutcome_AndTitles() {
        save("luca", 1, RoundOutcome.WON, 1);
        save("luca", 2, RoundOutcome.DRAWN, 2);
        save("luca", 2, RoundOutcome.WON, 3);
        save("luca", 3, RoundOutcome.WON, 4);   // final ganada = título
        save("luca", 1, RoundOutcome.LOST, 5);

        assertEquals(new HistorySummary(5, 3, 1, 1, 1), service.summary(new User("luca")));
    }

    @Test
    void eachUser_SeesOnlyTheirOwnHistory() {
        save("luca", 1, RoundOutcome.WON, 1);
        save("ana", 1, RoundOutcome.LOST, 2);

        assertEquals(1, service.summary(new User("ana")).roundsPlayed());
        assertEquals(RoundOutcome.LOST, service.recentRounds(new User("ana"), 10).get(0).outcome());
    }

    @Test
    void recentRounds_AreNewestFirst_AndLimited() {
        save("luca", 1, RoundOutcome.WON, 1);
        save("luca", 2, RoundOutcome.WON, 2);
        save("luca", 3, RoundOutcome.LOST, 3);

        List<RoundRecord> recent = service.recentRounds(new User("luca"), 2);

        assertEquals(2, recent.size());
        assertEquals(3, recent.get(0).round());
        assertEquals(2, recent.get(1).round());
    }

    @Test
    void newUser_HasAnEmptyHistory() {
        assertEquals(new HistorySummary(0, 0, 0, 0, 0), service.summary(new User("nuevo")));
        assertTrue(service.recentRounds(new User("nuevo"), 10).isEmpty());
    }

    @Test
    void filteringByDifficulty_CountsOnlyThoseRounds_AndLeavesOutTheOnesWithout() {
        repository.save(new RoundRecord("luca", "A", 1, 4, RoundOutcome.WON, 10, LocalDateTime.of(2026, 10, 1, 10, 0), Difficulty.EASY));
        repository.save(new RoundRecord("luca", "B", 2, 4, RoundOutcome.LOST, 10, LocalDateTime.of(2026, 10, 2, 10, 0), Difficulty.EASY));
        repository.save(new RoundRecord("luca", "C", 1, 4, RoundOutcome.DRAWN, 10, LocalDateTime.of(2026, 10, 3, 10, 0), Difficulty.HARD));
        save("luca", 1, RoundOutcome.WON, 4);   // ronda vieja, sin dificultad
        User luca = new User("luca");

        assertEquals(new HistorySummary(2, 1, 1, 0, 0), service.summary(luca, Difficulty.EASY));
        assertEquals(new HistorySummary(1, 0, 0, 1, 0), service.summary(luca, Difficulty.HARD));
        assertEquals(new HistorySummary(0, 0, 0, 0, 0), service.summary(luca, Difficulty.MEDIUM));
        assertEquals(4, service.summary(luca, null).roundsPlayed(), "sin filtro entran todas, también las viejas");
        assertEquals(List.of("B", "A"), service.recentRounds(luca, 10, Difficulty.EASY).stream()
                .map(RoundRecord::rivalName).toList());
    }
}
