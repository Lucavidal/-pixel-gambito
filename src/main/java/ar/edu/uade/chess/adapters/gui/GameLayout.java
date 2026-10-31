package ar.edu.uade.chess.adapters.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * La disposición común de las ventanas de juego: el tablero manda (ocupa todo el
 * espacio libre, con una línea de ayuda abajo) y a la derecha una sola hoja con los
 * jugadores, la planilla y las acciones, pegada al borde de la ventana.
 */
final class GameLayout {

    static final int SIDE_WIDTH = 340;

    private GameLayout() { }

    static JPanel content(BoardPanel board, String hint, JComponent side) {
        JLabel help = Theme.label(hint, Theme.SMALL, Theme.TEXT_MUTED);
        help.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        JPanel boardColumn = new JPanel(new BorderLayout(0, 4));
        boardColumn.setOpaque(false);
        boardColumn.setBorder(Theme.padding(14, 14));
        boardColumn.add(board, BorderLayout.CENTER);
        boardColumn.add(help, BorderLayout.SOUTH);

        side.setOpaque(true);
        side.setBackground(Theme.CARD);
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.BORDER), Theme.padding(18, 18)));
        side.setPreferredSize(new Dimension(SIDE_WIDTH, side.getPreferredSize().height));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BACKGROUND);
        content.add(boardColumn, BorderLayout.CENTER);
        content.add(side, BorderLayout.EAST);
        return content;
    }

    /** Panel transparente con BorderLayout, para armar la hoja lateral por partes. */
    static JPanel stack(int gap) {
        JPanel panel = new JPanel(new BorderLayout(0, gap));
        panel.setOpaque(false);
        return panel;
    }
}
