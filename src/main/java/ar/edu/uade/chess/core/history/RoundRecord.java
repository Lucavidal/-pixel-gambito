package ar.edu.uade.chess.core.history;

import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.tournament.RoundOutcome;

import java.time.LocalDateTime;

/**
 * Una ronda del torneo ya terminada, tal como se guarda en el historial.
 * Es un dato del núcleo: no tiene anotaciones ni nada de la base de datos.
 *
 * @param username    quién la jugó
 * @param rivalName   contra quién
 * @param round       número de ronda (empieza en 1)
 * @param totalRounds cantidad de rondas del torneo (para saber si era la final)
 * @param outcome     WON, LOST o DRAWN
 * @param userMoves   cuántas jugadas hizo el usuario en esa partida
 * @param playedAt    cuándo terminó
 * @param difficulty  la dificultad que eligió el usuario al empezar el torneo; null si no la eligió
 *                    (la escalera clásica de consola y ventana) o si la ronda se guardó antes de que existiera
 */
public record RoundRecord(String username,
                          String rivalName,
                          int round,
                          int totalRounds,
                          RoundOutcome outcome,
                          int userMoves,
                          LocalDateTime playedAt,
                          Difficulty difficulty) {

    /** Una ronda sin dificultad elegida. */
    public RoundRecord(String username, String rivalName, int round, int totalRounds, RoundOutcome outcome,
                       int userMoves, LocalDateTime playedAt) {
        this(username, rivalName, round, totalRounds, outcome, userMoves, playedAt, null);
    }

    /** Ganar la última ronda = salir campeón. */
    public boolean wonTheTournament() {
        return outcome == RoundOutcome.WON && round == totalRounds;
    }
}
