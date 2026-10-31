package ar.edu.uade.chess.adapters.gui;

import ar.edu.uade.chess.core.auth.AuthResult;
import ar.edu.uade.chess.core.auth.AuthService;
import ar.edu.uade.chess.core.auth.IAuthService;
import ar.edu.uade.chess.core.auth.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Optional;

/**
 * ADAPTADOR DE ENTRADA gráfico: inicio de sesión (ventana modal) con dos modos,
 * "Iniciar sesión" y "Crear cuenta". Usa solo el puerto IAuthService.
 *
 * A la izquierda, la columna de la marca (verde paño, con un tablerito); a la
 * derecha, el formulario con las etiquetas arriba de cada campo.
 *
 * Si alguien intenta entrar con un usuario que no existe, se lo lleva a "Crear cuenta"
 * con el nombre ya escrito, en vez de decirle solo "incorrecto".
 * Las contraseñas se escriben ocultas (JPasswordField) y se borran de memoria al usarlas.
 */
public class LoginDialog {

    private static final int LOGIN_TAB = 0;
    private static final int REGISTER_TAB = 1;
    private static final String[] TAB_NAMES = {"login", "register"};

    private final IAuthService auth;

    private JDialog dialog;
    private final CardLayout forms = new CardLayout();
    private final JPanel formCards = new JPanel(forms);
    private final SegmentButton loginSegment = new SegmentButton("Iniciar sesión");
    private final SegmentButton registerSegment = new SegmentButton("Crear cuenta");
    private final JTextField loginUser = new JTextField(20);
    private final JPasswordField loginPassword = new JPasswordField(20);
    private final JTextField newUser = new JTextField(20);
    private final JTextField newDisplayName = new JTextField(20);
    private final JPasswordField newPassword = new JPasswordField(20);
    private final JPasswordField repeatPassword = new JPasswordField(20);
    private final JButton loginButton = Theme.primaryButton("Ingresar", null);
    private final JButton registerButton = Theme.primaryButton("Crear cuenta y entrar", null);
    private final Paragraph message = new Paragraph(" ", Theme.BODY_STRONG, Theme.DANGER, 360);
    private User loggedIn;

    public LoginDialog(IAuthService auth) {
        this.auth = auth;
    }

    /** Muestra la ventana y espera. Devuelve el usuario, o vacío si la persona la cerró. */
    public Optional<User> ask() {
        dialog = new JDialog((Frame) null, "Ajedrez — Iniciar sesión", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());
        loginSegment.addActionListener(e -> selectTab(LOGIN_TAB));
        registerSegment.addActionListener(e -> selectTab(REGISTER_TAB));

        JPanel segments = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        segments.setOpaque(false);
        segments.add(loginSegment);
        segments.add(registerSegment);
        segments.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        formCards.setOpaque(false);
        formCards.add(loginForm(), TAB_NAMES[LOGIN_TAB]);
        formCards.add(registerForm(), TAB_NAMES[REGISTER_TAB]);


        JPanel form = new JPanel(new BorderLayout(0, 18));
        form.setBackground(Theme.CARD);
        form.setBorder(BorderFactory.createEmptyBorder(28, 32, 20, 32));
        form.add(segments, BorderLayout.NORTH);
        form.add(formCards, BorderLayout.CENTER);
        JPanel messageArea = new JPanel(new BorderLayout()); // alto fijo: el error no empuja el formulario
        messageArea.setOpaque(false);
        messageArea.setPreferredSize(new Dimension(360, 42));
        messageArea.add(message, BorderLayout.NORTH);
        form.add(messageArea, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.add(new BrandPanel(), BorderLayout.WEST);
        content.add(form, BorderLayout.CENTER);

        dialog.setContentPane(content);
        selectTab(LOGIN_TAB);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                dialog.toFront();                 // que no quede escondida detrás de otra ventana
                loginUser.requestFocusInWindow();
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true); // modal: se queda acá hasta que se cierre
        return Optional.ofNullable(loggedIn);
    }

    private JPanel loginForm() {
        JPanel panel = form();
        field(panel, "Usuario", loginUser);
        field(panel, "Contraseña", loginPassword);
        panel.add(hint("¿Primera vez? Elegí \"Crear cuenta\" arriba."));
        actions(panel, loginButton);
        return panel;
    }

    private JPanel registerForm() {
        JPanel panel = form();
        field(panel, "Usuario", newUser);
        panel.add(hint("De " + AuthService.MIN_USERNAME_LENGTH + " a " + AuthService.MAX_USERNAME_LENGTH
                + " caracteres: letras, números, espacios, . _ - @"));
        panel.add(Box.createVerticalStrut(10));
        field(panel, "¿Cómo te llamamos? (opcional)", newDisplayName);
        panel.add(hint("Hasta " + AuthService.MAX_DISPLAY_NAME_LENGTH + " caracteres. Es el nombre con el que te saludamos."));
        panel.add(Box.createVerticalStrut(10));
        field(panel, "Contraseña", newPassword);
        panel.add(hint("Al menos " + AuthService.MIN_PASSWORD_LENGTH + " caracteres."));
        panel.add(Box.createVerticalStrut(10));
        field(panel, "Repetir contraseña", repeatPassword);
        actions(panel, registerButton);
        return panel;
    }

    private void login() {
        String username = loginUser.getText();
        if (username.isBlank()) {
            showError("Escribí tu usuario para ingresar.", loginUser);
            return;
        }
        char[] password = loginPassword.getPassword();
        Optional<User> user = auth.login(username, new String(password));
        Arrays.fill(password, '\0');
        if (user.isPresent()) {
            finish(user.get());
        } else if (!auth.isRegistered(username)) {
            newUser.setText(username.trim());
            selectTab(REGISTER_TAB);
            showError(AuthResult.UNKNOWN_USER.getMessage(), newPassword);
        } else {
            loginPassword.setText("");
            showError(AuthResult.WRONG_PASSWORD.getMessage() + " Volvé a escribirla.", loginPassword);
        }
    }

    private void register() {
        char[] password = newPassword.getPassword();
        char[] repeated = repeatPassword.getPassword();
        boolean same = Arrays.equals(password, repeated);
        String text = new String(password);
        Arrays.fill(password, '\0');
        Arrays.fill(repeated, '\0');
        if (!same) {
            repeatPassword.setText("");
            showError("Las contraseñas no coinciden. Escribí la misma contraseña otra vez.", repeatPassword);
            return;
        }
        AuthResult result = auth.register(newUser.getText(), text, newDisplayName.getText());
        if (result.isSuccess()) {
            auth.login(newUser.getText(), text).ifPresent(this::finish);
        } else if (result == AuthResult.USERNAME_TAKEN) {
            loginUser.setText(newUser.getText().trim());
            selectTab(LOGIN_TAB);
            showError(result.getMessage(), loginPassword);
        } else if (result == AuthResult.DISPLAY_NAME_TOO_LONG) {
            showError(result.getMessage(), newDisplayName);
        } else if (result == AuthResult.PASSWORD_TOO_SHORT) {
            newPassword.setText("");
            repeatPassword.setText("");
            showError(result.getMessage(), newPassword);
        } else {
            showError(result.getMessage(), newUser);
        }
    }

    private void finish(User user) {
        loggedIn = user;
        dialog.dispose();
    }

    /** Cambia de modo: muestra su formulario, limpia el error y ajusta qué botón responde al Enter. */
    private void selectTab(int tab) {
        forms.show(formCards, TAB_NAMES[tab]);
        loginSegment.setSelected(tab == LOGIN_TAB);
        registerSegment.setSelected(tab == REGISTER_TAB);
        message.setText(" ");
        clearErrorDescriptions();
        dialog.getRootPane().setDefaultButton(tab == LOGIN_TAB ? loginButton : registerButton);
        SwingUtilities.invokeLater(() -> (tab == LOGIN_TAB ? loginUser : newUser).requestFocusInWindow());
    }

    /**
     * Muestra el error y lleva el foco al campo que hay que corregir. El texto también
     * queda como descripción del campo, así un lector de pantalla lo lee al llegar.
     */
    private void showError(String text, JComponent field) {
        clearErrorDescriptions();
        message.setText(text);
        field.getAccessibleContext().setAccessibleDescription(text);
        // Al cambiar de modo, el foco va al primer campo; pidiéndolo "más tarde"
        // (después de ese cambio) el foco queda en el campo del error.
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            field.requestFocusInWindow();
            if (field instanceof JTextField textField) {
                textField.selectAll();
            }
        }));
    }

    private void clearErrorDescriptions() {
        for (JComponent field : new JComponent[]{loginUser, loginPassword, newUser, newDisplayName, newPassword, repeatPassword}) {
            field.getAccessibleContext().setAccessibleDescription(null);
        }
    }

    private static JPanel form() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    /** Etiqueta arriba y campo abajo, a todo el ancho: se lee de un vistazo. */
    private static void field(JPanel panel, String label, JTextField field) {
        JLabel text = Theme.label(label, Theme.BODY_STRONG, Theme.TEXT);
        text.setLabelFor(field); // el campo toma este texto como nombre accesible
        text.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        Theme.styleField(field);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        panel.add(text);
        panel.add(field);
        panel.add(Box.createVerticalStrut(10));
    }

    private static JLabel hint(String text) {
        JLabel label = Theme.label(text, Theme.SMALL, Theme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return label;
    }

    private static void actions(JPanel panel, JButton button) {
        panel.add(Box.createVerticalStrut(8));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(button);
    }

    /** Pestaña del selector de modo: subrayado verde la elegida, gris la otra. */
    private static final class SegmentButton extends JButton {
        private boolean chosen;

        SegmentButton(String text) {
            super(text);
            setFont(Theme.BODY_STRONG);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 0, 10, 22));
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
        public void setSelected(boolean selected) {
            chosen = selected;
            getAccessibleContext().setAccessibleDescription(selected ? "Elegido" : null);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont());
            FontMetrics metrics = g.getFontMetrics();
            int textWidth = metrics.stringWidth(getText());
            int y = getInsets().top + metrics.getAscent();
            g.setColor(chosen ? Theme.TEXT : getModel().isRollover() ? Theme.ACCENT : Theme.TEXT_MUTED);
            g.drawString(getText(), 0, y);
            if (chosen) {
                g.setColor(Theme.ACCENT);
                g.fillRect(0, getHeight() - 3, textWidth, 3);
            }
            if (Theme.showsFocus(this)) {
                g.setColor(Theme.FOCUS);
                g.setStroke(new BasicStroke(2));
                g.drawRect(-1 + 2, 2, textWidth + 4, getHeight() - 6);
            }
            g.dispose();
        }
    }

    /** La columna verde de la izquierda: un tablerito, el nombre y para qué sirve la cuenta. */
    private static final class BrandPanel extends JPanel {
        private static final int SQUARE = 20;
        private static final int SQUARES = 4;

        BrandPanel() {
            setLayout(new BorderLayout());
            setBackground(Theme.ACCENT);
            setPreferredSize(new Dimension(270, 430));
            setBorder(BorderFactory.createEmptyBorder(28 + SQUARE * SQUARES + 26, 28, 24, 28));

            JPanel texts = new JPanel();
            texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
            texts.setOpaque(false);
            JLabel title = Theme.label("Ajedrez", Theme.HERO, Theme.ON_ACCENT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            Paragraph body = new Paragraph("Partidas entre dos personas y un torneo escalera contra "
                    + "la computadora. Tus rondas quedan guardadas en tu cuenta.", Theme.BODY, new Color(0xC9DCCF), 214);
            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            texts.add(title);
            texts.add(body);
            add(texts, BorderLayout.NORTH);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int x0 = 28;
            int y0 = 28;
            for (int row = 0; row < SQUARES; row++) {
                for (int column = 0; column < SQUARES; column++) {
                    g.setColor((row + column) % 2 == 0 ? Theme.LIGHT_SQUARE : Theme.DARK_SQUARE);
                    g.fillRect(x0 + column * SQUARE, y0 + row * SQUARE, SQUARE, SQUARE);
                }
            }
            // un caballo negro sobre el tablerito
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, SQUARE - 3));
            g.setColor(Theme.TEXT);
            FontMetrics metrics = g.getFontMetrics();
            String knight = "♞";
            int cx = x0 + 2 * SQUARE + (SQUARE - metrics.stringWidth(knight)) / 2;
            int cy = y0 + SQUARE + (SQUARE - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(knight, cx, cy);
            g.dispose();
        }
    }
}
