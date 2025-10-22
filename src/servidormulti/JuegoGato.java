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
