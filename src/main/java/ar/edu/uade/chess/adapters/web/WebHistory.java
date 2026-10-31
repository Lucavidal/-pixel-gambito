package ar.edu.uade.chess.adapters.web;

import ar.edu.uade.chess.adapters.HistoryTexts;
import ar.edu.uade.chess.adapters.web.WebDtos.DifficultyOptionDto;
import ar.edu.uade.chess.adapters.web.WebDtos.HistoryDto;
import ar.edu.uade.chess.adapters.web.WebDtos.HistorySummaryDto;
import ar.edu.uade.chess.adapters.web.WebDtos.RoundRecordDto;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.history.HistorySummary;
import ar.edu.uade.chess.core.history.IHistoryService;
import ar.edu.uade.chess.core.history.RoundRecord;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * El historial del torneo tal como lo ve el navegador ("Mi historial").
 *
 * Solo habla con el PUERTO IHistoryService, igual que HistoryDialog (Swing) y la consola:
 * no sabe nada de la base. Arma los mismos textos que la ventana ("12 rondas jugadas: ...",
 * "Ganada", "Campeón") para que el front solo tenga que mostrarlos.
 */
public class WebHistory {

    /** Cuántas rondas se muestran como máximo (las mismas que la ventana Swing). */
    public static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    /** "22 oct 2026, 10:42": el mes en palabras no se confunde entre día/mes y mes/día. */
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-AR"));

    private final IHistoryService history;

    public WebHistory(IHistoryService history) {
        this.history = history;
    }

    /** Las opciones del filtro por dificultad, en orden (Fácil, Media, Difícil). */
    private static final List<DifficultyOptionDto> DIFFICULTIES = Arrays.stream(Difficulty.values())
            .map(d -> new DifficultyOptionDto(d.name(), HistoryTexts.difficultyText(d)))
            .toList();

    /** Totales y últimas rondas del usuario, de la más nueva a la más vieja. */
    public HistoryDto of(User user, int limit) {
        return of(user, limit, null);
    }

    /** Lo mismo, solo con las rondas de esa dificultad (null = todas): los totales también se filtran. */
    public HistoryDto of(User user, int limit, Difficulty difficulty) {
        if (limit < 1) {
            throw new IllegalArgumentException("El límite tiene que ser 1 o más.");
        }
        HistorySummary s = history.summary(user, difficulty);
        List<RoundRecordDto> rounds = history.recentRounds(user, Math.min(limit, MAX_LIMIT), difficulty).stream()
                .map(WebHistory::toDto)
                .toList();
        return new HistoryDto(user.username(), user.visibleName(), difficulty == null ? null : difficulty.name(),
                new HistorySummaryDto(s.roundsPlayed(), s.wins(), s.losses(), s.draws(), s.titles(),
                        summaryText(s), recordText(s)),
                rounds, DIFFICULTIES);
    }

    /** "EASY" -> Difficulty.EASY; vacío o null = sin filtro. Otro valor es un pedido inválido (400). */
    public static Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Difficulty.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Dificultad desconocida: " + value + ". Usá EASY, MEDIUM o HARD.");
        }
    }

    private static RoundRecordDto toDto(RoundRecord r) {
        String outcome = r.wonTheTournament() ? "CHAMPION" : r.outcome().name();
        return new RoundRecordDto(r.playedAt().toString(), DATE.format(r.playedAt()), r.round(), r.totalRounds(),
                r.rivalName(), r.difficulty() == null ? null : r.difficulty().name(),
                HistoryTexts.difficultyText(r.difficulty()), outcome, outcomeText(r), r.userMoves());
    }

    /** "12 rondas jugadas: 7 ganadas, 3 perdidas y 2 tablas. Ganaste 1 torneo." (como HistoryDialog) */
    static String summaryText(HistorySummary s) {
        if (s.roundsPlayed() == 0) {
            return "Sin rondas jugadas.";
        }
        String text = count(s.roundsPlayed(), "ronda jugada", "rondas jugadas") + ": "
                + count(s.wins(), "ganada", "ganadas") + ", "
                + count(s.losses(), "perdida", "perdidas") + " y "
                + s.draws() + " tablas.";
        return s.titles() == 0 ? text : text + " Ganaste " + count(s.titles(), "torneo", "torneos") + ".";
    }

    /** "Tu récord en el torneo: 7 ganadas · 2 tablas · 3 perdidas" (el mismo texto que la ventana Swing). */
    static String recordText(HistorySummary s) {
        return HistoryTexts.recordText(s);
    }

    private static String outcomeText(RoundRecord r) {
        if (r.wonTheTournament()) {
            return "Campeón";
        }
        return switch (r.outcome()) {
            case WON -> "Ganada";
            case LOST -> "Perdida";
            case DRAWN -> "Tablas";
            case ONGOING -> "En curso";
        };
    }

    /** "1 ronda jugada" / "3 rondas jugadas". */
    private static String count(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
