package ar.edu.uade.chess.core.history;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.tournament.HistoricalPlayer;
import ar.edu.uade.chess.core.tournament.ITournament;
import ar.edu.uade.chess.core.tournament.TournamentStatus;
import ar.edu.uade.chess.core.tournament.TurnReport;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Patrón DECORATOR: un torneo que hace todo lo que hace el torneo que envuelve,
 * y además guarda en el historial cada ronda que termina.
 *
 * Así Tournament no se entera de que existe un historial (no se modificó), y el
 * guardado se puede sacar o cambiar sin tocar las reglas del torneo. Para el
 * adaptador es un ITournament más.
 *
 * El reloj (Clock) entra por constructor para que los tests sean deterministas.
 * La dificultad es la que eligió el usuario al empezar (la web la pide); la escalera clásica
 * de consola y ventana no la pide, y sus rondas se guardan sin dificultad (null).
 */
public class RecordingTournament implements ITournament {

    private final ITournament tournament;
    private final IMatchHistoryRepository history;
    private final User user;
    private final Clock clock;
    private final Difficulty chosenDifficulty;

    private int userMovesThisRound;

    public RecordingTournament(ITournament tournament, IMatchHistoryRepository history, User user, Clock clock) {
        this(tournament, history, user, clock, null);
    }

    /** @param chosenDifficulty la dificultad que eligió el usuario para este torneo (null = no eligió) */
    public RecordingTournament(ITournament tournament, IMatchHistoryRepository history, User user, Clock clock,
                               Difficulty chosenDifficulty) {
        this.tournament = tournament;
        this.history = history;
        this.user = user;
        this.clock = clock;
        this.chosenDifficulty = chosenDifficulty;
    }

    @Override
    public void start() {
        userMovesThisRound = 0;
        tournament.start();
    }

    @Override
    public TurnReport play(Position from, Position to) {
        return recording(() -> tournament.play(from, to));
    }

    @Override
    public TurnReport playUserMove(Position from, Position to) {
        return recording(() -> tournament.playUserMove(from, to));
    }

    @Override
    public TurnReport playOpponentMove() {
        return recording(tournament::playOpponentMove);
    }

    /**
     * Ejecuta el turno y, si con él terminó la ronda, la guarda. Los datos de la
     * ronda se leen ANTES de jugar: si se gana, el torneo ya pasa al rival siguiente.
     */
    private TurnReport recording(Supplier<TurnReport> turn) {
        HistoricalPlayer rival = tournament.getCurrentRival();
        int round = tournament.getCurrentRound();

        TurnReport report = turn.get();

        if (report.userResult() != null && report.userResult().isSuccess()) {
            userMovesThisRound++;
        }
        if (report.outcome().isFinished()) {
            history.save(new RoundRecord(user.username(), rival.name(), round, tournament.getTotalRounds(),
                    report.outcome(), userMovesThisRound, LocalDateTime.now(clock), chosenDifficulty));
            userMovesThisRound = 0;
        }
        return report;
    }

    @Override public boolean isAwaitingOpponent() { return tournament.isAwaitingOpponent(); }
    @Override public int getCurrentRound() { return tournament.getCurrentRound(); }
    @Override public int getTotalRounds() { return tournament.getTotalRounds(); }
    @Override public HistoricalPlayer getCurrentRival() { return tournament.getCurrentRival(); }
    @Override public List<HistoricalPlayer> getRivals() { return tournament.getRivals(); }
    @Override public BoardView getBoard() { return tournament.getBoard(); }
    @Override public GameStatus getGameStatus() { return tournament.getGameStatus(); }
    @Override public Color getUserColor() { return tournament.getUserColor(); }
    @Override public Set<Position> legalMoves(Position origin) { return tournament.legalMoves(origin); }
    @Override public TournamentStatus getStatus() { return tournament.getStatus(); }
}
