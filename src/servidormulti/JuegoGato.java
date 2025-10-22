package servidormulti;

import java.util.Arrays;
import java.util.Random;

public class JuegoGato {
    public final String jugadorX;
    public final String jugadorO;
    private String turnoActual;
    private final char[] tablero;

    public JuegoGato(String retador, String retado) {
        this.tablero = new char[9];
        Arrays.fill(this.tablero, ' ');

        Random random = new Random();
        if (random.nextBoolean()) {
            this.jugadorX = retador;
            this.jugadorO = retado;
        } else {
            this.jugadorX = retado;
            this.jugadorO = retador;
        }
        this.turnoActual = this.jugadorX;
    }

    public String getTurnoActual() {
        return turnoActual;
    }

    public String getOponente(String username) {
        return username.equals(jugadorX) ? jugadorO : jugadorX;
    }

    // Devuelve 'X' u 'O' para el jugador
    public char getMarca(String username) {
        return username.equals(jugadorX) ? 'X' : 'O';
    }

    public String dibujarTablero() {
        StringBuilder sb = new StringBuilder("\n--- Tablero ---\n");
        for (int i = 0; i < 9; i += 3) {
            sb.append(" ").append(tablero[i]).append(" | ").append(tablero[i+1]).append(" | ").append(tablero[i+2]).append("  ");
            sb.append("[").append(i).append("][").append(i+1).append("][").append(i+2).append("]\n");
            if (i < 6) sb.append("---|---|---\n");
        }
        sb.append("---------------\n");
        sb.append("Tú eres '").append(getMarca(turnoActual)).append("'. Turno de: ").append(turnoActual).append("\n");
        return sb.toString();
    }

    /**
     * @param posicion 0-8
     * @param jugador El username que intenta mover
     * @return 0:OK, 1:No es tu turno, 2:Posición ocupada, 3:Posición inválida
     */
    public int realizarMovimiento(int posicion, String jugador) {
        if (!jugador.equals(turnoActual)) return 1;
        if (posicion < 0 || posicion >= 9) return 3;
        if (tablero[posicion] != ' ') return 2;

        tablero[posicion] = getMarca(jugador);
        verificarEstado();

        // Cambiar turno si no ha terminado
        if (!hayGanador() && !hayEmpate()) {
            turnoActual = getOponente(jugador);
        }
        return 0;
    }
