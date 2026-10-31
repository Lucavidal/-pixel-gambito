package ar.edu.uade.chess.core.tournament;

import ar.edu.uade.chess.core.ai.AggressiveStyle;
import ar.edu.uade.chess.core.ai.BalancedStyle;
import ar.edu.uade.chess.core.ai.DefensiveStyle;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.ai.IPlayStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Patrón FACTORY: el único lugar donde se decide quiénes son los rivales del torneo.
 *
 * Hay dos escaleras:
 *  - createRoster(): la clásica de 3 rondas (fácil, media, difícil). La usan la consola y la ventana Swing.
 *  - createLadder(nivel): la de la web, donde el usuario elige desde qué dificultad empieza.
 *    Son LADDER_ROUNDS rondas que suben de dificultad: RIVALS_PER_LEVEL rivales del nivel elegido
 *    y después del siguiente; si ya no hay un nivel más, se completa con el último.
 *    (Fácil = 2 fáciles + 2 medios; Media = 2 medios + 2 difíciles; Difícil = 4 difíciles.)
 *    Los rivales de cada nivel se sortean entre los del plantel, así cada torneo es distinto.
 *
 * En los dos casos la lista va de menor a mayor dificultad: la cantidad de rondas es la cantidad
 * de rivales. Ni el torneo ni la IA se modifican para agregar rivales o cambiar la escalera.
 *
 * Los nombres son de ajedrecistas históricos reales; las descripciones son
 * neutrales a propósito (no se les atribuyen frases ni datos biográficos).
 * El estilo y la dificultad son una decisión de juego, no una afirmación sobre la persona.
 */
public final class HistoricalPlayerFactory {

    public static final int DEFAULT_ROUNDS = 3;

    /** Rondas de la escalera que se arma eligiendo la dificultad inicial. */
    public static final int LADDER_ROUNDS = 4;

    /** Cuántos rivales de cada nivel entran en esa escalera antes de pasar al siguiente. */
    public static final int RIVALS_PER_LEVEL = 2;

    /** Los rivales de la escalera clásica (salen del plantel completo). */
    private static final List<String> CLASSIC = List.of("Tigran Petrosian", "José Raúl Capablanca", "Mikhail Tal");

    /**
     * El plantel se arma una sola vez: los estilos no tienen estado, y así el mismo rival
     * es el mismo objeto en todas las escaleras (los records se comparan por sus campos).
     */
    private static final List<HistoricalPlayer> ALL = Stream.of(
                        player("Tigran Petrosian", new DefensiveStyle(), Difficulty.EASY),
                        player("Paul Morphy", new AggressiveStyle(), Difficulty.EASY),
                        player("Max Euwe", new BalancedStyle(), Difficulty.EASY),
                        player("Wilhelm Steinitz", new DefensiveStyle(), Difficulty.EASY),
                        player("José Raúl Capablanca", new BalancedStyle(), Difficulty.MEDIUM),
                        player("Mikhail Botvinnik", new DefensiveStyle(), Difficulty.MEDIUM),
                        player("David Bronstein", new AggressiveStyle(), Difficulty.MEDIUM),
                        player("Vasily Smyslov", new BalancedStyle(), Difficulty.MEDIUM),
                        player("Mikhail Tal", new AggressiveStyle(), Difficulty.HARD),
                        player("Bobby Fischer", new BalancedStyle(), Difficulty.HARD),
                        player("Alexander Alekhine", new AggressiveStyle(), Difficulty.HARD),
                        player("Boris Spassky", new DefensiveStyle(), Difficulty.HARD))
                .sorted(Comparator.comparing(HistoricalPlayer::difficulty))
                .toList();

    private HistoricalPlayerFactory() { }

    /** La escalera clásica de 3 rondas, una por dificultad (consola y ventana Swing). */
    public static List<HistoricalPlayer> createRoster() {
        return ALL.stream().filter(p -> CLASSIC.contains(p.name())).toList();
    }

    /** Todo el plantel: 4 rivales por nivel, ordenados de menor a mayor dificultad. */
    public static List<HistoricalPlayer> allPlayers() {
        return ALL;
    }

    /**
     * Escalera que empieza en la dificultad elegida y sube (ver el comentario de la clase).
     * El Random entra por parámetro para que los tests sean deterministas.
     */
    public static List<HistoricalPlayer> createLadder(Difficulty start, Random random) {
        Difficulty[] levels = Difficulty.values();   // en orden de dificultad
        List<HistoricalPlayer> ladder = new ArrayList<>();
        for (int i = start.ordinal(); i < levels.length && ladder.size() < LADDER_ROUNDS; i++) {
            boolean lastLevel = i == levels.length - 1;
            int missing = LADDER_ROUNDS - ladder.size();
            int take = lastLevel ? missing : Math.min(RIVALS_PER_LEVEL, missing);
            List<HistoricalPlayer> candidates = new ArrayList<>(playersOf(levels[i]));
            Collections.shuffle(candidates, random);
            ladder.addAll(candidates.subList(0, Math.min(take, candidates.size())));
        }
        return List.copyOf(ladder);
    }

    private static List<HistoricalPlayer> playersOf(Difficulty difficulty) {
        return ALL.stream().filter(p -> p.difficulty() == difficulty).toList();
    }

    private static HistoricalPlayer player(String name, IPlayStyle style, Difficulty difficulty) {
        String description = "Ajedrecista histórico. En este torneo juega con estilo "
                + style.getDisplayName().toLowerCase(Locale.ROOT) + ".";
        return new HistoricalPlayer(name, description, style, difficulty);
    }
}
