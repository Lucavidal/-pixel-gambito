package ar.edu.uade.chess.adapters;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.history.HistorySummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Los textos del historial que comparten la web, la ventana y la consola. */
class HistoryTextsTest {

    @Test
    void record_WithoutRounds_SaysSoInsteadOfZeros() {
        assertEquals("Todavía no jugaste el torneo.", HistoryTexts.recordText(new HistorySummary(0, 0, 0, 0, 0)));
    }

    @Test
    void record_Singular() {
        assertEquals("Tu récord en el torneo: 1 ganada · 1 tablas · 1 perdida",
                HistoryTexts.recordText(new HistorySummary(3, 1, 1, 1, 0)));
    }

    @Test
    void record_PluralAndZero() {
        assertEquals("Tu récord en el torneo: 0 ganadas · 0 tablas · 1 perdida",
                HistoryTexts.recordText(new HistorySummary(1, 0, 1, 0, 0)));
        assertEquals("Tu récord en el torneo: 2 ganadas · 3 tablas · 4 perdidas",
                HistoryTexts.recordText(new HistorySummary(9, 2, 4, 3, 0)));
    }

    @Test
    void record_AddsTitles_OnlyIfThereAreAny() {
        assertEquals("Tu récord en el torneo: 4 ganadas · 0 tablas · 0 perdidas · 1 título",
                HistoryTexts.recordText(new HistorySummary(4, 4, 0, 0, 1)));
        assertEquals("Tu récord en el torneo: 8 ganadas · 0 tablas · 0 perdidas · 2 títulos",
                HistoryTexts.recordText(new HistorySummary(8, 8, 0, 0, 2)));
    }

    @Test
    void difficulty_InSpanish_AndADashWhenThereIsNone() {
        assertEquals("Fácil", HistoryTexts.difficultyText(Difficulty.EASY));
        assertEquals("Media", HistoryTexts.difficultyText(Difficulty.MEDIUM));
        assertEquals("Difícil", HistoryTexts.difficultyText(Difficulty.HARD));
        assertEquals("—", HistoryTexts.difficultyText(null));
    }
}
