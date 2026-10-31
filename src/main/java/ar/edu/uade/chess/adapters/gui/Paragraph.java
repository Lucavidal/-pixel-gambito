package ar.edu.uade.chess.adapters.gui;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Texto de varias líneas que corta por palabras al ancho que tiene de verdad.
 *
 * Reemplaza al truco de "<html><body style='width:..px'>" en JLabel: el motor HTML de
 * Swing escala esos píxeles según el DPI de la pantalla y el texto terminaba cortado
 * a la derecha. Antes de que la ventana se acomode usa el ancho sugerido, así pack()
 * ya le reserva la altura justa.
 */
public class Paragraph extends JTextArea {

    private final int suggestedWidth;

    public Paragraph(String text, Font font, Color color, int suggestedWidth) {
        super(text);
        this.suggestedWidth = suggestedWidth;
        setFont(font);
        setForeground(color);
        setOpaque(false);
        setEditable(false);
        setFocusable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(BorderFactory.createEmptyBorder());
        setHighlighter(null);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = getWidth() > 0 ? getWidth() : suggestedWidth;
        if (getWidth() == 0) {
            setSize(width, Short.MAX_VALUE); // calcula las líneas con ese ancho
        }
        return new Dimension(width, super.getPreferredSize().height);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        revalidate(); // otra cantidad de líneas = otra altura
    }
}
