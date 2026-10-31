package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.game.MoveResult;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import java.util.function.Supplier;

/**
 * ADAPTADOR DE ENTRADA gráfico para una partida libre entre dos personas.
 *
 * Muestra todo lo que el núcleo sabe hacer en una partida: movimientos legales
 * resaltados, jaque (rey en rojo), jaque mate, ahogado, capturas, deshacer y
 * rehacer, y la planilla de jugadas. Depende solo del puerto IGame; cada
 * "Nueva partida" pide una partida nueva a la fábrica que recibe.
 */
public class GameWindow {

    private final Supplier<IGame> games;
    private final MoveLog log;
    private final String playerName;
    private final Runnable onClose;
    private final BoardPanel boardPanel;
    private final ClickToMove selector = new ClickToMove();

    private final Paragraph messageLabel = new Paragraph(" ", Theme.SMALL, Theme.TEXT_MUTED, 300);
    private final MoveListPanel moveList = new MoveListPanel();
    private final PlayerStrip whiteStrip;
    private final PlayerStrip blackStrip;
    private JButton undoButton;
    private JButton redoButton;
    private JFrame frame;
    private IGame game;

    /**
     * @param games      fábrica de partidas (una nueva al abrir y en cada "Nueva partida")
     * @param playerName usuario que inició sesión (se muestra en el título)
     * @param onClose    qué hacer al cerrar la ventana (por ejemplo, volver al menú)
     */
    public GameWindow(Supplier<IGame> games, PieceGlyphs glyphs, MoveLog log, String playerName, Runnable onClose) {
        this.games = games;
        this.log = log;
        this.playerName = playerName;
        this.onClose = onClose;
        this.boardPanel = new BoardPanel(glyphs);
        this.whiteStrip = new PlayerStrip(glyphs, "Blancas");
        this.blackStrip = new PlayerStrip(glyphs, "Negras");
    }

    public void show() {
        game = games.get();
        boardPanel.onSquareClicked(this::onSquareClicked);
        boardPanel.setClickable(square -> !game.getStatus().isOver()
                && (selector.getTargets().contains(square) || !ownLegalMoves(square).isEmpty()));

        frame = new JFrame("Partida libre — " + playerName);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leave(); // la X pregunta lo mismo que "Volver al menú"
            }

            @Override
            public void windowClosed(WindowEvent e) {
                onClose.run();
            }
        });
        JPanel content = GameLayout.content(boardPanel, "Clic en una pieza para ver a dónde puede ir. "
                + "Con el teclado: flechas y Enter. Ctrl+Z deshace.", sidePanel());
        frame.setContentPane(content);
        bindShortcuts(content);

        refresh("Partida nueva: empiezan las blancas.");
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * La hoja lateral, de arriba abajo como el tablero: negras, la planilla, blancas
     * y las acciones.
     */
    private JPanel sidePanel() {
        undoButton = Theme.button("Deshacer", "Deshace la última jugada (Ctrl+Z)");
        undoButton.addActionListener(e -> undo());
        redoButton = Theme.button("Rehacer", "Rehace la jugada deshecha (Ctrl+Y)");
        redoButton.addActionListener(e -> redo());
        JButton newGame = Theme.button("Nueva partida", "Empieza de cero");
        newGame.addActionListener(e -> newGame());
        JButton back = Theme.linkButton("Volver al menú", "Cierra esta partida");
        back.addActionListener(e -> leave());
        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setOpaque(false);
        buttons.add(undoButton);
        buttons.add(redoButton);
        buttons.add(newGame);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(back, BorderLayout.WEST);

        JPanel sheet = GameLayout.stack(8);
        sheet.add(Theme.heading("Planilla"), BorderLayout.NORTH);
        sheet.add(moveList, BorderLayout.CENTER);
        sheet.add(messageLabel, BorderLayout.SOUTH);

        JPanel bottom = GameLayout.stack(12);
        bottom.add(whiteStrip, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);
        bottom.add(footer, BorderLayout.SOUTH);

        JPanel side = GameLayout.stack(16);
        side.add(blackStrip, BorderLayout.NORTH);
        side.add(sheet, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private void bindShortcuts(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Z"), "undo");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Y"), "redo");
        root.getActionMap().put("undo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { undo(); }
        });
        root.getActionMap().put("redo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { redo(); }
        });
    }

    private void onSquareClicked(Position square) {
        if (game.getStatus().isOver()) {
            return;
        }
        selector.click(square, ownLegalMoves(square)).ifPresentOrElse(this::play, () -> refresh(null));
    }

    private void play(Move move) {
        Piece moved = game.getBoard().pieceAt(move.from());
        Piece target = game.getBoard().pieceAt(move.to());
        MoveResult result = game.move(move.from(), move.to());
        if (result.isSuccess()) {
            log.record(move, moved, target, suffix());
        }
        refresh(result.getMessage());
        if (game.getStatus().isOver()) {
            JOptionPane.showMessageDialog(frame, statusText(), "Fin de la partida", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void undo() {
        if (game.undo()) {
            log.undo();
            refresh("Jugada deshecha.");
        } else {
            refresh("No hay jugadas para deshacer.");
        }
    }

    private void redo() {
        if (game.redo()) {
            log.redo();
            refresh("Jugada rehecha.");
        } else {
            refresh("No hay jugadas para rehacer.");
        }
    }

    private void newGame() {
        if (gameInProgress() && !Theme.confirm(frame, "Nueva partida",
                "Se pierde la partida en curso. ¿Empezar una nueva?", "Empezar de cero")) {
            return;
        }
        game = games.get();
        log.clear();
        refresh("Partida nueva.");
    }

    /** Cierra la ventana; si hay una partida a medias, primero pregunta. */
    private void leave() {
        if (gameInProgress() && !Theme.confirm(frame, "Volver al menú",
                "La partida libre no se guarda: si salís, se pierde. ¿Volver al menú?", "Salir de la partida")) {
            return;
        }
        frame.dispose();
    }

    private boolean gameInProgress() {
        return !log.entries().isEmpty() && !game.getStatus().isOver();
    }

    /** "+" si la jugada dio jaque, "#" si dio mate. */
    private String suffix() {
        return switch (game.getStatus()) {
            case CHECKMATE -> "#";
            case CHECK -> "+";
            default -> "";
        };
    }

    private Set<Position> ownLegalMoves(Position square) {
        Piece piece = game.getBoard().pieceAt(square);
        return piece != null && piece.getColor() == game.getTurn() ? game.legalMoves(square) : Set.of();
    }

    /** Redibuja todo. Con message == null solo cambió la selección. */
    private void refresh(String message) {
        if (message != null) {
            selector.clear();
            messageLabel.setText(message);
        }
        Color turn = game.getTurn();
        GameStatus status = game.getStatus();
        boolean inCheck = status == GameStatus.CHECK || status == GameStatus.CHECKMATE;
        boardPanel.showBoard(game.getBoard(), selector.getSelected(), selector.getTargets(),
                log.lastMove(), inCheck ? kingOf(game.getBoard(), turn) : null);

        showStrip(whiteStrip, Color.WHITE, turn, status);
        showStrip(blackStrip, Color.BLACK, turn, status);
        whiteStrip.setCaptured(log.capturedPiecesOf(Color.BLACK));
        blackStrip.setCaptured(log.capturedPiecesOf(Color.WHITE));
        moveList.show(log);
        undoButton.setEnabled(!log.entries().isEmpty());
        redoButton.setEnabled(log.canRedo());
    }

    /** Fila verde = le toca; debajo del nombre, cómo está ese bando. */
    private static void showStrip(PlayerStrip strip, Color side, Color turn, GameStatus status) {
        boolean toMove = side == turn;
        strip.setActive(toMove && !status.isOver());
        switch (status) {
            case IN_PROGRESS -> strip.setNote(toMove ? "Le toca mover" : "", null);
            case CHECK -> strip.setNote(toMove ? "¡En jaque!" : "", toMove ? new java.awt.Color(0xFFD2CB) : null);
            case CHECKMATE -> strip.setNote(toMove ? "Jaque mate: perdió" : "Ganó la partida",
                    toMove ? Theme.DANGER : Theme.ACCENT);
            case STALEMATE -> strip.setNote("Tablas por rey ahogado", null);
        }
    }

    private String statusText() {
        return switch (game.getStatus()) {
            case CHECKMATE -> "¡Jaque mate! Ganan las " + name(game.getTurn().opposite()) + ".";
            case STALEMATE -> "Rey ahogado: tablas.";
            case CHECK -> "¡Las " + name(game.getTurn()) + " están en jaque!";
            case IN_PROGRESS -> "Partida en curso, " + Theme.count(log.entries().size(), "jugada", "jugadas") + ".";
        };
    }

    /** Casilla del rey de ese color, recorriendo el tablero sin asumir su tamaño. */
    static Position kingOf(BoardView board, Color color) {
        for (int row = 0; row < board.rows(); row++) {
            for (int column = 0; column < board.columns(); column++) {
                Piece piece = board.pieceAt(new Position(row, column));
                if (piece != null && piece.isKing() && piece.getColor() == color) {
                    return new Position(row, column);
                }
            }
        }
        return null;
    }

    private static String name(Color color) {
        return color == Color.WHITE ? "Blancas" : "Negras";
    }
}
