package clientemulti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ParaMandar implements Runnable {
    private final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    private final DataOutputStream salida;
    private final Socket socket;

    public ParaMandar(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
    }

    private void mostrarAyuda() {
        System.out.println("\n--- Comandos disponibles ---");
        System.out.println(" Escribe 'login' o 'registrar' para iniciar sesión.");
        System.out.println(" Usa: /bloquear [usuario] | /desbloquear [usuario]");
        System.out.println(" Usa: /verbloqueados");
        System.out.println(" Usa: @[usuario] [mensaje] para privado");
        System.out.println(" Juego Gato: /jugar [usuario] | /aceptar | /mover [0-8] | /tablero");
        System.out.println(" Escribe 'salir' para desconectar.");
        System.out.println("---------------------------\n");
    }

    @Override
    public void run() {
        // Llama al método para mostrar los comandos al iniciar el cliente
        mostrarAyuda();
        try {
            while (true) {
                // Muestra un prompt para hacer más clara la entrada
                System.out.print(">");

                String mensaje = teclado.readLine();
                if (mensaje == null) break;


                if (mensaje.equalsIgnoreCase("/ayuda") || mensaje.equalsIgnoreCase("comandos")) {
                    mostrarAyuda();
                    continue;
                }

                salida.writeUTF(mensaje);
                salida.flush();

                if ("salir".equalsIgnoreCase(mensaje)) {
                    System.out.println("Cerrando conexión...");
                    socket.close();
                    break;
                }
            }
        } catch (IOException ex) {
            System.out.println("Error en ParaMandar: " + ex.getMessage());
        }
    }
}
