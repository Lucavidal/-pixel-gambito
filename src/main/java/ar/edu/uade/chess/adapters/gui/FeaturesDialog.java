package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.tournament.HistoricalPlayer;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/** "Qué incluye el juego": todo lo implementado, explicado para quien lo usa (y para la defensa). */
public class FeaturesDialog {

    private final List<HistoricalPlayer> roster;

    public FeaturesDialog(List<HistoricalPlayer> roster) {
        this.roster = roster;
    }

    public void show(Component parent) {
        StringBuilder rivals = new StringBuilder();
        for (int i = 0; i < roster.size(); i++) {
            HistoricalPlayer r = roster.get(i);
            rivals.append("<li><b>Ronda ").append(i + 1).append(": ").append(r.name()).append("</b> — ")
                    .append(r.style().getDisplayName()).append(", ").append(r.difficulty().getDisplayName().toLowerCase())
                    .append(". ").append(r.style().getDescription()).append("</li>");
        }
        String html = """
                <html><body style='font-family:%2$s; font-size:11px; color:%3$s; width:420px'>
                <h2 style='font-family:%4$s; font-weight:normal; color:%5$s'>Qué incluye el juego</h2>

                <h3>Reglas del ajedrez</h3>
                <ul>
                  <li>Tablero de 8×8 con las 6 piezas y su movimiento: peón (avance doble inicial y captura en diagonal),
                      torre, caballo, alfil, dama y rey.</li>
                  <li>Turnos alternados, capturas y rechazo de cualquier movimiento inválido, con el motivo.</li>
                  <li><b>Jaque</b> (el rey se marca en rojo), <b>jaque mate</b> y <b>rey ahogado</b> (tablas).</li>
                  <li>Nunca se permite una jugada que deje a tu propio rey en jaque.</li>
                  <li>No incluye enroque, captura al paso ni promoción del peón.</li>
                </ul>

                <h3>Partida libre (dos personas)</h3>
                <ul>
                  <li>Clic en una pieza: se marcan sus destinos legales (punto = casilla libre, anillo = captura).</li>
                  <li>También se juega con el teclado: flechas para recorrer el tablero, Enter o Espacio para elegir.</li>
                  <li><b>Deshacer</b> y <b>rehacer</b> jugadas (también con Ctrl+Z / Ctrl+Y).</li>
                  <li>Planilla de jugadas, piezas capturadas y la última jugada resaltada.</li>
                </ul>

                <h3>Torneo escalera contra la IA</h3>
                <ul>
                  <li>Jugás con blancas, una partida por ronda, con rivales cada vez más difíciles:
                      <ul>%1$s</ul></li>
                  <li>Jaque mate a favor: pasás de ronda. En contra: quedás eliminado. Ahogado: se repite la ronda.
                      Si ganás la última, sos <b>campeón</b>.</li>
                  <li>La IA solo elige jugadas legales. El <b>estilo</b> decide qué jugada le parece mejor; la
                      <b>dificultad</b> decide cuántas veces se equivoca a propósito.</li>
                  <li>En el torneo no se puede deshacer: la IA ya respondió.</li>
                </ul>

                <h3>Cuenta e historial</h3>
                <ul>
                  <li>Cada ronda terminada se guarda en tu cuenta (base de datos SQLite): rival, resultado, jugadas y fecha.</li>
                  <li>"Mi historial" muestra tus totales y las últimas rondas.</li>
                  <li>Las contraseñas se guardan cifradas con hash (PBKDF2), nunca en texto plano.</li>
                </ul>
                </body></html>""".formatted(rivals, Theme.BODY.getFamily(), hex(Theme.TEXT),
                Theme.TITLE.getFamily(), hex(Theme.ACCENT));
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setBackground(Theme.CARD);
        pane.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setPreferredSize(new Dimension(600, 480));
        JOptionPane.showMessageDialog(parent, scroll, "Qué incluye el juego", JOptionPane.PLAIN_MESSAGE);
    }

    /** El color como "#RRGGBB", para usarlo en el HTML. */
    private static String hex(java.awt.Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }
}
