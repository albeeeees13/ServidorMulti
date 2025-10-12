package servidormulti;

import java.io.*;
import java.net.Socket;

public class UnCliente implements Runnable {

    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private boolean autenticado = false;
    private int mensajesEnviados = 0;
    private final Socket socket;
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";

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
                String mensaje = entrada.readUTF();


                if (mensaje.equalsIgnoreCase("salir")) {
                    salida.writeUTF("Cerrando conexión...");
                    socket.close();
                    break;
                }


                if (!autenticado && mensajesEnviados >= 3) {
                    salida.writeUTF("Has alcanzado el límite de 3 mensajes. Escribe 'registrar' o 'login' para continuar.");
                    continue;
                }


                if (mensaje.equalsIgnoreCase("registrar")) {
                    registrarUsuario();
                    continue;
                }


                if (mensaje.equalsIgnoreCase("login")) {
                    iniciarSesion();
                    continue;
                }


                if (autenticado || mensajesEnviados < 3) {
                    mensajesEnviados++;

                    if (mensaje.startsWith("@")) {
                        String[] partes = mensaje.split(" ", 2);
                        if (partes.length < 2) continue;

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

                } else {
                    salida.writeUTF("No puedes mandar más mensajes sin registrarte o iniciar sesión.");
                }
            }

        } catch (IOException ex) {
            System.err.println("Error con cliente: " + ex.getMessage());
        }
    }

    // ----------------------------------------------------------
    // MÉTODOS DE LOGIN Y REGISTRO
    // ----------------------------------------------------------

    private void registrarUsuario() throws IOException {
        salida.writeUTF("Escribe un nombre de usuario:");
        String usuario = entrada.readUTF();

        salida.writeUTF("Escribe una contraseña:");
        String contrasena = entrada.readUTF();

        if (usuarioExiste(usuario)) {
            salida.writeUTF("Ese usuario ya existe. Intenta iniciar sesión con 'login'.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            writer.write(usuario + ":" + contrasena);
            writer.newLine();
        }

        autenticado = true;
        salida.writeUTF("✅ Usuario registrado correctamente. Ahora puedes mandar mensajes sin límite.");
    }

    private void iniciarSesion() throws IOException {
        salida.writeUTF("Usuario:");
        String usuario = entrada.readUTF();

        salida.writeUTF("Contraseña:");
        String contrasena = entrada.readUTF();

        if (validarUsuario(usuario, contrasena)) {
            autenticado = true;
            salida.writeUTF(" Inicio de sesión exitoso. Puedes continuar enviando mensajes.");
        } else {
            salida.writeUTF(" Usuario o contraseña incorrectos.");
        }
    }

    private boolean usuarioExiste(String usuario) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes[0].equalsIgnoreCase(usuario)) {
                    return true;
                }
            }
        } catch (FileNotFoundException ignored) {}
        return false;
    }

    private boolean validarUsuario(String usuario, String contrasena) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2 && partes[0].equalsIgnoreCase(usuario) && partes[1].equals(contrasena)) {
                    return true;
                }
            }
        } catch (FileNotFoundException ignored) {}
        return false;
    }
}


