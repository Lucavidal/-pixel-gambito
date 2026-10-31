package ar.edu.uade.chess.core.tournament;

/**
 * Situación del torneo.
 *
 * Es un enum y NO el patrón State, por el mismo motivo que GameStatus: los
 * estados no tienen comportamiento propio distinto (solo informan) y las
 * transiciones las decide Tournament según cómo termina cada partida.
 * Ver docs/CAMBIOS_TORNEO.md.
 */
public enum TournamentStatus {
    NOT_STARTED("Sin empezar"),
    IN_PROGRESS("En curso"),
    ELIMINATED("Eliminado"),
    CHAMPION("Campeón");

    private final String displayName;

    TournamentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public boolean isOver() {
        return this == ELIMINATED || this == CHAMPION;
    }
}
