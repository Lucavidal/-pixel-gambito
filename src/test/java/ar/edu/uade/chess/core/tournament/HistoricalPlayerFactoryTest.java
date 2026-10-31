package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.ai.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** El plantel de rivales que arma la fábrica. */
class HistoricalPlayerFactoryTest {

    @Test
    void roster_HasOneRivalPerRound() {
        assertEquals(HistoricalPlayerFactory.DEFAULT_ROUNDS, HistoricalPlayerFactory.createRoster().size());
    }

    @Test
    void roster_IsOrderedFromEasiestToHardest() {
        List<Difficulty> difficulties = HistoricalPlayerFactory.createRoster().stream()
                .map(HistoricalPlayer::difficulty).toList();

        assertEquals(List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD), difficulties);
    }

    @Test
    void roster_HasDifferentStylesAndNames() {
        List<HistoricalPlayer> roster = HistoricalPlayerFactory.createRoster();

        assertEquals(roster.size(), roster.stream().map(HistoricalPlayer::name).collect(Collectors.toSet()).size());
        assertEquals(roster.size(), roster.stream().map(p -> p.style().getDisplayName()).collect(Collectors.toSet()).size());
    }

    // --- plantel completo y escalera eligiendo la dificultad ---

    @Test
    void allPlayers_AreFourPerLevel_WithDifferentNames_AndIncludeTheClassicLadder() {
        List<HistoricalPlayer> all = HistoricalPlayerFactory.allPlayers();

        assertEquals(12, all.size());
        for (Difficulty level : Difficulty.values()) {
            assertEquals(4, all.stream().filter(p -> p.difficulty() == level).count(), level.name());
        }
        assertEquals(12, all.stream().map(HistoricalPlayer::name).collect(Collectors.toSet()).size());
        assertTrue(all.containsAll(HistoricalPlayerFactory.createRoster()), "los rivales de la escalera clásica están");
    }

    @Test
    void allPlayers_EveryLevelHasTheThreeStyles() {
        for (Difficulty level : Difficulty.values()) {
            Set<String> styles = HistoricalPlayerFactory.allPlayers().stream()
                    .filter(p -> p.difficulty() == level)
                    .map(p -> p.style().getDisplayName())
                    .collect(Collectors.toSet());
            assertEquals(3, styles.size(), level.name());
        }
    }

    @Test
    void ladder_StartsAtTheChosenLevel_AndGoesUp() {
        assertEquals(List.of(Difficulty.EASY, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.MEDIUM),
                difficultiesOf(Difficulty.EASY));
        assertEquals(List.of(Difficulty.MEDIUM, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.HARD),
                difficultiesOf(Difficulty.MEDIUM));
        assertEquals(List.of(Difficulty.HARD, Difficulty.HARD, Difficulty.HARD, Difficulty.HARD),
                difficultiesOf(Difficulty.HARD));
    }

    @Test
    void ladder_HasNoRepeatedRivals_AndChangesWithTheDraw() {
        Set<List<String>> ladders = new HashSet<>();
        for (int seed = 0; seed < 20; seed++) {
            List<String> names = HistoricalPlayerFactory.createLadder(Difficulty.EASY, new Random(seed)).stream()
                    .map(HistoricalPlayer::name).toList();
            assertEquals(HistoricalPlayerFactory.LADDER_ROUNDS, new HashSet<>(names).size(), names.toString());
            ladders.add(names);
        }
        assertTrue(ladders.size() > 1, "con otro sorteo salen otros rivales");
    }

    @Test
    void ladder_WithTheSameSeed_IsTheSame() {
        assertEquals(HistoricalPlayerFactory.createLadder(Difficulty.MEDIUM, new Random(7)),
                HistoricalPlayerFactory.createLadder(Difficulty.MEDIUM, new Random(7)));
    }

    private static List<Difficulty> difficultiesOf(Difficulty start) {
        return HistoricalPlayerFactory.createLadder(start, new Random(1)).stream()
                .map(HistoricalPlayer::difficulty).toList();
    }
}
