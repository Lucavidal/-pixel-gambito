package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.adapters.Brand;
import ar.edu.uade.chess.adapters.HistoryTexts;
import ar.edu.uade.chess.core.history.HistorySummary;
import ar.edu.uade.chess.core.history.IHistoryService;
import ar.edu.uade.chess.core.tournament.HistoricalPlayer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Menú principal, después de iniciar sesión: saludo, tu récord del torneo y las dos
 * cosas para jugar. El torneo es la opción destacada (relleno verde); la partida
 * libre va al lado, más sobria; historial y ayuda quedan como enlaces abajo.
 *
 * No decide qué pasa al elegir una opción: recibe las acciones (Runnable) de
 * GuiApplication, que es quien abre y cierra las ventanas.
 */
public class MainMenuWindow {

    /** Las acciones del menú, para que la ventana no conozca a las demás ventanas. */
    public record Actions(Runnable freeGame, Runnable tournament, Runnable history,
                          Runnable features, Runnable logout) { }

    private static final Dimension TILE = new Dimension(360, 270);
    private static final int TILE_TEXT_WIDTH = 360 - 48; // ancho de la opción menos su relleno

    private final User user;
    private final IHistoryService history;
    private final List<HistoricalPlayer> roster;
    private final Actions actions;
    private final JLabel record = Theme.label(" ", Theme.BODY, Theme.TEXT_MUTED);
    private JFrame frame;

    public MainMenuWindow(User user, IHistoryService history, List<HistoricalPlayer> roster, Actions actions) {
        this.user = user;
        this.history = history;
        this.roster = roster;
        this.actions = actions;
    }

    public void show() {
        frame = new JFrame("Ajedrez — " + user.visibleName());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0); // cerrar el menú = salir del programa
            }
        });

        JButton logout = Theme.linkButton("Cerrar sesión", "Volver a la pantalla de inicio de sesión");
        logout.addActionListener(e -> actions.logout().run());
        JPanel greeting = new JPanel(new BorderLayout(0, 4));
        greeting.setOpaque(false);
        greeting.add(Theme.label("Hola, " + user.visibleName(), Theme.HERO, Theme.TEXT), BorderLayout.NORTH);
        greeting.add(record, BorderLayout.SOUTH);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(greeting, BorderLayout.CENTER);
        header.add(logout, BorderLayout.EAST);

        JPanel tiles = new JPanel(new GridLayout(1, 2, 16, 0));
        tiles.setOpaque(false);
        tiles.add(freeGameTile());
        tiles.add(tournamentTile());

        JButton historyLink = Theme.linkButton("Mi historial", "Tus rondas del torneo, con fecha y resultado");
        historyLink.addActionListener(e -> actions.history().run());
        JButton featuresLink = Theme.linkButton("Qué incluye el juego", "Reglas, modos de juego y cómo piensa la IA");
        featuresLink.addActionListener(e -> actions.features().run());
        JButton exit = Theme.linkButton("Salir", "Cerrar el programa");
        exit.addActionListener(e -> System.exit(0));
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        links.setOpaque(false);
        links.add(historyLink);
        links.add(featuresLink);
        JPanel actionsRow = new JPanel(new BorderLayout());
        actionsRow.setOpaque(false);
        actionsRow.add(links, BorderLayout.WEST);
        actionsRow.add(exit, BorderLayout.EAST);
        // Pie: la misma línea de derechos de autor que la web (Brand)
        JLabel copyright = Theme.label(Brand.copyrightLine(), Theme.SMALL, Theme.TEXT_MUTED);
        copyright.setHorizontalAlignment(JLabel.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 14));
        footer.setOpaque(false);
        footer.add(actionsRow, BorderLayout.NORTH);
        footer.add(copyright, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 20, 32));
        content.add(header, BorderLayout.NORTH);
        content.add(tiles, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        frame.setContentPane(content);
        refreshRecord();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** Vuelve a mostrar el menú (por ejemplo, al cerrar una partida) con el récord actualizado. */
    public void showAgain() {
        refreshRecord();
        frame.setVisible(true);
        frame.toFront();
    }

    public void hide() {
        frame.setVisible(false);
    }

    public void close() {
        frame.dispose();
    }

    public JFrame frame() {
        return frame;
    }

    /** "Tu récord en el torneo: 3 ganadas · 1 tablas · 2 perdidas" (el mismo texto que la web). */
    private void refreshRecord() {
        HistorySummary s = history.summary(user);
        record.setText(HistoryTexts.recordText(s));
    }

    private JPanel freeGameTile() {
        PressableCard tile = new PressableCard(false, actions.freeGame());
        tile.add(titled("Partida libre", Theme.TEXT, new Paragraph("Dos personas en la misma computadora. "
                + "Se marcan las jugadas legales y podés deshacer, rehacer y seguir la planilla.",
                Theme.BODY, Theme.TEXT_MUTED, TILE_TEXT_WIDTH)), BorderLayout.NORTH);
        tile.add(Theme.label("Empezar partida", Theme.BODY_STRONG, Theme.ACCENT), BorderLayout.SOUTH);
        tile.describe("Partida libre", "Dos personas en la misma computadora.");
        return tile;
    }

    /** La opción destacada: el torneo, con su escalera de rivales a la vista. */
    private JPanel tournamentTile() {
        PressableCard tile = new PressableCard(true, actions.tournament());
        tile.add(titled("Torneo escalera", Theme.ON_ACCENT, new Paragraph("Jugás con blancas contra rivales "
                + "históricos, cada uno más difícil. Ganá las " + roster.size() + " rondas para ser campeón.",
                Theme.BODY, new Color(0xC9DCCF), TILE_TEXT_WIDTH)), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        for (int i = 0; i < roster.size(); i++) {
            HistoricalPlayer rival = roster.get(i);
            JLabel step = Theme.label((i + 1) + "   " + rival.name(), Theme.BODY_STRONG, Theme.ON_ACCENT);
            step.setAlignmentX(Component.LEFT_ALIGNMENT);
            step.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            body.add(step);
        }
        tile.add(body, BorderLayout.CENTER);
        tile.add(Theme.label("Entrar al torneo", Theme.BODY_STRONG, Theme.ON_ACCENT), BorderLayout.SOUTH);
        tile.describe("Torneo escalera", roster.size() + " rondas contra la computadora.");
        return tile;
    }

    /** Título de la opción con su descripción justo debajo. */
    private static JPanel titled(String title, Color color, Paragraph description) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(Theme.label(title, Theme.TITLE, color), BorderLayout.NORTH);
        panel.add(description, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Opción del menú que funciona como botón grande: se abre con el mouse o con el
     * teclado (Tab para llegar, Enter o Espacio para abrir). Al presionarla se achica
     * apenas (98 %) para confirmar el clic; el recuadro de foco aparece solo con teclado.
     */
    private static final class PressableCard extends JPanel {

        private static final double PRESSED_SCALE = 0.98;
        private static final int RADIUS = 14;

        private final boolean featured;
        private final Motion.Tween scale = new Motion.Tween(1, value -> repaint());
        private boolean hovered;

        PressableCard(boolean featured, Runnable action) {
            super(new BorderLayout(0, 12));
            this.featured = featured;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(24, 24, 22, 24));
            setPreferredSize(TILE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            Theme.repaintOnFocus(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    scale.animateTo(PRESSED_SCALE, 100, Motion.EASE_OUT);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    scale.animateTo(1, 160, Motion.EASE_OUT);
                    // mouseClicked no llega si el mouse se movió un píxel entre apretar y soltar
                    if (contains(e.getPoint())) {
                        requestFocusInWindow();
                        action.run();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "open");
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "open");
            getActionMap().put("open", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { action.run(); }
            });
        }

        void describe(String name, String description) {
            getAccessibleContext().setAccessibleName(name);
            getAccessibleContext().setAccessibleDescription(description);
        }

        @Override
        public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double s = scale.value();
            if (s < 0.999) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.translate(getWidth() / 2.0, getHeight() / 2.0);
                g.scale(s, s);
                g.translate(-getWidth() / 2.0, -getHeight() / 2.0);
            }
            int w = getWidth();
            int h = getHeight();
            if (featured) {
                g.setColor(hovered ? Theme.ACCENT_DARK : Theme.ACCENT);
                g.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
            } else {
                g.setColor(Theme.CARD);
                g.fillRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);
                g.setColor(hovered ? Theme.ACCENT : Theme.BORDER);
                g.setStroke(new BasicStroke(hovered ? 2 : 1));
                g.drawRoundRect(1, 1, w - 3, h - 3, RADIUS, RADIUS);
            }
            super.paint(g);
            if (Theme.showsFocus(this)) {
                g.setColor(Theme.FOCUS);
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(2, 2, w - 5, h - 5, RADIUS, RADIUS);
            }
            g.dispose();
        }
    }
}
