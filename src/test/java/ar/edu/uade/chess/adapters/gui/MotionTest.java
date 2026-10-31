package ar.edu.uade.chess.adapters.gui;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** Las curvas de las animaciones: empiezan en 0, terminan en 1 y nunca retroceden. */
class MotionTest {

    @Test
    void curves_StartAtZero_AndEndAtOne() {
        for (DoubleUnaryOperator curve : new DoubleUnaryOperator[]{Motion.EASE_OUT, Motion.EASE_IN_OUT}) {
            assertEquals(0, curve.applyAsDouble(0), 1e-9);
            assertEquals(1, curve.applyAsDouble(1), 1e-9);
        }
    }

    @Test
    void curves_NeverGoBackwards() {
        for (DoubleUnaryOperator curve : new DoubleUnaryOperator[]{Motion.EASE_OUT, Motion.EASE_IN_OUT}) {
            double previous = 0;
            for (int i = 1; i <= 100; i++) {
                double value = curve.applyAsDouble(i / 100.0);
                assertTrue(value >= previous - 1e-9, "retrocede en t=" + i / 100.0);
                previous = value;
            }
        }
    }

    @Test
    void easeOut_StartsFast_AndEaseInOut_StartsAndEndsSlow() {
        // ease-out: a un cuarto del tiempo ya hizo más de la mitad del camino
        assertTrue(Motion.EASE_OUT.applyAsDouble(0.25) > 0.5);
        // ease-in-out: arranca y frena despacio (la pieza no "salta" ni choca contra la casilla)
        assertTrue(Motion.EASE_IN_OUT.applyAsDouble(0.1) < 0.1);
        assertTrue(Motion.EASE_IN_OUT.applyAsDouble(0.9) > 0.9);
    }

    @Test
    void linearBezier_IsTheIdentity() {
        DoubleUnaryOperator linear = Motion.cubicBezier(0, 0, 1, 1);
        assertEquals(0.3, linear.applyAsDouble(0.3), 1e-6);
        assertEquals(0.8, linear.applyAsDouble(0.8), 1e-6);
    }
}
