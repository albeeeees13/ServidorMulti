package servidormulti;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Set;

public class UnCliente implements Runnable {

    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private String username = null;
    private int mensajesEnviados = 0;
    private final Socket socket;

    public UnCliente(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        try {
            salida.writeUTF("Bienvenido al servidor. Puedes mandar 3 mensajes antes de registrarte o iniciar sesión.");

            while (true) {
                String mensaje = entrada.readUTF().trim();

                if (mensaje.equalsIgnoreCase("salir")) {
                    salida.writeUTF("Cerrando conexión...");
                    socket.close();
                    break;
                }

                if (mensaje.equalsIgnoreCase("registrar")) {
                    registrarUsuario();
                    continue;
                }

                if (mensaje.equalsIgnoreCase("login")) {
                    iniciarSesion();
                    continue;
                }


                if (mensaje.startsWith("/bloquear ") || mensaje.startsWith("/desbloquear ")) {
                    if (username == null) {
                        salida.writeUTF("❌ Debes iniciar sesión para usar comandos de bloqueo.");
                    } else {
                        manejarComandoBloqueo(mensaje);
                    }
                    continue;
                }

                if (mensaje.equalsIgnoreCase("/verbloqueados")) {
                    if (username == null) {
                        salida.writeUTF("❌ Debes iniciar sesión para ver tus bloqueados.");
                    } else {
                        verBloqueados();
                    }
                    continue;
                }
                // -----------------------------

                // Restricción de mensajes
                if (username == null && mensajesEnviados >= 3) {
                    salida.writeUTF("Has alcanzado el límite de 3 mensajes. Escribe 'registrar' o 'login' para continuar.");
                    continue;
                }

                // 🔹 Lógica de Mensajería
                if (username != null || mensajesEnviados < 3) {
                    mensajesEnviados++;

                    String emisor = (username != null) ? username : "Cliente#" + socket.getPort();

                    if (mensaje.startsWith("@")) {
                        // Lógica de Mensajes Privados (se mantiene igual, ya funcionaba)
                        String[] partes = mensaje.split(" ", 2);
                        if (partes.length < 2) {
                            salida.writeUTF("Uso: @usuario mensaje");
                            continue;
                        }

                        String aQuien = partes[0].substring(1);
                        UnCliente clienteDestino = buscarClientePorNombre(aQuien);

                        if (clienteDestino != null && clienteDestino.username != null) {

                            if (!BloqueoManager.estaBloqueado(clienteDestino.username, emisor)) {
                                clienteDestino.salida.writeUTF("Privado de " + emisor + ": " + partes[1]);
                                salida.writeUTF("Mensaje enviado a " + aQuien);
                            } else {
                                salida.writeUTF("El usuario " + aQuien + " te ha bloqueado. No se pudo enviar el mensaje.");
                            }
                        } else {
                            salida.writeUTF("Usuario " + aQuien + " no encontrado o no está en el chat.");
                        }
                    } else {

                        difundirMensaje(emisor, mensaje);
                    }
                } else {
                    salida.writeUTF("No puedes mandar más mensajes sin registrarte o iniciar sesión.");
                }
            }
        } catch (IOException | SQLException ex) {
            System.err.println("Error con cliente " + (username != null ? username : socket.getPort()) + ": " + ex.getMessage());
        } finally {
            System.out.println("LOG: Desconexión del cliente " + (username != null ? username : socket.getPort()));
            try { socket.close(); } catch (IOException ignored) {}
        }
    }



    private void difundirMensaje(String emisor, String mensaje) {
        for (UnCliente cliente : ServidorMulti.clientes.values()) {


            if (cliente == this) {
                continue;
            }

            try {
                boolean estaBloqueado = false;


                if (cliente.username != null) {
                    estaBloqueado = BloqueoManager.estaBloqueado(cliente.username, emisor);
                }

                if (!estaBloqueado) {

                    cliente.salida.writeUTF(emisor + ": " + mensaje);
                }
            } catch (SQLException e) {
                System.err.println("Error SQL al verificar bloqueo para difusión: " + e.getMessage());

                try {
                    cliente.salida.writeUTF(emisor + ": " + mensaje);
                } catch (IOException ignored) {}
            } catch (IOException ignored) {

            }
        }
    }




    private void registrarUsuario() throws IOException {
        try {
            salida.writeUTF("Escribe un nombre de usuario:");
            String usuario = entrada.readUTF();

            salida.writeUTF("Escribe una contraseña:");
            String contrasena = entrada.readUTF();

            if (AuthManager.registrarUsuario(usuario, contrasena)) {
                this.username = usuario;
                salida.writeUTF("✅ Usuario registrado e inicio de sesión correcto. Ahora puedes mandar mensajes sin límite.");
            } else {
                salida.writeUTF("Ese usuario ya existe. Intenta iniciar sesión con 'login'.");
            }
        } catch (SQLException e) {
            salida.writeUTF("Error de servidor al registrar: " + e.getMessage());
            System.err.println("Error SQL en registro: " + e.getMessage());
        }
    }

    private void iniciarSesion() throws IOException {
        try {
            salida.writeUTF("Usuario:");
            String usuario = entrada.readUTF();

            salida.writeUTF("Contraseña:");
            String contrasena = entrada.readUTF();

            if (AuthManager.validarUsuario(usuario, contrasena)) {
                this.username = usuario;
                salida.writeUTF("✅ Inicio de sesión exitoso. Puedes continuar enviando mensajes.");
            } else {
                salida.writeUTF("❌ Usuario o contraseña incorrectos.");
            }
        } catch (SQLException e) {
            salida.writeUTF("Error de servidor al iniciar sesión: " + e.getMessage());
            System.err.println("Error SQL en login: " + e.getMessage());
        }
    }



    private void manejarComandoBloqueo(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ", 2);
        String comando = partes[0].toLowerCase();
        String accion = comando.substring(1);

        if (partes.length < 2) {
            salida.writeUTF("Uso inválido. Usa: " + comando + " usuario");
            return;
        }

        String aQuien = partes[1].trim();

        try {
            if (accion.equals("bloquear")) {
                BloqueoManager.bloquear(this.username, aQuien);
                salida.writeUTF("✅ Has bloqueado correctamente a " + aQuien + ". Sus mensajes ya no te llegarán.");
                System.out.println("LOG: " + this.username + " ha bloqueado a " + aQuien);
            } else {
                BloqueoManager.desbloquear(this.username, aQuien);
                salida.writeUTF("✅ Has desbloqueado a " + aQuien + ".");
                System.out.println("LOG: " + this.username + " ha desbloqueado a " + aQuien);
            }
        } catch (SQLException e) {
            salida.writeUTF("❌ Error en la base de datos: " + e.getMessage());
            System.err.println("Error SQL en " + accion + ": " + e.getMessage());
        }
    }

    private void verBloqueados() throws IOException {
        try {
            Set<String> bloqueados = BloqueoManager.verBloqueados(this.username);
            if (bloqueados.isEmpty()) {
                salida.writeUTF("Tu lista de bloqueados está vacía.");
            } else {
                salida.writeUTF("--- Usuarios Bloqueados ---");
                for (String user : bloqueados) {
                    salida.writeUTF("- " + user);
                }
                salida.writeUTF("---------------------------");
            }
        } catch (SQLException e) {
            salida.writeUTF("❌ Error al consultar bloqueados: " + e.getMessage());
        }
    }


    private UnCliente buscarClientePorNombre(String username) {
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.username != null && cliente.username.equalsIgnoreCase(username)) {
                return cliente;
            }
        }
        return null;
    }
}

