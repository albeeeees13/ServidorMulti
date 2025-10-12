ppackage servidormulti;

import java.io.*;
import java.net.Socket;

public class UnCliente implements Runnable {

    final DataOutputStream salida;
    final DataInputStream entrada;
    int mensajesEnviados = 0;
    boolean autenticado = false;
    Socket socket;

    UnCliente(Socket s) throws IOException {
        socket = s;
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        String mensaje;

        try {
            salida.writeUTF("Bienvenido al servidor. Puedes mandar 3 mensajes antes de registrarte o iniciar sesión.");

            while (true) {
                mensaje = entrada.readUTF();

                // Si el cliente quiere salir
                if (mensaje.equalsIgnoreCase("salir")) {
                    salida.writeUTF("Cerrando conexión...");
                    socket.close();
                    break;
                }

                // Si no está autenticado y ya mandó 3 mensajes
                if (!autenticado && mensajesEnviados >= 3) {
                    salida.writeUTF("Has alcanzado el límite de 3 mensajes. Escribe 'registrar' o 'login' para continuar.");
                    continue;
                }

                mensajesEnviados++;

                // Mensaje privado
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    String aQuien = partes[0].substring(1);
                    UnCliente cliente = ServidorMulti.clientes.get(aQuien);

                    if (cliente != null) {
                        cliente.salida.writeUTF("Privado de " + socket.getPort() + ": " + partes[1]);
                    } else {
                        salida.writeUTF("Cliente " + aQuien + " no encontrado.");
                    }
                } else {
                    for (UnCliente cliente : ServidorMulti.clientes.values()) {
                        if (cliente != this) {
                            cliente.salida.writeUTF("Cliente " + socket.getPort() + ": " + mensaje);
                        }
                    }
                }
            }

        } catch (IOException ex) {
            System.err.println("Error al recibir/enviar mensaje: " + ex.getMessage());
        }
    }
}

