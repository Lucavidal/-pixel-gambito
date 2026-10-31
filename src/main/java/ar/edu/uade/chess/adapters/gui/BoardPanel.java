package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Dibuja el tablero (solo lectura, vía BoardView) y avisa qué casilla se clickeó.
 *
 * Resalta: la pieza seleccionada, sus destinos legales (punto = casilla libre,
 * anillo = captura), la última jugada y el rey en jaque (en rojo).
 *
 * No asume 8x8: el tamaño de cada casilla se calcula con rows()/columns(), así
 * que un tablero de 10x10 se dibuja igual. La fila 0 (lado de las blancas) va abajo.
 */
public class BoardPanel extends JPanel {

    private static final int MARGIN = 20; // lugar para las letras y números de las coordenadas
    private static final int SLIDE_MILLIS = 240; // jugada de la IA: corta, para no hacer esperar
    private static final Color HOVER = new Color(255, 255, 255, 0x40);

    private final PieceGlyphs glyphs;
    private BoardView board;
    private Position selected;
    private Set<Position> highlighted = Set.of();
    private Move lastMove;
    private Position kingInCheck;
    private Position cursor; // casilla marcada para el teclado (flechas + Enter), o null
    private Position hovered; // casilla bajo el mouse, o null
    private Consumer<Position> clickListener = square -> { };
    private Predicate<Position> clickable = square -> false;

    // Jugada que se está deslizando (la de la IA), o null.
    private Move sliding;
    private String slidingGlyph;
    private String capturedGlyph; // pieza que se come, se desvanece mientras llega la otra
    private double slideProgress = 1;
    private Motion.Tween slide;

    public BoardPanel(PieceGlyphs glyphs) {
        this.glyphs = glyphs;
        setPreferredSize(new Dimension(600, 600));
        setBackground(Theme.BACKGROUND);
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Tablero");
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Position square = positionAt(e.getX(), e.getY());
                if (square != null) {
                    requestFocusInWindow();
                    moveCursorTo(square);
                    clickListener.accept(square);
                    updateHover(square);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) { updateHover(positionAt(e.getX(), e.getY())); }

            @Override
            public void mouseExited(MouseEvent e) { updateHover(null); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (cursor == null && board != null) {
                    moveCursorTo(new Position(0, board.columns() / 2)); // empieza cerca del rey blanco
                }
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) { repaint(); }
        });
        bindKeys();
    }

    /** Flechas: mover el cursor; Enter o Espacio: lo mismo que un clic en esa casilla. */
    private void bindKeys() {
        bindKey("UP", "up", () -> moveCursorBy(1, 0));
        bindKey("DOWN", "down", () -> moveCursorBy(-1, 0));
        bindKey("LEFT", "left", () -> moveCursorBy(0, -1));
        bindKey("RIGHT", "right", () -> moveCursorBy(0, 1));
        Runnable select = () -> {
            if (cursor != null) {
                clickListener.accept(cursor);
            }
        };
        bindKey("ENTER", "select", select);
        bindKey("SPACE", "select", select);
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void moveCursorBy(int rows, int columns) {
        if (board == null) {
            return;
        }
        Position base = cursor != null ? cursor : new Position(0, 0);
        Position next = new Position(base.row() + rows, base.column() + columns);
        if (board.contains(next)) {
            moveCursorTo(next);
        }
    }

    private void moveCursorTo(Position square) {
        cursor = square;
        describeCursor();
        repaint();
    }

    /** Lo que lee un lector de pantalla: la casilla y qué pieza hay ("e2, Peón (blancas)"). */
    private void describeCursor() {
        if (cursor == null || board == null) {
            return;
        }
        Piece piece = board.pieceAt(cursor);
        String square = (char) ('a' + cursor.column()) + String.valueOf(cursor.row() + 1);
        getAccessibleContext().setAccessibleDescription(piece == null ? square + ", vacía"
                : square + ", " + piece.getName() + (piece.getColor() == ar.edu.uade.chess.core.model.Color.WHITE
                        ? " (blancas)" : " (negras)"));
    }

    public void onSquareClicked(Consumer<Position> listener) {
        this.clickListener = listener;
    }

    /**
     * Qué casillas hacen algo al clickearlas (una pieza propia que puede mover, o un
     * destino legal). Solo esas muestran la mano y un leve brillo al pasar el mouse:
     * así se ve qué se puede tocar sin tener que probar.
     */
    public void setClickable(Predicate<Position> clickable) {
        this.clickable = clickable;
    }

    /**
     * Desliza una pieza de origen a destino. Se usa solo para la jugada de la IA: es la
     * que la persona no hizo y tiene que poder seguir con la vista. Las jugadas propias
     * (mouse o teclado) no se animan: la persona ya sabe qué hizo y animarlas la frena.
     * Llamarlo DESPUÉS de showBoard, con el tablero ya actualizado.
     */
    public void animateMove(Move move, Piece moved, Piece captured) {
        stopSlide();
        sliding = move;
        slidingGlyph = glyphs.of(moved);
        capturedGlyph = captured == null ? null : glyphs.of(captured);
        slideProgress = 0;
        slide = new Motion.Tween(0, value -> {
            slideProgress = value;
            if (value >= 1) {
                sliding = null;
            }
            repaint();
        });
        slide.animateTo(1, SLIDE_MILLIS, Motion.EASE_IN_OUT);
    }

    private void stopSlide() {
        if (slide != null) {
            slide.stop();
            slide = null;
        }
        sliding = null;
        slideProgress = 1;
    }

    private void updateHover(Position square) {
        boolean active = square != null && clickable.test(square);
        Position newHover = active ? square : null;
        // null = hereda el cursor de la ventana (por ejemplo, el reloj mientras piensa la IA)
        setCursor(active ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
        if (!java.util.Objects.equals(newHover, hovered)) {
            hovered = newHover;
            repaint();
        }
    }

    /** Muestra el tablero con la casilla seleccionada y sus destinos resaltados. */
    public void showBoard(BoardView board, Position selected, Set<Position> highlighted) {
        showBoard(board, selected, highlighted, null, null);
    }

    /**
     * @param lastMove    última jugada (se resaltan origen y destino), o null
     * @param kingInCheck casilla del rey en jaque (se pinta en rojo), o null
     */
    public void showBoard(BoardView board, Position selected, Set<Position> highlighted,
                          Move lastMove, Position kingInCheck) {
        this.board = board;
        this.selected = selected;
        this.highlighted = highlighted;
        this.lastMove = lastMove;
        this.kingInCheck = kingInCheck;
        if (cursor != null && !board.contains(cursor)) {
            cursor = null;
        }
        stopSlide(); // un tablero nuevo interrumpe cualquier pieza que se estuviera deslizando
        describeCursor();
        if (hovered != null) {
            updateHover(hovered); // lo que se puede clickear cambió con la jugada
        }
        repaint();
    }

    /** Casilla que está bajo ese punto del panel, o null si el punto cae afuera del tablero. */
    public Position positionAt(int x, int y) {
        if (board == null) {
            return null;
        }
        int size = squareSize();
        int column = Math.floorDiv(x - MARGIN, size);
        int rowFromTop = Math.floorDiv(y - MARGIN, size);
        Position square = new Position(board.rows() - 1 - rowFromTop, column);
        return board.contains(square) ? square : null;
    }

    private int squareSize() {
        int width = getWidth() > 0 ? getWidth() : getPreferredSize().width;
        int height = getHeight() > 0 ? getHeight() : getPreferredSize().height;
        int byWidth = (width - 2 * MARGIN) / board.columns();
        int byHeight = (height - 2 * MARGIN) / board.rows();
        return Math.max(1, Math.min(byWidth, byHeight));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (board == null) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int size = squareSize();
        Font pieceFont = new Font(Font.SANS_SERIF, Font.PLAIN, (int) (size * 0.8));

        // Marco alrededor del tablero.
        g.setColor(Theme.BOARD_FRAME);
        g.fillRoundRect(MARGIN - 6, MARGIN - 6, board.columns() * size + 12, board.rows() * size + 12, 10, 10);

        for (int row = 0; row < board.rows(); row++) {
            for (int column = 0; column < board.columns(); column++) {
                Position square = new Position(row, column);
                int x = MARGIN + column * size;
                int y = MARGIN + (board.rows() - 1 - row) * size;
                paintSquare(g, square, x, y, size);

                Piece piece = board.pieceAt(square);
                if (highlighted.contains(square)) {
                    g.setColor(Theme.TARGET);
                    if (piece == null) {
                        g.fillOval(x + size * 3 / 8, y + size * 3 / 8, size / 4, size / 4);
                    } else {
                        g.setStroke(new BasicStroke(Math.max(3f, size / 12f)));
                        g.drawOval(x + 4, y + 4, size - 8, size - 8);
                    }
                }
                if (sliding != null && square.equals(sliding.to())) {
                    // Mientras llega la pieza, la comida se desvanece en su lugar.
                    if (capturedGlyph != null) {
                        drawFaded(g, capturedGlyph, pieceFont, x, y, size, 1 - slideProgress);
                    }
                } else if (piece != null) {
                    drawCentered(g, glyphs.of(piece), pieceFont, x, y, size);
                }
            }
        }
        if (sliding != null) {
            // La pieza en movimiento va encima de todo, entre el centro del origen y el del destino.
            double fromX = MARGIN + sliding.from().column() * size;
            double fromY = MARGIN + (board.rows() - 1 - sliding.from().row()) * size;
            double toX = MARGIN + sliding.to().column() * size;
            double toY = MARGIN + (board.rows() - 1 - sliding.to().row()) * size;
            drawCentered(g, slidingGlyph, pieceFont,
                    (int) Math.round(fromX + (toX - fromX) * slideProgress),
                    (int) Math.round(fromY + (toY - fromY) * slideProgress), size);
        }
        paintCoordinates(g, size);
    }

    private void drawFaded(Graphics2D g, String text, Font font, int x, int y, int size, double opacity) {
        Graphics2D faded = (Graphics2D) g.create();
        faded.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float) Math.max(0, Math.min(1, opacity))));
        drawCentered(faded, text, font, x, y, size);
        faded.dispose();
    }

    private void paintSquare(Graphics2D g, Position square, int x, int y, int size) {
        boolean dark = (square.row() + square.column()) % 2 == 0;
        g.setColor(dark ? Theme.DARK_SQUARE : Theme.LIGHT_SQUARE);
        g.fillRect(x, y, size, size);

        if (lastMove != null && (square.equals(lastMove.from()) || square.equals(lastMove.to()))) {
            g.setColor(Theme.LAST_MOVE);
            g.fillRect(x, y, size, size);
        }
        if (square.equals(selected)) {
            g.setColor(Theme.SELECTED);
            g.fillRect(x, y, size, size);
        }
        if (square.equals(hovered) && !square.equals(selected)) {
            // Sin animación: el mouse pasa por el tablero decenas de veces por partida.
            g.setColor(HOVER);
            g.fillRect(x, y, size, size);
        }
        if (square.equals(kingInCheck)) {
            float radius = size * 0.6f;
            g.setPaint(new RadialGradientPaint(x + size / 2f, y + size / 2f, radius,
                    new float[]{0f, 1f}, new Color[]{Theme.CHECK, new Color(0, 0, 0, 0)}));
            g.fillRect(x, y, size, size);
        }
        if (square.equals(cursor) && Theme.showsFocus(this)) {
            // recuadro doble (blanco + azul) para que se vea sobre casillas claras y oscuras
            int stroke = Math.max(3, size / 16);
            g.setStroke(new BasicStroke(stroke + 2f));
            g.setColor(Color.WHITE);
            g.drawRect(x + stroke, y + stroke, size - 2 * stroke, size - 2 * stroke);
            g.setStroke(new BasicStroke(stroke));
            g.setColor(Theme.FOCUS);
            g.drawRect(x + stroke, y + stroke, size - 2 * stroke, size - 2 * stroke);
        }
    }

    private void paintCoordinates(Graphics2D g, int size) {
        g.setColor(Theme.TEXT_MUTED);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics metrics = g.getFontMetrics();
        for (int column = 0; column < board.columns(); column++) {
            String letter = String.valueOf((char) ('a' + column));
            int x = MARGIN + column * size + (size - metrics.stringWidth(letter)) / 2;
            g.drawString(letter, x, MARGIN + board.rows() * size + 18);
        }
        for (int row = 0; row < board.rows(); row++) {
            String number = String.valueOf(row + 1);
            int y = MARGIN + (board.rows() - 1 - row) * size + (size + metrics.getAscent()) / 2 - 2;
            g.drawString(number, (MARGIN - 6 - metrics.stringWidth(number)) / 2, y);
        }
    }

    private void drawCentered(Graphics2D g, String text, Font font, int x, int y, int size) {
        g.setFont(font);
        g.setColor(Color.BLACK);
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (size - metrics.stringWidth(text)) / 2;
        int textY = y + (size - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(text, textX, textY);
    }
}
