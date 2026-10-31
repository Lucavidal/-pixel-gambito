package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.model.Piece;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;
import java.util.stream.Collectors;

/**
 * La fila de un jugador al costado del tablero: nombre, un aviso corto ("Juega",
 * "En jaque", "Ganó") y las piezas que capturó. Las negras van arriba y las blancas
 * abajo, igual que en el tablero.
 *
 * El bando que tiene que mover se pinta con el verde del paño, como el lado del
 * reloj de ajedrez que está corriendo: se ve de quién es el turno sin leer nada.
 */
public class PlayerStrip extends JPanel {

    private static final int RADIUS = 10;
    private static final Font GLYPHS = new Font(Font.SANS_SERIF, Font.PLAIN, 20);

    private final PieceGlyphs glyphs;
    private String name;
    private final JLabel nameLabel = new JLabel();
    private final JLabel noteLabel = new JLabel(" ");
    private final JLabel capturedLabel = new JLabel();
    private boolean active;

    public PlayerStrip(PieceGlyphs glyphs, String name) {
        super(new BorderLayout(10, 0));
        this.glyphs = glyphs;
        this.name = name;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        nameLabel.setText(name);
        nameLabel.setFont(Theme.SUBTITLE);
        noteLabel.setFont(Theme.SMALL);
        capturedLabel.setFont(GLYPHS);
        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.setOpaque(false);
        texts.add(nameLabel);
        texts.add(noteLabel);
        add(texts, BorderLayout.CENTER);
        add(capturedLabel, BorderLayout.EAST);
        setActive(false);
        setCaptured(List.of());
    }

    /** Cambia el nombre que se muestra (en el torneo, el rival cambia en cada ronda). */
    public void setPlayerName(String name) {
        this.name = name;
        nameLabel.setText(name);
    }

    /** true = es su turno (fila verde). */
    public void setActive(boolean active) {
        this.active = active;
        nameLabel.setForeground(active ? Theme.ON_ACCENT : Theme.TEXT);
        capturedLabel.setForeground(active ? Theme.ON_ACCENT : Theme.TEXT);
        repaint();
    }

    /** Aviso corto debajo del nombre; color null = el normal de la fila. */
    public void setNote(String text, Color color) {
        noteLabel.setText(text == null || text.isEmpty() ? " " : text);
        noteLabel.setForeground(color != null ? color : active ? new Color(0xC9DCCF) : Theme.TEXT_MUTED);
        getAccessibleContext().setAccessibleName(name + (text == null || text.isEmpty() ? "" : ", " + text));
    }

    /** Piezas que este jugador le capturó al rival. */
    public void setCaptured(List<Piece> pieces) {
        capturedLabel.setText(pieces.stream().map(glyphs::of).collect(Collectors.joining()));
        capturedLabel.getAccessibleContext().setAccessibleDescription(pieces.isEmpty() ? "No capturó piezas"
                : "Capturó: " + pieces.stream().map(Piece::getName).collect(Collectors.joining(", ")));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (active) {
            g.setColor(Theme.ACCENT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
        } else {
            g.setColor(Theme.BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
