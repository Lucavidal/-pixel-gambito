package ar.edu.uade.chess.adapters.gui;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Botón dibujado a mano para que siga la paleta de la interfaz (los botones de
 * Windows son grises y no combinan). Tres tipos:
 *  - PRIMARY: relleno verde, la acción principal de la pantalla;
 *  - SECONDARY: contorno, para el resto de las acciones;
 *  - LINK: solo texto verde, para lo que es navegación secundaria.
 * Muestra hover, presionado, deshabilitado y el recuadro de foco (solo con teclado).
 */
public class FlatButton extends JButton {

    public enum Kind { PRIMARY, SECONDARY, LINK }

    private static final int RADIUS = 8;

    private final Kind kind;

    public FlatButton(String text, String tooltip, Kind kind) {
        super(text);
        this.kind = kind;
        setToolTipText(tooltip);
        setFont(kind == Kind.PRIMARY ? Theme.BODY_STRONG : Theme.BODY);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false); // el foco lo dibuja paintComponent, solo si se usa el teclado
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(kind == Kind.LINK ? BorderFactory.createEmptyBorder(6, 4, 6, 4)
                : BorderFactory.createEmptyBorder(9, 16, 9, 16));
        Theme.repaintOnFocus(this);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        var insets = getInsets();
        return new Dimension(metrics.stringWidth(getText()) + insets.left + insets.right,
                metrics.getHeight() + insets.top + insets.bottom);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ButtonModel model = getModel();
        boolean enabled = isEnabled();
        boolean pressed = enabled && model.isArmed() && model.isPressed();
        boolean hover = enabled && model.isRollover();
        int w = getWidth();
        int h = getHeight();

        Color text;
        switch (kind) {
            case PRIMARY -> {
                g.setColor(!enabled ? Theme.BORDER : pressed ? Theme.ACCENT_DARK.darker()
                        : hover ? Theme.ACCENT_DARK : Theme.ACCENT);
                g.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                text = enabled ? Theme.ON_ACCENT : Theme.TEXT_MUTED;
            }
            case SECONDARY -> {
                g.setColor(pressed ? Theme.BORDER : hover ? Theme.ACCENT_TINT : Theme.CARD);
                g.fillRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);
                g.setColor(enabled && (hover || pressed) ? Theme.ACCENT : Theme.BORDER);
                g.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);
                text = enabled ? Theme.TEXT : new Color(0x9AA59D);
            }
            default -> text = !enabled ? Theme.TEXT_MUTED : pressed ? Theme.ACCENT_DARK : Theme.ACCENT;
        }

        g.setFont(getFont());
        FontMetrics metrics = g.getFontMetrics();
        int textX = (w - metrics.stringWidth(getText())) / 2;
        int textY = (h - metrics.getHeight()) / 2 + metrics.getAscent();
        g.setColor(text);
        g.drawString(getText(), textX, textY);
        if (kind == Kind.LINK && hover) {
            g.fillRect(textX, textY + 2, metrics.stringWidth(getText()), 1); // subrayado al pasar el mouse
        }

        if (Theme.showsFocus(this)) {
            g.setColor(Theme.FOCUS);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(1, 1, w - 3, h - 3, RADIUS, RADIUS);
        }
        g.dispose();
    }
}
