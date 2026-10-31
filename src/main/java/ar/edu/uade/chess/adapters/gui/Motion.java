package ar.edu.uade.chess.adapters.gui;

import javax.swing.Timer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

/**
 * Animaciones cortas de la interfaz. Es solo presentación.
 *
 * Reglas que sigue la interfaz:
 *  - Solo se anima lo que ayuda a entender (la jugada de la IA) o confirma una acción
 *    (presionar una tarjeta). Lo que se hace muchas veces o con el teclado NO se anima.
 *  - Duraciones por debajo de 300 ms y curvas "fuertes": las de fábrica se sienten flojas.
 *  - Una animación nueva interrumpe a la anterior y arranca desde donde estaba, sin saltos.
 */
public final class Motion {

    private Motion() { }

    /** Para lo que entra o responde a la persona: arranca rápido. */
    public static final DoubleUnaryOperator EASE_OUT = cubicBezier(0.23, 1, 0.32, 1);
    /** Para algo que se mueve de un lugar a otro de la pantalla (una pieza). */
    public static final DoubleUnaryOperator EASE_IN_OUT = cubicBezier(0.77, 0, 0.175, 1);

    private static final int FRAME_MILLIS = 16; // ~60 cuadros por segundo

    /**
     * La misma curva que cubic-bezier(x1, y1, x2, y2) de CSS: recibe el tiempo (0 a 1)
     * y devuelve el avance (0 a 1).
     */
    public static DoubleUnaryOperator cubicBezier(double x1, double y1, double x2, double y2) {
        return t -> {
            if (t <= 0) {
                return 0;
            }
            if (t >= 1) {
                return 1;
            }
            // Busca (por bisección) el parámetro s cuya x es t, y devuelve su y.
            double low = 0;
            double high = 1;
            double s = t;
            for (int i = 0; i < 30; i++) {
                s = (low + high) / 2;
                if (bezier(s, x1, x2) < t) {
                    low = s;
                } else {
                    high = s;
                }
            }
            return bezier(s, y1, y2);
        };
    }

    private static double bezier(double s, double p1, double p2) {
        double inverse = 1 - s;
        return 3 * inverse * inverse * s * p1 + 3 * inverse * s * s * p2 + s * s * s;
    }

    /**
     * Un valor que se anima de donde está hacia un destino. Si se le pide otro destino
     * a mitad de camino, sigue desde el valor actual (se puede interrumpir sin saltos).
     */
    public static final class Tween {

        private final DoubleConsumer onFrame;
        private double value;
        private Timer timer;

        public Tween(double initial, DoubleConsumer onFrame) {
            this.value = initial;
            this.onFrame = onFrame;
        }

        public double value() { return value; }

        public void animateTo(double target, int durationMillis, DoubleUnaryOperator easing) {
            stop();
            double from = value;
            long start = System.nanoTime();
            timer = new Timer(FRAME_MILLIS, e -> {
                double t = Math.min(1, (System.nanoTime() - start) / 1_000_000.0 / durationMillis);
                value = from + (target - from) * easing.applyAsDouble(t);
                onFrame.accept(value);
                if (t >= 1) {
                    stop();
                }
            });
            timer.setInitialDelay(0);
            timer.start();
        }

        public void stop() {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }
    }
}
