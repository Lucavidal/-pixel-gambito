package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;

import java.util.List;
import java.util.Set;

/**
 * PUERTO DE ENTRADA del campeonato tipo escalera contra la IA.
 *
 * Es lo único que un adaptador necesita conocer para jugar el torneo.
 * A propósito NO expone deshacer/rehacer ni la partida (IGame): en el torneo
 * el usuario no puede deshacer la respuesta de la IA.
 */
public interface ITournament {

    /** Empieza (o reinicia) el torneo en la ronda 1. */
    void start();

    /** Ronda actual, empezando en 1. */
    int getCurrentRound();

    int getTotalRounds();

    HistoricalPlayer getCurrentRival();

    /** Todos los rivales, en orden de ronda (para mostrar la "escalera" completa). */
    List<HistoricalPlayer> getRivals();

    /** Aplica la jugada del usuario y, si fue válida y la ronda sigue, la respuesta de la IA. */
    TurnReport play(Position from, Position to);

    /**
     * Primera mitad de play(): aplica SOLO la jugada del usuario. Si fue válida y la
     * ronda sigue, el torneo queda esperando la respuesta de la IA (isAwaitingOpponent()).
     * Sirve para que un adaptador muestre la jugada del usuario antes de la de la IA
     * (por ejemplo, con una demora de "pensando..."). La demora es cosa del adaptador.
     */
    TurnReport playUserMove(Position from, Position to);

    /** Segunda mitad de play(): la IA responde. Solo se puede llamar si isAwaitingOpponent(). */
    TurnReport playOpponentMove();

    /** ¿El usuario ya jugó y falta la respuesta de la IA? */
    boolean isAwaitingOpponent();

    /** Solo lectura: el tablero de la partida de la ronda actual. */
    BoardView getBoard();

    GameStatus getGameStatus();

    Color getUserColor();

    /** Movimientos legales de la pieza en esa casilla, en la partida actual. */
    Set<Position> legalMoves(Position origin);

    TournamentStatus getStatus();
}
