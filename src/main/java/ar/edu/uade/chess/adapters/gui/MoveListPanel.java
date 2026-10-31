package ar.edu.uade.chess.adapters.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/**
 * La planilla de jugadas como la de un torneo: número, jugada de blancas y de negras,
 * con renglones. Siempre muestra la última jugada. Sin jugadas, dice cómo empezar.
 */
public class MoveListPanel extends JPanel {

    private static final String EMPTY = "empty";
    private static final String TABLE = "table";

    private final CardLayout cards = new CardLayout();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"", "Blancas", "Negras"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JScrollPane scroll = new JScrollPane(table);

    public MoveListPanel() {
        setLayout(cards);
        setOpaque(false);

        table.setFont(Theme.MOVES);
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.CARD);
        table.setRowHeight(26);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getAccessibleContext().setAccessibleName("Planilla de jugadas");

        table.getColumnModel().getColumn(0).setCellRenderer(new PaddedRenderer(SwingConstants.RIGHT, Theme.TEXT_MUTED));
        table.getColumnModel().getColumn(0).setPreferredWidth(44);
        table.getColumnModel().getColumn(0).setMaxWidth(48);
        table.getColumnModel().getColumn(1).setCellRenderer(new PaddedRenderer(SwingConstants.LEFT, Theme.TEXT));
        table.getColumnModel().getColumn(2).setCellRenderer(new PaddedRenderer(SwingConstants.LEFT, Theme.TEXT));

        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.CARD);
        scroll.setPreferredSize(new Dimension(260, 170));

        JPanel empty = new JPanel(new BorderLayout());
        empty.setBackground(Theme.CARD);
        empty.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), Theme.padding(12, 12)));
        empty.add(new Paragraph("Todavía no hay jugadas. Empiezan las blancas.", Theme.SMALL, Theme.TEXT_MUTED, 260),
                BorderLayout.NORTH);

        add(empty, EMPTY);
        add(scroll, TABLE);
    }

    public void show(MoveLog log) {
        List<MoveLog.Entry> entries = log.entries();
        model.setRowCount(0);
        for (int i = 0; i < entries.size(); i += 2) {
            String white = log.text(entries.get(i));
            String black = i + 1 < entries.size() ? log.text(entries.get(i + 1)) : "";
            model.addRow(new Object[]{(i / 2 + 1) + ".", white, black});
        }
        cards.show(this, entries.isEmpty() ? EMPTY : TABLE);
        if (!entries.isEmpty()) {
            int last = model.getRowCount() - 1;
            table.scrollRectToVisible(table.getCellRect(last, 0, true));
        }
    }

    /**
     * Celda con aire a los costados. DefaultTableCellRenderer vuelve a poner su borde
     * en cada celda, por eso el relleno se aplica después de llamarlo.
     */
    private static final class PaddedRenderer extends DefaultTableCellRenderer {
        private final int alignment;
        private final java.awt.Color color;

        PaddedRenderer(int alignment, java.awt.Color color) {
            this.alignment = alignment;
            this.color = color;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setHorizontalAlignment(alignment);
            setForeground(color);
            setBackground(Theme.CARD);
            setBorder(alignment == SwingConstants.RIGHT ? BorderFactory.createEmptyBorder(0, 0, 0, 10)
                    : BorderFactory.createEmptyBorder(0, 10, 0, 0));
            return this;
        }
    }

    /** Encabezado plano, en el gris de la planilla, sin el relieve de Windows. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(Theme.SMALL);
            setForeground(Theme.TEXT_MUTED);
            setBackground(Theme.BACKGROUND);
            setHorizontalAlignment(column == 0 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(5, column == 0 ? 0 : 10, 5, column == 0 ? 10 : 0)));
            return this;
        }
    }
}
