package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.ai.IMoveChooser;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;

import java.util.List;
import java.util.Set;

/**
 * Campeonato tipo escalera: un rival por ronda, en orden de dificultad.
 *
 * Reglas (decisiones de diseño, ver docs/CAMBIOS_TORNEO.md):
 *  - ganar la ronda (jaque mate al rival) pasa a la siguiente; ganar la última = CHAMPION;
 *  - perder (jaque mate en contra) = ELIMINATED;
 *  - tablas (rey ahogado) = se repite la ronda contra el mismo rival, con una partida nueva.
 *
 * Todas sus dependencias entran por constructor: los rivales, la fábrica de
 * partidas (cada ronda es una partida nueva) y la fábrica de rivales (la IA).
 */
public class Tournament implements ITournament {

    private final List<HistoricalPlayer> rivals;
    private final IGameFactory gameFactory;
    private final IOpponentFactory opponentFactory;
    private final Color userColor;

    private int roundIndex;           // 0 = primera ronda
    private IGame game;               // partida de la ronda actual
    private IMoveChooser opponent;    // IA del rival actual
    private TournamentStatus status = TournamentStatus.NOT_STARTED;

    public Tournament(List<HistoricalPlayer> rivals,
                      IGameFactory gameFactory,
                      IOpponentFactory opponentFactory,
                      Color userColor) {
        if (rivals.isEmpty()) {
            throw new IllegalArgumentException("El torneo necesita al menos un rival");
        }
        this.rivals = List.copyOf(rivals);
        this.gameFactory = gameFactory;
        this.opponentFactory = opponentFactory;
        this.userColor = userColor;
    }

    @Override
    public void start() {
        roundIndex = 0;
        status = TournamentStatus.IN_PROGRESS;
        startRound();
    }

    @Override
    public TurnReport play(Position from, Position to) {
        TurnReport userTurn = playUserMove(from, to);
        if (!isAwaitingOpponent()) {
            return userTurn; // jugada inválida, o la ronda terminó con la jugada del usuario
        }
        TurnReport opponentTurn = playOpponentMove();
        return new TurnReport(userTurn.userResult(), opponentTurn.aiReply(), opponentTurn.outcome());
    }

    @Override
    public TurnReport playUserMove(Position from, Position to) {
        if (status != TournamentStatus.IN_PROGRESS) {
            return new TurnReport(MoveResult.GAME_OVER, null, RoundOutcome.ONGOING);
        }
        if (isAwaitingOpponent()) {
            return new TurnReport(MoveResult.OPPONENT_PIECE, null, RoundOutcome.ONGOING);
        }
        MoveResult result = game.move(from, to);
        if (!result.isSuccess()) {
            return new TurnReport(result, null, RoundOutcome.ONGOING);
        }
        RoundOutcome outcome = currentOutcome();
        resolve(outcome);
        return new TurnReport(result, null, outcome);
    }

    @Override
    public TurnReport playOpponentMove() {
        if (!isAwaitingOpponent()) {
            throw new IllegalStateException("No es el turno de la IA");
        }
        Move reply = playOpponent();
        RoundOutcome outcome = currentOutcome();
        resolve(outcome);
        return new TurnReport(null, reply, outcome);
    }

    @Override
    public boolean isAwaitingOpponent() {
        return status == TournamentStatus.IN_PROGRESS && game.getTurn() != userColor;
    }

    /** Partida nueva contra el rival de la ronda actual. Si el usuario juega con negras, la IA abre. */
    private void startRound() {
        game = gameFactory.create();
        opponent = opponentFactory.create(getCurrentRival());
        if (game.getTurn() != userColor && !game.getStatus().isOver()) {
            playOpponent();
        }
    }

    private Move playOpponent() {
        Move reply = opponent.chooseMove(game);
        MoveResult result = game.move(reply.from(), reply.to());
        if (!result.isSuccess()) {
            throw new IllegalStateException("El rival eligió una jugada ilegal: " + reply);
        }
        return reply;
    }

    /** El que queda en jaque mate es el que tiene el turno. */
    private RoundOutcome currentOutcome() {
        GameStatus gameStatus = game.getStatus();
        if (gameStatus == GameStatus.CHECKMATE) {
            return game.getTurn() == userColor ? RoundOutcome.LOST : RoundOutcome.WON;
        }
        if (gameStatus == GameStatus.STALEMATE) {
            return RoundOutcome.DRAWN;
        }
        return RoundOutcome.ONGOING;
    }

    private void resolve(RoundOutcome outcome) {
        switch (outcome) {
            case WON -> {
                if (roundIndex == rivals.size() - 1) {
                    status = TournamentStatus.CHAMPION;
                } else {
                    roundIndex++;
                    startRound();
                }
            }
            case LOST -> status = TournamentStatus.ELIMINATED;
            case DRAWN -> startRound();
            case ONGOING -> { }
        }
    }

    @Override public int getCurrentRound() { return roundIndex + 1; }
    @Override public int getTotalRounds() { return rivals.size(); }
    @Override public HistoricalPlayer getCurrentRival() { return rivals.get(roundIndex); }
    @Override public List<HistoricalPlayer> getRivals() { return rivals; }
    @Override public TournamentStatus getStatus() { return status; }
    @Override public Color getUserColor() { return userColor; }

    @Override
    public BoardView getBoard() {
        return currentGame().getBoard();
    }

    @Override
    public GameStatus getGameStatus() {
        return currentGame().getStatus();
    }

    @Override
    public Set<Position> legalMoves(Position origin) {
        return currentGame().legalMoves(origin);
    }

    private IGame currentGame() {
        if (game == null) {
            throw new IllegalStateException("El torneo no empezó: llamar a start()");
        }
        return game;
    }
}
