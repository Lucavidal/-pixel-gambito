package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.MoveResult;

/**
 * Lo que pasó en un turno del torneo: la jugada del usuario y, si correspondía,
 * la respuesta de la IA.
 *
 * @param userResult resultado de la jugada del usuario (si no fue SUCCESS, la IA no respondió);
 *                   null en el reporte de playOpponentMove(), donde solo jugó la IA
 * @param aiReply    jugada de la IA, o null si no llegó a jugar (jugada inválida o la ronda terminó antes)
 * @param outcome    cómo quedó la ronda que se estaba jugando
 */
public record TurnReport(MoveResult userResult, Move aiReply, RoundOutcome outcome) {
}
