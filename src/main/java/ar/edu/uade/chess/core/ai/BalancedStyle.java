package ar.edu.uade.chess.core.ai;

/**
 * Mezcla los otros dos estilos por COMPOSICIÓN: suma lo que opina el agresivo
 * y lo que opina el defensivo. Así una captura que deja una pieza colgada
 * vale menos que la misma captura hecha con seguridad.
 */
public final class BalancedStyle implements IPlayStyle {

    private final IPlayStyle aggressive = new AggressiveStyle();
    private final IPlayStyle defensive = new DefensiveStyle();

    @Override
    public int score(MoveFacts facts) {
        // Cada uno ya suma el bonus de mate: se descuenta una vez para no contarlo doble.
        int score = aggressive.score(facts) + defensive.score(facts);
        return facts.givesCheckmate() ? score - CHECKMATE_BONUS : score;
    }

    @Override
    public String getDisplayName() { return "Equilibrado"; }

    @Override
    public String getDescription() { return "Captura cuando es seguro: pesa lo que gana contra lo que arriesga."; }
}
