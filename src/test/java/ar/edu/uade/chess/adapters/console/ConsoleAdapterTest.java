package ar.edu.uade.chess.adapters.console;

import ar.edu.uade.chess.core.commands.CommandHistory;
import ar.edu.uade.chess.core.game.Game;
import ar.edu.uade.chess.core.rules.CheckDetector;
import ar.edu.uade.chess.core.rules.MoveValidator;
import ar.edu.uade.chess.core.setup.StandardSetup;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de INTEGRACIÓN: adaptador de consola + núcleo real, con la entrada
 * simulada por texto. No hace falta un teclado ni una ventana.
 */
class ConsoleAdapterTest {

    private String play(String moves) throws Exception {
        CheckDetector detector = new CheckDetector();
        Game game = new Game(new StandardSetup().createBoard(),
                new MoveValidator(detector), detector, new CommandHistory());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        new ConsoleAdapter(game, new BufferedReader(new StringReader(moves)), output,
                new AlgebraicNotation(), new TextRenderer()).play();

        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void foolsMateThroughConsole_AnnouncesCheckmate() throws Exception {
        String output = play("f2 f3\ne7 e5\ng2 g4\nd8 h4\n");

        assertTrue(output.contains("JAQUE MATE"));
        assertTrue(output.contains("Ganan Negras"));
    }

    @Test
    void invalidInput_DoesNotBreakTheProgram() throws Exception {
        String output = play("hola\nzz 99\ne2 e5\nsalir\n");

        assertTrue(output.contains("Formato"));
        assertTrue(output.contains("Entrada inválida") || output.contains("fuera del tablero"));
        assertTrue(output.contains("no puede moverse"));
        assertTrue(output.contains("Partida finalizada."));
    }
}
