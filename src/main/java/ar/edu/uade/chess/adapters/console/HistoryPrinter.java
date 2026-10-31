package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.adapters.HistoryTexts;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.history.HistorySummary;
import ar.edu.uade.chess.core.history.IHistoryService;
import ar.edu.uade.chess.core.history.RoundRecord;

import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Muestra en la consola el historial del usuario que inició sesión (comando "historial"). */
public class HistoryPrinter {

    public static final int RECENT_ROUNDS = 10;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IHistoryService history;
    private final User user;

    public HistoryPrinter(IHistoryService history, User user) {
        this.history = history;
        this.user = user;
    }

    public void print(PrintStream output) {
        HistorySummary s = history.summary(user);
        output.println("--- Historial de " + user.visibleName() + " ---");
        output.println("Rondas: " + s.roundsPlayed() + " | Ganadas: " + s.wins() + " | Perdidas: " + s.losses()
                + " | Tablas: " + s.draws() + " | Torneos ganados: " + s.titles());
        List<RoundRecord> recent = history.recentRounds(user, RECENT_ROUNDS);
        if (recent.isEmpty()) {
            output.println("Todavía no jugaste ninguna ronda.");
            return;
        }
        for (RoundRecord r : recent) {
            output.println(DATE.format(r.playedAt()) + "  Ronda " + r.round() + "/" + r.totalRounds()
                    + "  vs " + r.rivalName() + "  " + outcomeText(r) + "  (" + r.userMoves() + " jugadas)"
                    + "  Dificultad: " + HistoryTexts.difficultyText(r.difficulty()));
        }
    }

    static String outcomeText(RoundRecord r) {
        if (r.wonTheTournament()) {
            return "GANADA - CAMPEÓN";
        }
        return switch (r.outcome()) {
            case WON -> "GANADA";
            case LOST -> "PERDIDA";
            case DRAWN -> "TABLAS";
            case ONGOING -> "EN CURSO";
        };
    }
}
