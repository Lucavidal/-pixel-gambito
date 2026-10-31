package ar.edu.uade.chess.core.tournament;

/** Cómo quedó la ronda actual después de una jugada, desde el punto de vista del usuario. */
public enum RoundOutcome {
    ONGOING,
    WON,
    LOST,
    DRAWN;

    public boolean isFinished() {
        return this != ONGOING;
    }
}
