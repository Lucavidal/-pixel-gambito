package ar.edu.uade.chess.adapters;

/**
 * La marca del juego para las interfaces Java (la ventana Swing). Es la misma que web/js/brand.js:
 * si cambia el nombre, el año o el mail, se cambia en los dos lugares (Java no puede leer el .js).
 */
public final class Brand {

    public static final String NAME = "Pixel Gambito";
    public static final String EMAIL = "pixelgambito.contacto@gmail.com";
    public static final int YEAR = 2026;

    private Brand() { }

    /** La línea del pie, igual que el footer de la web en español. */
    public static String copyrightLine() {
        return "© " + YEAR + " " + NAME + " · TPO Ingeniería de Software · UADE · Contacto: " + EMAIL;
    }
}
