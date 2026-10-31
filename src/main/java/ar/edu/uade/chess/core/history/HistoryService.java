package ar.edu.uade.chess.core.history;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.User;

import java.util.List;

/** Arma los totales y la lista de últimas rondas a partir de lo guardado. */
public class HistoryService implements IHistoryService {

    private final IMatchHistoryRepository repository;

    public HistoryService(IMatchHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public HistorySummary summary(User user) {
        return summary(user, null);
    }

    @Override
    public List<RoundRecord> recentRounds(User user, int limit) {
        return recentRounds(user, limit, null);
    }

    @Override
    public HistorySummary summary(User user, Difficulty difficulty) {
        List<RoundRecord> rounds = rounds(user, difficulty);
        int wins = 0;
        int losses = 0;
        int draws = 0;
        int titles = 0;
        for (RoundRecord round : rounds) {
            switch (round.outcome()) {
                case WON -> wins++;
                case LOST -> losses++;
                case DRAWN -> draws++;
                case ONGOING -> { } // nunca se guarda una ronda sin terminar
            }
            if (round.wonTheTournament()) {
                titles++;
            }
        }
        return new HistorySummary(rounds.size(), wins, losses, draws, titles);
    }

    @Override
    public List<RoundRecord> recentRounds(User user, int limit, Difficulty difficulty) {
        return rounds(user, difficulty).stream().limit(limit).toList();
    }

    /** Con un filtro, las rondas viejas (sin dificultad guardada) no entran. */
    private List<RoundRecord> rounds(User user, Difficulty difficulty) {
        List<RoundRecord> all = repository.findByUsername(user.username());
        return difficulty == null ? all : all.stream().filter(r -> r.difficulty() == difficulty).toList();
    }
}
