package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.adapters.HistoryTexts;
import ar.edu.uade.chess.core.ai.Difficulty;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.history.HistorySummary;
import ar.edu.uade.chess.core.history.IHistoryService;
import ar.edu.uade.chess.core.history.RoundRecord;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Muestra el historial del usuario: los totales en una frase arriba y una tabla con
 * las últimas rondas, con el mismo aspecto que la planilla de jugadas.
 * El resultado lleva color (verde ganada, rojo perdida, bronce campeón), además del texto;
 * la dificultad, un color más suave por nivel. Arriba se puede filtrar por dificultad,
 * y los totales siguen al filtro.
 */
public class HistoryDialog {

    public static final int RECENT_ROUNDS = 50;
    /** "22 oct 2026, 10:42": el mes en palabras no se confunde entre día/mes y mes/día. */
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-AR"));
    private static final int DIFFICULTY_COLUMN = 3;
    private static final int OUTCOME_COLUMN = 4;
    private static final int MOVES_COLUMN = 5;
    private static final String ALL = "Todas las dificultades";

    private final IHistoryService history;
    private final User user;

    public HistoryDialog(IHistoryService history, User user) {
        this.history = history;
        this.user = user;
    }

    public void show(Component parent) {
        Paragraph totals = new Paragraph(" ", Theme.BODY, Theme.TEXT, 560);
        totals.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Fecha", "Ronda", "Rival", "Dificultad", "Resultado", "Jugadas"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }

            // "Jugadas" como número: va a la derecha y las cifras quedan encolumnadas.
            @Override
            public Class<?> getColumnClass(int column) { return column == MOVES_COLUMN ? Integer.class : String.class; }
        };
        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(DIFFICULTY_COLUMN).setCellRenderer(new DifficultyRenderer());
        table.getColumnModel().getColumn(OUTCOME_COLUMN).setCellRenderer(new OutcomeRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getAccessibleContext().setAccessibleName("Últimas rondas del torneo");
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.setPreferredSize(new Dimension(660, 280));

        Paragraph empty = new Paragraph(" ", Theme.BODY, Theme.TEXT_MUTED, 420);
        CardLayout cards = new CardLayout();
        JPanel body = new JPanel(cards);
        body.setOpaque(false);
        body.add(scroll, "table");
        body.add(empty, "empty");

        // Filtro: "Todas" o una dificultad. Cambiarlo vuelve a pedir los totales y las rondas.
        JComboBox<String> filter = new JComboBox<>();
        filter.addItem(ALL);
        for (Difficulty d : Difficulty.values()) {
            filter.addItem(HistoryTexts.difficultyText(d));
        }
        filter.setFont(Theme.BODY);
        filter.getAccessibleContext().setAccessibleName("Filtrar por dificultad");
        Runnable refresh = () -> {
            Difficulty selected = filter.getSelectedIndex() == 0 ? null : Difficulty.values()[filter.getSelectedIndex() - 1];
            HistorySummary s = history.summary(user, selected);
            totals.setText(summaryText(s));
            model.setRowCount(0);
            List<RoundRecord> rounds = history.recentRounds(user, RECENT_ROUNDS, selected);
            for (RoundRecord r : rounds) {
                model.addRow(new Object[]{DATE.format(r.playedAt()), r.round() + " de " + r.totalRounds(),
                        r.rivalName(), HistoryTexts.difficultyText(r.difficulty()), outcomeText(r), r.userMoves()});
            }
            empty.setText(selected == null
                    ? "Todavía no jugaste ninguna ronda. Entrá al torneo escalera desde el menú: "
                      + "cada ronda que termines aparece acá."
                    : "No hay rondas guardadas con dificultad " + HistoryTexts.difficultyText(selected) + ".");
            cards.show(body, rounds.isEmpty() ? "empty" : "table");
        };
        filter.addActionListener(e -> refresh.run());
        refresh.run();

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        filterRow.add(Theme.label("Dificultad:", Theme.BODY, Theme.TEXT_MUTED));
        filterRow.add(filter);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(filterRow, BorderLayout.NORTH);
        top.add(totals, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.add(top, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(parent, content, "Historial de " + user.visibleName(), JOptionPane.PLAIN_MESSAGE);
    }

    /** "12 rondas jugadas: 7 ganadas, 3 perdidas y 2 tablas. Ganaste 1 torneo." */
    private static String summaryText(HistorySummary s) {
        if (s.roundsPlayed() == 0) {
            return "Sin rondas jugadas.";
        }
        String text = Theme.count(s.roundsPlayed(), "ronda jugada", "rondas jugadas") + ": "
                + Theme.count(s.wins(), "ganada", "ganadas") + ", "
                + Theme.count(s.losses(), "perdida", "perdidas") + " y "
                + s.draws() + " tablas.";
        return s.titles() == 0 ? text : text + " Ganaste " + Theme.count(s.titles(), "torneo", "torneos") + ".";
    }

    private static String outcomeText(RoundRecord r) {
        if (r.wonTheTournament()) {
            return "Campeón";
        }
        return switch (r.outcome()) {
            case WON -> "Ganada";
            case LOST -> "Perdida";
            case DRAWN -> "Tablas";
            case ONGOING -> "En curso";
        };
    }

    /** El resultado en su color, en negrita: se encuentra de un vistazo al recorrer la tabla. */
    private static final class OutcomeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell = table.getDefaultRenderer(Object.class)
                    .getTableCellRendererComponent(table, value, false, false, row, column);
            cell.setFont(Theme.BODY_STRONG);
            cell.setForeground(switch (String.valueOf(value)) {
                case "Campeón" -> Theme.CHAMPION;
                case "Ganada" -> Theme.ACCENT;
                case "Perdida" -> Theme.DANGER;
                default -> Theme.TEXT_MUTED;
            });
            return cell;
        }
    }

    /** La dificultad con un color suave por nivel (sin negrita: el protagonista es el resultado). */
    private static final class DifficultyRenderer extends DefaultTableCellRenderer {
        private static final Color EASY = new Color(0x4C7A5C);
        private static final Color MEDIUM = new Color(0x8A7240);
        private static final Color HARD = new Color(0x9A4E46);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component cell = table.getDefaultRenderer(Object.class)
                    .getTableCellRendererComponent(table, value, false, false, row, column);
            String text = String.valueOf(value);
            cell.setForeground(text.equals(HistoryTexts.difficultyText(Difficulty.EASY)) ? EASY
                    : text.equals(HistoryTexts.difficultyText(Difficulty.MEDIUM)) ? MEDIUM
                    : text.equals(HistoryTexts.difficultyText(Difficulty.HARD)) ? HARD
                    : Theme.TEXT_MUTED);
            return cell;
        }
    }
}
