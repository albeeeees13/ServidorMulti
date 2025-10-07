package servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {

    static HashMap<String, UnCliente> clientes = new HashMap<>();

    public static void main(String[] args) {
        int puerto = 8080;
        int contador = 0;

        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);

            while (true) {
                Socket socket = servidorSocket.accept();

                UnCliente uncliente = new UnCliente(socket);
                Thread hilo = new Thread(uncliente);

                String idCliente = Integer.toString(contador);
                clientes.put(idCliente, uncliente);

                hilo.start();

                System.out.println("se conecto el chango #" + contador);

                contador++;
            }
        } catch (IOException e) {
        }
    }
}
