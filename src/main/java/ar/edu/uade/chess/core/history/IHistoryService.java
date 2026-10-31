package ar.edu.uade.chess.core.history;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.User;

import java.util.List;

/**
 * PUERTO DE ENTRADA para consultar el historial. Los adaptadores lo usan para
 * mostrarlo; no hablan directo con la base (el puerto de salida es del núcleo).
 */
public interface IHistoryService {

    HistorySummary summary(User user);

    /** Las últimas rondas del usuario, de la más nueva a la más vieja. */
    List<RoundRecord> recentRounds(User user, int limit);

    /** Totales solo de las rondas jugadas con esa dificultad (null = todas). */
    HistorySummary summary(User user, Difficulty difficulty);

    /** Últimas rondas jugadas con esa dificultad (null = todas). */
    List<RoundRecord> recentRounds(User user, int limit, Difficulty difficulty);
}
