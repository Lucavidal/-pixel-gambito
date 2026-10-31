package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.adapters.console.AlgebraicNotation;
import ar.edu.uade.chess.core.auth.IAuthService;
import ar.edu.uade.chess.core.auth.User;
import ar.edu.uade.chess.core.game.IGame;
import ar.edu.uade.chess.core.history.IHistoryService;
import ar.edu.uade.chess.core.tournament.HistoricalPlayer;
import ar.edu.uade.chess.core.tournament.ITournament;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Arma la navegación de la interfaz gráfica: inicio de sesión → menú principal →
 * partida libre / torneo / historial → volver al menú → cerrar sesión.
 *
 * Recibe por constructor los puertos (IAuthService, IHistoryService) y las fábricas
 * de partidas y torneos: no crea nada del núcleo por su cuenta. Main es quien conecta.
 */
public class GuiApplication {

    private final IAuthService auth;
    private final IHistoryService history;
    private final Supplier<IGame> freeGames;
    private final Function<User, ITournament> tournaments;
    private final List<HistoricalPlayer> roster;
    private final int aiDelayMillis;
    private final PieceGlyphs glyphs = new PieceGlyphs();
    private final AlgebraicNotation notation = new AlgebraicNotation();

    /**
     * @param freeGames     fábrica de partidas libres (una nueva por partida)
     * @param tournaments   arma el torneo de un usuario (con su historial)
     * @param roster        rivales del torneo, para describirlos en el menú
     * @param aiDelayMillis cuánto "piensa" la IA antes de mostrar su jugada
     */
    public GuiApplication(IAuthService auth, IHistoryService history, Supplier<IGame> freeGames,
                          Function<User, ITournament> tournaments, List<HistoricalPlayer> roster, int aiDelayMillis) {
        this.auth = auth;
        this.history = history;
        this.freeGames = freeGames;
        this.tournaments = tournaments;
        this.roster = roster;
        this.aiDelayMillis = aiDelayMillis;
    }

    /** @param openTournament true = después de iniciar sesión, abrir directamente el torneo */
    public void start(boolean openTournament) {
        SwingUtilities.invokeLater(() -> {
            useSystemLookAndFeel();
            tuneTooltips();
            Theme.trackInputMode();
            showErrorsOnScreen();
            loginAndShowMenu(openTournament);
        });
    }

    private void loginAndShowMenu(boolean openTournament) {
        Optional<User> user = new LoginDialog(auth).ask();
        if (user.isEmpty()) {
            System.exit(0);
        }
        MainMenuWindow[] menu = new MainMenuWindow[1];
        menu[0] = new MainMenuWindow(user.get(), history, roster, new MainMenuWindow.Actions(
                () -> openFreeGame(user.get(), menu[0]),
                () -> openTournament(user.get(), menu[0]),
                () -> new HistoryDialog(history, user.get()).show(menu[0].frame()),
                () -> new FeaturesDialog(roster).show(menu[0].frame()),
                () -> {
                    menu[0].close();
                    loginAndShowMenu(false);
                }));
        menu[0].show();
        if (openTournament) {
            openTournament(user.get(), menu[0]);
        }
    }

    private void openFreeGame(User user, MainMenuWindow menu) {
        menu.hide();
        new GameWindow(freeGames, glyphs, new MoveLog(notation), user.visibleName(), menu::showAgain).show();
    }

    private void openTournament(User user, MainMenuWindow menu) {
        menu.hide();
        new TournamentWindow(tournaments.apply(user), glyphs, notation, new MoveLog(notation), aiDelayMillis,
                user.visibleName(), new HistoryDialog(history, user), menu::showAgain).show();
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // si no se puede, Swing usa su aspecto por defecto: no es un error
        }
    }

    /**
     * La primera ayuda espera un poco (para no saltar al pasar el mouse de casualidad);
     * si ya hay una abierta, la del botón de al lado aparece al instante. Y se queda
     * el tiempo suficiente para leerla.
     */
    private static void tuneTooltips() {
        ToolTipManager tooltips = ToolTipManager.sharedInstance();
        tooltips.setInitialDelay(450);
        tooltips.setReshowDelay(1500);
        tooltips.setDismissDelay(10_000);
    }

    /**
     * Sin consola (por ejemplo, con javaw o doble clic) un error no se vería nunca:
     * se muestra en una ventana, además de imprimirse.
     */
    private static void showErrorsOnScreen() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            JOptionPane.showMessageDialog(null,
                    "Ocurrió un error inesperado:\n" + error.getMessage()
                            + "\n\nSi vuelve a pasar, cerrá el programa y abrilo de nuevo."
                            + "\n\nDetalle: " + cause,
                    "Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}
