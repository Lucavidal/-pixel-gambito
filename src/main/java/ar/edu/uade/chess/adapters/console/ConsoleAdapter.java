package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.stream.Collectors;

/**
 * ADAPTADOR DE ENTRADA: traduce lo que escribe una persona en la consola
 * a llamadas al puerto IGame, y muestra el resultado.
 *
 * Depende del núcleo (IGame); el núcleo no sabe que esta clase existe.
 * Recibe la entrada y la salida por constructor, así puede probarse con
 * texto simulado en vez de un teclado real.
 *
 * Los comandos que escribe el jugador ("deshacer", "ver", ...) y los textos
 * que se muestran quedan en español: son la interfaz de usuario, no el código.
 */
public class ConsoleAdapter {

    private final IGame game;
    private final BufferedReader input;
    private final PrintStream output;
    private final AlgebraicNotation notation;
    private final TextRenderer renderer;

    public ConsoleAdapter(IGame game, BufferedReader input, PrintStream output,
                          AlgebraicNotation notation, TextRenderer renderer) {
        this.game = game;
        this.input = input;
        this.output = output;
        this.notation = notation;
        this.renderer = renderer;
    }

    public void play() throws IOException {
        output.println("=== Ajedrez ===");
        showHelp();
        while (true) {
            showBoard();
            if (game.getStatus().isOver()) {
                announceEnd();
                return;
            }
            output.print(name(game.getTurn()) + " > ");
            String line = input.readLine();
            if (line == null || line.trim().equalsIgnoreCase("salir")) {
                output.println("Partida finalizada.");
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
                case "deshacer" -> output.println(game.undo() ? "Jugada deshecha." : "No hay jugadas para deshacer.");
                case "rehacer" -> output.println(game.redo() ? "Jugada rehecha." : "No hay jugadas para rehacer.");
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
        Position from = notation.toPosition(parts[0]);
        Position to = notation.toPosition(parts[1]);
        MoveResult result = game.move(from, to);
        output.println(result.getMessage());
    }

    private void showLegalMoves(String[] parts) {
        if (parts.length != 2) {
            output.println("Formato: ver <casilla>, por ejemplo: ver g1");
            return;
        }
        var destinations = game.legalMoves(notation.toPosition(parts[1]));
        output.println(destinations.isEmpty()
                ? "Esa casilla no tiene movimientos legales."
                : "Destinos legales: " + destinations.stream().map(notation::toText).collect(Collectors.joining(" ")));
    }

    private void showBoard() {
        output.println();
        output.print(renderer.render(game.getBoard()));
        if (game.getStatus() == GameStatus.CHECK) {
            output.println("¡" + name(game.getTurn()) + " está en JAQUE!");
        }
    }

    private void announceEnd() {
        if (game.getStatus() == GameStatus.CHECKMATE) {
            output.println("¡JAQUE MATE! Ganan " + name(game.getTurn().opposite()) + ".");
        } else {
            output.println("Rey ahogado: la partida termina en tablas.");
        }
    }

    private void showHelp() {
        output.println("Comandos: 'e2 e4' mueve | 'ver g1' muestra destinos | 'deshacer' | 'rehacer' | 'ayuda' | 'salir'");
        output.println("Blancas en MAYÚSCULA, negras en minúscula. P=peón T=torre C=caballo A=alfil D=dama R=rey");
    }

    private String name(Color color) {
        return color == Color.WHITE ? "Blancas" : "Negras";
    }
}
