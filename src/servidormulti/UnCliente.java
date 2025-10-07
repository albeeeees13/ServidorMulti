package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class UnCliente implements Runnable {

    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;

    UnCliente(Socket s) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        String mensaje;
        while (true) {
            try {
                mensaje = entrada.readUTF();

                if (mensaje.startsWith("")) {
                    String[] partes = mensaje.split(" ");
                    String aQuien = partes[0].substring(1);
                    UnCliente cliente = ServidorMulti.clientes.get(aQuien);

                    if (cliente != null) {
                        cliente.salida.writeUTF(mensaje);
                    } else {
                        salida.writeUTF("Cliente " + aQuien + " no encontrado.");
                    }

                } else {
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        cliente.salida.writeUTF(mensaje);
                    }
                }
            } catch (IOException ex) {
                System.err.println("Error al recibir/enviar mensaje: " + ex.getMessage());
                break;
            }
        }
    }
}
