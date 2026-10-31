package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.tournament.HistoricalPlayer;
import ar.edu.uade.chess.core.tournament.ITournament;
import ar.edu.uade.chess.core.tournament.TournamentStatus;
import ar.edu.uade.chess.core.tournament.TurnReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Collectors;

/**
 * ADAPTADOR DE ENTRADA del torneo: traduce lo que escribe una persona en la
 * consola a llamadas al puerto ITournament, y muestra ronda, rival y resultados.
 *
 * Depende del núcleo (ITournament); el núcleo no sabe que esta clase existe.
 * Reutiliza la notación y el dibujo del tablero del adaptador de partida libre.
 */
public class TournamentConsoleAdapter {

    private final ITournament tournament;
    private final BufferedReader input;
    private final PrintStream output;
    private final AlgebraicNotation notation;
    private final TextRenderer renderer;
    private final int opponentDelayMillis;
    private final HistoryPrinter historyPrinter; // null = sin historial (por ejemplo, en los tests)

    /** Sin demora ni historial: la IA responde en el acto (es lo que usan los tests). */
    public TournamentConsoleAdapter(ITournament tournament, BufferedReader input, PrintStream output,
                                    AlgebraicNotation notation, TextRenderer renderer) {
        this(tournament, input, output, notation, renderer, 0, null);
    }

    /**
     * @param opponentDelayMillis cuánto "piensa" la IA antes de mostrar su jugada.
     *                            Es solo presentación: el núcleo no espera nada.
     * @param historyPrinter      muestra el historial con el comando "historial" (puede ser null)
     */
    public TournamentConsoleAdapter(ITournament tournament, BufferedReader input, PrintStream output,
                                    AlgebraicNotation notation, TextRenderer renderer, int opponentDelayMillis,
                                    HistoryPrinter historyPrinter) {
        this.tournament = tournament;
        this.input = input;
        this.output = output;
        this.notation = notation;
        this.renderer = renderer;
        this.opponentDelayMillis = opponentDelayMillis;
        this.historyPrinter = historyPrinter;
    }

    private void showHistory() {
        if (historyPrinter == null) {
            output.println("El historial no está disponible.");
        } else {
            historyPrinter.print(output);
        }
    }

    private void pause() {
        try {
            Thread.sleep(opponentDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void play() throws IOException {
        output.println("=== Torneo escalera ===");
        showHelp();
        tournament.start();
        announceRound();
        while (true) {
            if (tournament.getStatus().isOver()) {
                announceEnd();
                return;
            }
            showBoard();
            output.print("Ronda " + tournament.getCurrentRound() + " > ");
            String line = input.readLine();
            if (line == null || line.trim().equalsIgnoreCase("salir")) {
                output.println("Abandonaste el torneo.");
                return;
            }
            process(line.trim());
        }
    }

    private void process(String line) {
        String[] parts = line.toLowerCase().split("\\s+");
        try {
            switch (parts[0]) {
                case "ayuda" -> showHelp();
                case "rival" -> showRival();
                case "historial" -> showHistory();
                case "ver" -> showLegalMoves(parts);
                default -> tryMove(parts);
            }
        } catch (IllegalArgumentException e) {
            output.println("Entrada inválida: " + e.getMessage());
        }
    }

    private void tryMove(String[] parts) {
        if (parts.length != 2) {
            output.println("Formato: <origen> <destino>, por ejemplo: e2 e4  (o escribí 'ayuda')");
            return;
        }
        // Se guarda antes de jugar: si la ronda termina, el torneo ya pasa al rival siguiente.
        HistoricalPlayer rival = tournament.getCurrentRival();
        TurnReport report = tournament.playUserMove(notation.toPosition(parts[0]), notation.toPosition(parts[1]));
        output.println(report.userResult().getMessage());
        if (tournament.isAwaitingOpponent()) {
            if (opponentDelayMillis > 0) {
                output.println(rival.name() + " está pensando...");
                pause();
            }
            report = tournament.playOpponentMove();
            output.println(rival.name() + " juega " + text(report.aiReply()) + ".");
        }
        switch (report.outcome()) {
            case WON -> {
                output.println("¡JAQUE MATE! Le ganaste a " + rival.name() + ".");
                if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
                    announceRound();
                }
            }
            case LOST -> output.println("¡JAQUE MATE! " + rival.name() + " te ganó.");
            case DRAWN -> {
                output.println("Rey ahogado: tablas. Se repite la ronda " + tournament.getCurrentRound() + ".");
                announceRound();
            }
            case ONGOING -> { }
        }
    }

    private void announceRound() {
        output.println();
        output.println("--- Ronda " + tournament.getCurrentRound() + " de " + tournament.getTotalRounds() + " ---");
        showRival();
    }

    private void showRival() {
        HistoricalPlayer rival = tournament.getCurrentRival();
        output.println("Rival: " + rival.name() + " | Estilo: " + rival.style().getDisplayName()
                + " | Dificultad: " + rival.difficulty().getDisplayName());
        output.println(rival.description());
    }

    private void announceEnd() {
        output.println();
        output.println("Torneo terminado: " + tournament.getStatus().getDisplayName() + ".");
        if (tournament.getStatus() == TournamentStatus.CHAMPION) {
            output.println("¡Ganaste las " + tournament.getTotalRounds() + " rondas! Sos el campeón.");
        } else {
            output.println("Quedaste eliminado en la ronda " + tournament.getCurrentRound() + ".");
        }
    }

    private void showLegalMoves(String[] parts) {
        if (parts.length != 2) {
            output.println("Formato: ver <casilla>, por ejemplo: ver g1");
            return;
        }
        var destinations = tournament.legalMoves(notation.toPosition(parts[1]));
        output.println(destinations.isEmpty()
                ? "Esa casilla no tiene movimientos legales."
                : "Destinos legales: " + destinations.stream().map(notation::toText).collect(Collectors.joining(" ")));
    }

    private void showBoard() {
        output.println();
        output.print(renderer.render(tournament.getBoard()));
        if (tournament.getGameStatus() == GameStatus.CHECK) {
            output.println("¡Estás en JAQUE!");
        }
    }

    private void showHelp() {
        output.println("Comandos: 'e2 e4' mueve | 'ver g1' muestra destinos | 'rival' | 'historial' | 'ayuda' | 'salir'");
        output.println("Jugás con " + (tournament.getUserColor() == Color.WHITE
                ? "blancas (MAYÚSCULA)" : "negras (minúscula)") + ". P=peón T=torre C=caballo A=alfil D=dama R=rey");
    }

    private String text(Move move) {
        return notation.toText(move.from()) + " " + notation.toText(move.to());
    }
}
