package ar.edu.uade.chess.core.history;

/** Totales del historial de un usuario. */
public record HistorySummary(int roundsPlayed, int wins, int losses, int draws, int titles) {
}
