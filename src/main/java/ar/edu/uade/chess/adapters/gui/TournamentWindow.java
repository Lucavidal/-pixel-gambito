package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.core.ai.Move;
import ar.edu.uade.chess.core.game.GameStatus;
import ar.edu.uade.chess.core.model.BoardView;
import ar.edu.uade.chess.core.model.Color;
import ar.edu.uade.chess.core.model.Piece;
import ar.edu.uade.chess.core.model.Position;
import ar.edu.uade.chess.core.tournament.HistoricalPlayer;
import ar.edu.uade.chess.core.tournament.ITournament;
import ar.edu.uade.chess.core.tournament.RoundOutcome;
import ar.edu.uade.chess.core.tournament.TournamentStatus;
import ar.edu.uade.chess.core.tournament.TurnReport;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ADAPTADOR DE ENTRADA gráfico para el torneo escalera contra la IA.
 *
 * Muestra todo lo del torneo: la escalera de rivales (ganada / actual / pendiente),
 * la ficha del rival (estilo y dificultad explicados), la IA "pensando", la
 * planilla de jugadas, las capturas, el jaque y el resultado de cada ronda.
 *
 * Depende solo del puerto ITournament. La demora de la IA es un javax.swing.Timer:
 * corre en el hilo de la interfaz, así la ventana no se congela y no hay concurrencia.
 */
public class TournamentWindow {

    private final ITournament tournament;
    private final AlgebraicNotation notation;
    private final MoveLog log;
    private final int opponentDelayMillis;
    private final String playerName;
    private final HistoryDialog historyDialog;
    private final Runnable onClose;
    private final BoardPanel boardPanel;
    private final ClickToMove selector = new ClickToMove();

    private final JLabel roundTitle = Theme.label(" ", Theme.TITLE, Theme.TEXT);
    private final JPanel ladder = new JPanel();
    private final Paragraph rivalInfo = new Paragraph(" ", Theme.SMALL, Theme.TEXT_MUTED, 300);
    private final Paragraph messageLabel = new Paragraph(" ", Theme.SMALL, Theme.TEXT_MUTED, 300);
    private final MoveListPanel moveList = new MoveListPanel();
    private final PlayerStrip rivalStrip;
    private final PlayerStrip userStrip;
    private JFrame frame;
    private Timer opponentTimer; // respuesta de la IA pendiente, o null

    /**
     * @param opponentDelayMillis cuánto "piensa" la IA antes de mostrar su jugada (0 = sin demora).
     *                            Es solo presentación: el núcleo no espera nada.
     * @param playerName          usuario que inició sesión
     * @param historyDialog       ventana del historial del usuario
     * @param onClose             qué hacer al cerrar la ventana (por ejemplo, volver al menú)
     */
    public TournamentWindow(ITournament tournament, PieceGlyphs glyphs, AlgebraicNotation notation, MoveLog log,
                            int opponentDelayMillis, String playerName, HistoryDialog historyDialog, Runnable onClose) {
        this.tournament = tournament;
        this.notation = notation;
        this.log = log;
        this.opponentDelayMillis = opponentDelayMillis;
        this.playerName = playerName;
        this.historyDialog = historyDialog;
        this.onClose = onClose;
        this.boardPanel = new BoardPanel(glyphs);
        this.rivalStrip = new PlayerStrip(glyphs, " ");
        this.userStrip = new PlayerStrip(glyphs, "Vos, con blancas");
    }

    public void show() {
        boardPanel.onSquareClicked(this::onSquareClicked);
        boardPanel.setClickable(square -> tournament.getStatus() == TournamentStatus.IN_PROGRESS
                && !tournament.isAwaitingOpponent()
                && (selector.getTargets().contains(square) || !ownLegalMoves(square).isEmpty()));
        frame = new JFrame("Torneo escalera — " + playerName);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leave(); // la X pregunta lo mismo que "Volver al menú"
            }

            @Override
            public void windowClosed(WindowEvent e) {
                stopPendingReply();
                onClose.run();
            }
        });
        frame.setContentPane(GameLayout.content(boardPanel, "Clic en una de tus piezas para ver a dónde puede ir. "
                + "Con el teclado: flechas y Enter.", sidePanel()));

        tournament.start();
        log.clear();
        refresh("Empezás vos, con blancas.");
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * La hoja lateral: arriba la ronda y la escalera; después el rival (del lado de
     * las negras, arriba como en el tablero), la planilla, vos y las acciones.
     */
    private JPanel sidePanel() {
        ladder.setLayout(new BoxLayout(ladder, BoxLayout.Y_AXIS));
        ladder.setOpaque(false);

        JPanel header = GameLayout.stack(8);
        header.add(roundTitle, BorderLayout.NORTH);
        header.add(ladder, BorderLayout.CENTER);

        JPanel rival = GameLayout.stack(6);
        rival.add(rivalStrip, BorderLayout.NORTH);
        rival.add(rivalInfo, BorderLayout.CENTER);

        JPanel top = GameLayout.stack(16);
        top.add(header, BorderLayout.NORTH);
        top.add(rival, BorderLayout.CENTER);

        JPanel sheet = GameLayout.stack(8);
        sheet.add(Theme.heading("Planilla"), BorderLayout.NORTH);
        sheet.add(moveList, BorderLayout.CENTER);
        sheet.add(messageLabel, BorderLayout.SOUTH);

        JButton restart = Theme.button("Reiniciar torneo", "Vuelve a la ronda 1");
        restart.addActionListener(e -> restart());
        JButton history = Theme.button("Mi historial", "Tus rondas guardadas");
        history.addActionListener(e -> historyDialog.show(frame));
        JButton back = Theme.linkButton("Volver al menú", "Abandona el torneo");
        back.addActionListener(e -> leave());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(restart);
        buttons.add(history);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(back, BorderLayout.WEST);

        JPanel bottom = GameLayout.stack(12);
        bottom.add(userStrip, BorderLayout.NORTH);
        bottom.add(buttons, BorderLayout.CENTER);
        bottom.add(footer, BorderLayout.SOUTH);

        JPanel side = GameLayout.stack(16);
        side.add(top, BorderLayout.NORTH);
        side.add(sheet, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private void onSquareClicked(Position square) {
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS || tournament.isAwaitingOpponent()) {
            return;
        }
        selector.click(square, ownLegalMoves(square)).ifPresentOrElse(this::play, () -> refresh(null));
    }

    private void play(Move move) {
        // Se guarda antes de jugar: si la ronda termina, el torneo ya pasa al rival siguiente.
        HistoricalPlayer rival = tournament.getCurrentRival();
        Piece moved = tournament.getBoard().pieceAt(move.from());
        Piece target = tournament.getBoard().pieceAt(move.to());
        TurnReport report = tournament.playUserMove(move.from(), move.to());
        if (!report.userResult().isSuccess()) {
            refresh(report.userResult().getMessage());
            return;
        }
        log.record(move, moved, target, report.outcome() == RoundOutcome.WON ? "#" : checkSuffix());
        if (!tournament.isAwaitingOpponent()) {
            finishRound(report.outcome(), rival);
            return;
        }
        refresh("Jugaste " + text(move) + ".");
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        opponentTimer = new Timer(opponentDelayMillis, e -> replyFromOpponent(rival));
        opponentTimer.setRepeats(false);
        opponentTimer.start();
    }

    private void replyFromOpponent(HistoricalPlayer rival) {
        opponentTimer = null;
        frame.setCursor(Cursor.getDefaultCursor());
        Map<Position, Piece> before = snapshot(tournament.getBoard()); // para saber qué capturó la IA
        TurnReport reply = tournament.playOpponentMove();
        Move move = reply.aiReply();
        log.record(move, before.get(move.from()), before.get(move.to()),
                reply.outcome() == RoundOutcome.LOST ? "#" : checkSuffix());
        if (reply.outcome().isFinished()) {
            finishRound(reply.outcome(), rival);
        } else {
            refresh(rival.name() + " jugó " + text(move) + ". Te toca.");
            // La jugada del rival se desliza: es la que no hiciste vos y tenés que poder seguirla.
            boardPanel.animateMove(move, before.get(move.from()), before.get(move.to()));
        }
    }

    /** Avisa cómo terminó la ronda. Si se pasa a otra ronda (o se repite), la planilla arranca de cero. */
    private void finishRound(RoundOutcome outcome, HistoricalPlayer rival) {
        String message = switch (outcome) {
            case WON -> tournament.getStatus() == TournamentStatus.CHAMPION
                    ? "¡Jaque mate a " + rival.name() + "! Ganaste las " + tournament.getTotalRounds()
                      + " rondas: sos campeón del torneo."
                    : "¡Jaque mate! Le ganaste a " + rival.name() + ".\nPasás a la ronda "
                      + tournament.getCurrentRound() + " contra " + tournament.getCurrentRival().name() + ".";
            case LOST -> "¡Jaque mate! " + rival.name() + " te ganó.\nQuedaste eliminado en la ronda "
                    + tournament.getCurrentRound() + ".";
            case DRAWN -> "Rey ahogado: tablas.\nSe repite la ronda " + tournament.getCurrentRound()
                    + " contra " + rival.name() + ".";
            case ONGOING -> "";
        };
        boolean newGameStarted = outcome == RoundOutcome.DRAWN
                || (outcome == RoundOutcome.WON && tournament.getStatus() == TournamentStatus.IN_PROGRESS);
        if (newGameStarted) {
            log.clear();
        }
        refresh(message.lines().findFirst().orElse(""));
        JOptionPane.showMessageDialog(frame, message, "Torneo", outcome == RoundOutcome.LOST
                ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    private void restart() {
        if (tournamentStarted() && !Theme.confirm(frame, "Reiniciar torneo",
                "Vas a volver a la ronda 1 y perder el avance de este torneo. "
                        + "Las rondas terminadas siguen en tu historial.", "Reiniciar")) {
            return;
        }
        stopPendingReply();
        tournament.start();
        log.clear();
        refresh("Torneo reiniciado.");
    }

    /** Cierra la ventana; si el torneo está a medias, primero pregunta. */
    private void leave() {
        if (tournamentStarted() && !Theme.confirm(frame, "Abandonar torneo",
                "Si volvés al menú, abandonás el torneo y la próxima vez empieza desde la ronda 1. "
                        + "Las rondas terminadas siguen en tu historial.", "Abandonar torneo")) {
            return;
        }
        frame.dispose();
    }

    /** Hay avance que se perdería: el torneo sigue y ya se jugó algo. */
    private boolean tournamentStarted() {
        return tournament.getStatus() == TournamentStatus.IN_PROGRESS
                && (tournament.getCurrentRound() > 1 || !log.entries().isEmpty());
    }

    private void stopPendingReply() {
        if (opponentTimer != null) {
            opponentTimer.stop(); // descarta la respuesta pendiente de la partida anterior
            opponentTimer = null;
            frame.setCursor(Cursor.getDefaultCursor());
        }
    }

    /** "+" si el bando que ahora tiene el turno quedó en jaque. */
    private String checkSuffix() {
        return tournament.getGameStatus() == GameStatus.CHECK ? "+" : "";
    }

    private Set<Position> ownLegalMoves(Position square) {
        Piece piece = tournament.getBoard().pieceAt(square);
        return piece != null && piece.getColor() == tournament.getUserColor() ? tournament.legalMoves(square) : Set.of();
    }

    /** Redibuja todo. Con message == null solo cambió la selección. */
    private void refresh(String message) {
        if (message != null) {
            selector.clear();
            messageLabel.setText(message);
        }
        GameStatus gameStatus = tournament.getGameStatus();
        TournamentStatus status = tournament.getStatus();
        boolean awaiting = tournament.isAwaitingOpponent();
        Color toMove = awaiting ? tournament.getUserColor().opposite() : tournament.getUserColor();
        boolean inCheck = gameStatus == GameStatus.CHECK || gameStatus == GameStatus.CHECKMATE;
        boardPanel.showBoard(tournament.getBoard(), selector.getSelected(), selector.getTargets(),
                log.lastMove(), inCheck ? GameWindow.kingOf(tournament.getBoard(), toMove) : null);

        refreshLadder();
        HistoricalPlayer rival = tournament.getCurrentRival();
        switch (status) {
            case CHAMPION -> setRoundTitle("Campeón del torneo", Theme.CHAMPION);
            case ELIMINATED -> setRoundTitle("Eliminado en la ronda " + tournament.getCurrentRound(), Theme.DANGER);
            default -> setRoundTitle("Ronda " + tournament.getCurrentRound() + " de " + tournament.getTotalRounds(),
                    Theme.TEXT);
        }

        boolean playing = status == TournamentStatus.IN_PROGRESS;
        rivalStrip.setPlayerName(rival.name());
        rivalStrip.setActive(playing && awaiting);
        rivalStrip.setNote(awaiting ? "Está pensando…" : rival.style().getDisplayName() + ", dificultad "
                + rival.difficulty().getDisplayName().toLowerCase(), null);
        rivalInfo.setText(rival.style().getDescription() + " Juega al azar el "
                + Math.round(rival.difficulty().randomMoveProbability() * 100) + "% de las veces.");

        userStrip.setActive(playing && !awaiting);
        if (status == TournamentStatus.CHAMPION) {
            userStrip.setNote("Ganaste las " + tournament.getTotalRounds() + " rondas", Theme.CHAMPION);
        } else if (status == TournamentStatus.ELIMINATED) {
            userStrip.setNote("Quedaste eliminado", Theme.DANGER);
        } else if (!awaiting && gameStatus == GameStatus.CHECK) {
            userStrip.setNote("¡En jaque! Te toca", new java.awt.Color(0xFFD2CB));
        } else {
            userStrip.setNote(awaiting ? "" : "Te toca mover", null);
        }
        userStrip.setCaptured(log.capturedPiecesOf(tournament.getUserColor().opposite()));
        rivalStrip.setCaptured(log.capturedPiecesOf(tournament.getUserColor()));
        moveList.show(log);
    }

    private void setRoundTitle(String text, java.awt.Color color) {
        roundTitle.setText(text);
        roundTitle.setForeground(color);
    }

    /**
     * La escalera: una fila por ronda con su marca (ganada, jugando, perdida o
     * pendiente). Es una secuencia real, por eso va numerada.
     */
    private void refreshLadder() {
        ladder.removeAll();
        List<HistoricalPlayer> rivals = tournament.getRivals();
        int current = tournament.getCurrentRound() - 1;
        for (int i = 0; i < rivals.size(); i++) {
            HistoricalPlayer rival = rivals.get(i);
            Step step;
            if (i < current || (i == current && tournament.getStatus() == TournamentStatus.CHAMPION)) {
                step = Step.WON;
            } else if (i == current && tournament.getStatus() == TournamentStatus.ELIMINATED) {
                step = Step.LOST;
            } else if (i == current) {
                step = Step.CURRENT;
            } else {
                step = Step.PENDING;
            }
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            JLabel name = Theme.label((i + 1) + "  " + rival.name(),
                    step == Step.CURRENT ? Theme.BODY_STRONG : Theme.BODY,
                    step == Step.PENDING ? Theme.TEXT_MUTED : Theme.TEXT);
            JLabel state = Theme.label(step.label, Theme.SMALL, step.color);
            row.add(new StepMarker(step), BorderLayout.WEST);
            row.add(name, BorderLayout.CENTER);
            row.add(state, BorderLayout.EAST);
            row.getAccessibleContext().setAccessibleName("Ronda " + (i + 1) + ", " + rival.name() + ": " + step.label);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            ladder.add(row);
        }
        ladder.revalidate();
        ladder.repaint();
    }

    private enum Step {
        WON("Ganada", Theme.ACCENT), CURRENT("Jugando", Theme.ACCENT),
        LOST("Perdida", Theme.DANGER), PENDING("", Theme.TEXT_MUTED);

        final String label;
        final java.awt.Color color;

        Step(String label, java.awt.Color color) {
            this.label = label;
            this.color = color;
        }
    }

    /** El punto de cada escalón: lleno si se ganó o perdió, anillo si se está jugando, hueco si falta. */
    private static final class StepMarker extends JComponent {
        private final Step step;

        StepMarker(Step step) {
            this.step = step;
            setPreferredSize(new Dimension(14, 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = 12;
            int x = 1;
            int y = (getHeight() - size) / 2;
            switch (step) {
                case WON, LOST -> {
                    g.setColor(step.color);
                    g.fillOval(x, y, size, size);
                }
                case CURRENT -> {
                    g.setColor(Theme.ACCENT);
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(x + 1, y + 1, size - 2, size - 2);
                    g.fillOval(x + 4, y + 4, size - 8, size - 8);
                }
                case PENDING -> {
                    g.setColor(Theme.BORDER);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawOval(x + 1, y + 1, size - 2, size - 2);
                }
            }
            g.dispose();
        }
    }

    private static Map<Position, Piece> snapshot(BoardView board) {
        Map<Position, Piece> pieces = new HashMap<>();
        for (int row = 0; row < board.rows(); row++) {
            for (int column = 0; column < board.columns(); column++) {
                Position p = new Position(row, column);
                if (board.pieceAt(p) != null) {
                    pieces.put(p, board.pieceAt(p));
                }
            }
        }
        return pieces;
    }

    private String text(Move move) {
        return notation.toText(move.from()) + "-" + notation.toText(move.to());
    }
}
