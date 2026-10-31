package ar.edu.uade.chess.adapters.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;

/**
 * Colores, fuentes y "ladrillos" visuales compartidos por todas las ventanas. Es solo presentación.
 *
 * La idea visual es una sala de torneo: el tablero de vinilo verde y crema de los
 * torneos, el paño verde de las mesas (que también marca a quién le toca, como el
 * lado del reloj que corre) y el rojo del reloj solo para el jaque y las derrotas.
 * Los nombres y las jugadas van en Bahnschrift (la DIN de Windows, la de los relojes
 * y marcadores); el resto del texto en Segoe UI.
 */
public final class Theme {

    private Theme() { }

    // Sala y papel
    public static final Color BACKGROUND = new Color(0xE3E8E2); // la sala: gris verdoso frío
    public static final Color CARD = new Color(0xF8F9F6);       // la hoja: paneles y planilla
    public static final Color BORDER = new Color(0xC9D1C9);
    public static final Color TEXT = new Color(0x16231C);
    public static final Color TEXT_MUTED = new Color(0x55635A);

    // Paño (acción principal y turno), reloj (jaque, derrota) y bronce (campeón)
    public static final Color ACCENT = new Color(0x2C5A43);
    public static final Color ACCENT_DARK = new Color(0x1F4331);
    public static final Color ACCENT_TINT = new Color(0xDCE8DF);
    public static final Color ON_ACCENT = new Color(0xF4F7F2);
    public static final Color SUCCESS = new Color(0x2C5A43);
    public static final Color DANGER = new Color(0xB83227);
    public static final Color CHAMPION = new Color(0x8E6D22);
    public static final Color INFO = new Color(0x2B5C8A);

    // Tablero de vinilo de torneo
    public static final Color LIGHT_SQUARE = new Color(0xECEDD5);
    public static final Color DARK_SQUARE = new Color(0x6F9466);
    public static final Color BOARD_FRAME = new Color(0x1F4331);
    public static final Color SELECTED = new Color(0x3D, 0x8B, 0xD9, 0x80);
    public static final Color LAST_MOVE = new Color(0xF0, 0xD9, 0x5C, 0x90);
    public static final Color TARGET = new Color(0x16, 0x23, 0x1C, 0x60);
    public static final Color CHECK = new Color(0xE0, 0x3A, 0x2C, 0xE0);
    /** Recuadro de lo que tiene el foco del teclado: azul, para que se distinga del verde. */
    public static final Color FOCUS = new Color(0x2B6CB0);

    private static final String DISPLAY_FAMILY = installed("Bahnschrift", Font.SANS_SERIF);
    private static final String BODY_FAMILY = installed("Segoe UI", Font.SANS_SERIF);

    public static final Font HERO = new Font(DISPLAY_FAMILY, Font.PLAIN, 32);
    public static final Font TITLE = new Font(DISPLAY_FAMILY, Font.PLAIN, 22);
    public static final Font SUBTITLE = new Font(DISPLAY_FAMILY, Font.PLAIN, 16);
    /** Jugadas de la planilla y números: la DIN se lee como un marcador. */
    public static final Font MOVES = new Font(DISPLAY_FAMILY, Font.PLAIN, 15);
    public static final Font BODY = new Font(BODY_FAMILY, Font.PLAIN, 13);
    public static final Font BODY_STRONG = new Font(BODY_FAMILY, Font.BOLD, 13);
    public static final Font SMALL = new Font(BODY_FAMILY, Font.PLAIN, 12);

    /** La fuente pedida si está instalada; si no (otro sistema), la genérica. */
    private static String installed(String family, String fallback) {
        try {
            String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            return Arrays.asList(families).contains(family) ? family : fallback;
        } catch (RuntimeException headless) {
            return fallback;
        }
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** Texto que se acomoda en varias líneas dentro de ese ancho (en píxeles), en vez de cortarse. */
    public static String wrap(String text, int widthPx) {
        return "<html><body style='width:" + widthPx + "px'>" + text + "</body></html>";
    }

    /** Botón común (contorno). */
    public static FlatButton button(String text, String tooltip) {
        return new FlatButton(text, tooltip, FlatButton.Kind.SECONDARY);
    }

    /** La acción principal de la pantalla (relleno verde). Una por pantalla. */
    public static FlatButton primaryButton(String text, String tooltip) {
        return new FlatButton(text, tooltip, FlatButton.Kind.PRIMARY);
    }

    /** Acción secundaria que se lee como un enlace. */
    public static FlatButton linkButton(String text, String tooltip) {
        return new FlatButton(text, tooltip, FlatButton.Kind.LINK);
    }

    /** Campo de texto con aire adentro y borde verde al tener el foco. */
    public static void styleField(JTextField field) {
        Border rest = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(7, 9, 7, 9));
        Border focused = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 2), BorderFactory.createEmptyBorder(6, 8, 6, 8));
        field.setFont(new Font(BODY_FAMILY, Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(TEXT);
        field.setBorder(rest);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { field.setBorder(focused); }

            @Override
            public void focusLost(FocusEvent e) { field.setBorder(rest); }
        });
    }

    /**
     * Tabla de solo lectura con el aspecto de la planilla: renglones finos, sin líneas
     * verticales, encabezado plano y las columnas de números a la derecha (encolumnadas).
     */
    public static void styleTable(JTable table) {
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setBackground(CARD);
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowSelectionAllowed(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setFont(SMALL);
                setForeground(TEXT_MUTED);
                setBackground(BACKGROUND);
                setHorizontalAlignment(isNumeric(t, column) ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), padding(6, 10)));
                return this;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setHorizontalAlignment(isNumeric(t, column) ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setForeground(TEXT);
                setBackground(CARD);
                setBorder(padding(0, 10));
                return this;
            }
        });
        table.setDefaultRenderer(Integer.class, table.getDefaultRenderer(Object.class));
    }

    private static boolean isNumeric(JTable table, int column) {
        return Number.class.isAssignableFrom(table.getColumnClass(column));
    }

    /** Título de una sección del panel: sin mayúsculas ni recuadro, solo jerarquía tipográfica. */
    public static JLabel heading(String text) {
        JLabel label = label(text, SUBTITLE, TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return label;
    }

    /**
     * Pide confirmación con botones que dicen qué hacen ("Reiniciar" / "Cancelar")
     * en vez de "Sí" / "No". Devuelve true solo si se eligió la acción.
     */
    public static boolean confirm(Component parent, String title, String message, String actionLabel) {
        Object[] options = {actionLabel, "Cancelar"};
        int choice = JOptionPane.showOptionDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    /** "1 jugada", "3 jugadas": el número con la palabra en singular o plural. */
    public static String count(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    private static volatile boolean usingKeyboard = false;

    /**
     * Como :focus-visible de la web: el recuadro de foco se muestra solo si la persona
     * está usando el teclado. Una tecla lo enciende, un clic lo apaga. Así quien usa el
     * mouse no ve un recuadro azul "suelto" en algo que no tocó.
     * Se llama una vez al arrancar la interfaz.
     */
    public static void trackInputMode() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            boolean keyboard = event.getID() == KeyEvent.KEY_PRESSED;
            boolean mouse = event.getID() == MouseEvent.MOUSE_PRESSED;
            if ((keyboard && !usingKeyboard) || (mouse && usingKeyboard)) {
                usingKeyboard = keyboard;
                for (Window window : Window.getWindows()) {
                    window.repaint();
                }
            }
        }, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
    }

    /** Si ese componente tiene que dibujar su recuadro de foco. */
    public static boolean showsFocus(Component component) {
        return usingKeyboard && component.isFocusOwner();
    }

    /** Repinta el componente cuando gana o pierde el foco (para su recuadro de foco). */
    public static void repaintOnFocus(JComponent component) {
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { component.repaint(); }

            @Override
            public void focusLost(FocusEvent e) { component.repaint(); }
        });
    }

    public static Border padding(int size) {
        return BorderFactory.createEmptyBorder(size, size, size, size);
    }

    public static Border padding(int vertical, int horizontal) {
        return BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal);
    }
}
