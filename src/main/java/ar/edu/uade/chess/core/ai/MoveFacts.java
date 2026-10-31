package ar.edu.uade.chess.core.ai;

/**
 * Lo que se sabe de una jugada después de simularla. Los estilos de juego
 * solo ponderan estos datos; ninguno vuelve a simular nada.
 *
 * @param capturedValue  valor de la pieza capturada (0 si no captura)
 * @param givesCheck     si deja al rey rival en jaque
 * @param givesCheckmate si deja al rival en jaque mate
 * @param hangingValue   suma del valor de las piezas propias que el rival puede capturar legalmente después de la jugada
 * @param kingShelter    cantidad de piezas propias en las casillas vecinas al rey propio
 */
public record MoveFacts(int capturedValue,
                        boolean givesCheck,
                        boolean givesCheckmate,
                        int hangingValue,
                        int kingShelter) {
}
