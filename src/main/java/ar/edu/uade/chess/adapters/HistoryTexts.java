package ar.edu.uade.chess.adapters;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.history.HistorySummary;

/**
 * Los textos del historial que comparten las tres interfaces (web, ventana Swing y consola).
 * Traducir el núcleo a palabras en español es trabajo de los adaptadores: por eso vive acá
 * y no en el núcleo, y una sola vez para que las tres pantallas digan lo mismo.
 */
public final class HistoryTexts {

    /** Lo que se muestra cuando una ronda no tiene dificultad guardada. */
    public static final String NO_DIFFICULTY = "—";

    private HistoryTexts() { }

    /** EASY -> "Fácil"; null (rondas viejas o de la escalera clásica) -> "—". */
    public static String difficultyText(Difficulty difficulty) {
        if (difficulty == null) {
            return NO_DIFFICULTY;
        }
        return switch (difficulty) {
            case EASY -> "Fácil";
            case MEDIUM -> "Media";
            case HARD -> "Difícil";
        };
    }

    /**
     * "Tu récord en el torneo: 3 ganadas · 1 tablas · 2 perdidas · 1 título".
     * Sin rondas jugadas, en vez de ceros: "Todavía no jugaste el torneo.".
     * "Tablas" es invariable en ajedrez (como en el resumen del historial): "1 tablas", "2 tablas".
     */
    public static String recordText(HistorySummary s) {
        if (s.roundsPlayed() == 0) {
            return "Todavía no jugaste el torneo.";
        }
        String text = "Tu récord en el torneo: " + count(s.wins(), "ganada", "ganadas")
                + " · " + s.draws() + " tablas"
                + " · " + count(s.losses(), "perdida", "perdidas");
        return s.titles() == 0 ? text : text + " · " + count(s.titles(), "título", "títulos");
    }

    /** "1 ganada" / "2 ganadas" / "0 ganadas". */
    public static String count(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
