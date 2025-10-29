package servidormulti;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Set;
import java.util.Map;

public class UnCliente implements Runnable {

    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private String username = null;
    private int mensajesEnviados = 0;
    private final Socket socket;

    private JuegoGato juegoActual = null;

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

                if (username != null) {
                    if (mensaje.startsWith("/bloquear ") || mensaje.startsWith("/desbloquear ")) {
                        manejarComandoBloqueo(mensaje);
                        continue;
                    }
                    if (mensaje.equalsIgnoreCase("/verbloqueados")) {
                        verBloqueados();
                        continue;
                    }

                    if (mensaje.equalsIgnoreCase("/ranking")) {
                        manejarComandoRanking();
                        continue;
                    }
                    if (mensaje.startsWith("/estadistica ")) {
                        manejarComandoEstadistica(mensaje.substring(13).trim());
                        continue;
                    }

                    if (mensaje.startsWith("/jugar ")) {
                        manejarComandoJugar(mensaje.substring(7).trim());
                        continue;
                    }
                    if (mensaje.equalsIgnoreCase("/aceptar")) {
                        manejarComandoAceptar();
                        continue;
                    }
                    if (mensaje.startsWith("/mover ")) {
                        manejarComandoMover(mensaje.substring(7).trim());
                        continue;
                    }
                    if (mensaje.equalsIgnoreCase("/tablero")) {
                        manejarComandoTablero();
                        continue;
                    }
                }

                if (username == null && mensajesEnviados >= 3) {
                    salida.writeUTF("Has alcanzado el límite de 3 mensajes. Escribe 'registrar' o 'login' para continuar.");
                    continue;
                }

                if (username != null || mensajesEnviados < 3) {
                    mensajesEnviados++;

                    String emisor = (username != null) ? username : "Cliente#" + socket.getPort();

                    if (mensaje.startsWith("@")) {
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
            if (this.juegoActual != null) {
                terminarJuegoAbandono(this.username, this.juegoActual);
            }

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
                salida.writeUTF("Usuario registrado e inicio de sesión correcto. Ahora puedes mandar mensajes sin límite.");
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
                salida.writeUTF("Inicio de sesión exitoso. Puedes continuar enviando mensajes.");
            } else {
                salida.writeUTF("Usuario o contraseña incorrectos.");
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
                salida.writeUTF("Has bloqueado correctamente a " + aQuien + ". Sus mensajes ya no te llegarán.");
                System.out.println("LOG: " + this.username + " ha bloqueado a " + aQuien);
            } else {
                BloqueoManager.desbloquear(this.username, aQuien);
                salida.writeUTF("Has desbloqueado a " + aQuien + ".");
                System.out.println("LOG: " + this.username + " ha desbloqueado a " + aQuien);
            }
        } catch (SQLException e) {
            salida.writeUTF("Error en la base de datos: " + e.getMessage());
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
            salida.writeUTF("Error al consultar bloqueados: " + e.getMessage());
        }
    }

    private void manejarComandoJugar(String oponente) throws IOException, SQLException {
        if (oponente.equalsIgnoreCase(username)) {
            salida.writeUTF("No puedes invitarte a ti mismo.");
            return;
        }
        if (this.juegoActual != null) {
            salida.writeUTF("Ya estás en un juego contra " + juegoActual.getOponente(username) + ".");
            return;
        }

        if (!AuthManager.usuarioExiste(oponente)) {
            salida.writeUTF("El usuario '" + oponente + "' no está registrado.");
            return;
        }

        UnCliente oponenteCliente = buscarClientePorNombre(oponente);
        if (oponenteCliente == null || oponenteCliente.juegoActual != null) {
            salida.writeUTF("El usuario '" + oponente + "' no está conectado o ya está jugando.");
            return;
        }

        ServidorMulti.invitacionesPendientes.put(oponente, username);
        salida.writeUTF("Invitación enviada a " + oponente + ". Esperando respuesta...");
        oponenteCliente.salida.writeUTF("\n¡Has sido retado al GATO por " + username + "! Escribe /aceptar para empezar.");
    }

    private void manejarComandoAceptar() throws IOException {
        String retador = ServidorMulti.invitacionesPendientes.remove(username);
        if (retador == null) {
            salida.writeUTF("No tienes invitaciones pendientes.");
            return;
        }

        UnCliente retadorCliente = buscarClientePorNombre(retador);
        if (retadorCliente == null || retadorCliente.juegoActual != null) {
            salida.writeUTF("El retador se desconectó o ya está jugando.");
            notificarOponente(retador, "El retado se desconectó o ya no está disponible.");
            return;
        }

        JuegoGato nuevoJuego = new JuegoGato(retador, username);
        this.juegoActual = nuevoJuego;
        retadorCliente.juegoActual = nuevoJuego;

        ServidorMulti.juegosActivos.put(retador, nuevoJuego);
        ServidorMulti.juegosActivos.put(username, nuevoJuego);

        String mensajeInicio = "Juego iniciado! Tú eres '" + nuevoJuego.getMarca(username) + "'. " + nuevoJuego.dibujarTablero();
        salida.writeUTF(mensajeInicio);

        String mensajeRetador = "Juego iniciado! " + username + " aceptó. Tú eres '" + nuevoJuego.getMarca(retador) + "'. " + nuevoJuego.dibujarTablero();
        retadorCliente.salida.writeUTF(mensajeRetador);
    }

    private void manejarComandoMover(String posicionStr) throws IOException {
        if (juegoActual == null) {
            salida.writeUTF("No estás en un juego. Usa /jugar [usuario].");
            return;
        }

        try {
            int posicion = Integer.parseInt(posicionStr);
            int resultado = juegoActual.realizarMovimiento(posicion, username);
            String oponente = juegoActual.getOponente(username);

            if (resultado == 1) salida.writeUTF("No es tu turno.");
            else if (resultado == 2) salida.writeUTF("La posición " + posicion + " ya está ocupada.");
            else if (resultado == 3) salida.writeUTF("Posición inválida (0-8).");
            else {
                String tableroActual = juegoActual.dibujarTablero();

                if (juegoActual.hayGanador()) {
                    String ganador = juegoActual.getGanadorUsername();
                    String perdedor = juegoActual.getOponente(ganador);

                    salida.writeUTF("GANASTE! " + tableroActual);
                    notificarOponente(oponente, "HAS PERDIDO! " + ganador + " ganó. " + tableroActual);

                    AuthManager.actualizarRecord(ganador, "victoria");
                    AuthManager.actualizarRecord(perdedor, "derrota");

                    terminarJuego(ganador, perdedor);
                } else if (juegoActual.hayEmpate()) {
                    salida.writeUTF("EMPATE! " + tableroActual);
                    notificarOponente(oponente, "EMPATE! " + tableroActual);

                    AuthManager.actualizarRecord(username, "empate");
                    AuthManager.actualizarRecord(oponente, "empate");

                    terminarJuego(username, oponente);
                } else {
                    salida.writeUTF("Movimiento realizado. Turno de " + oponente + ". " + tableroActual);
                    notificarOponente(oponente, username + " movió a [" + posicion + "]. ¡Es tu turno! " + juegoActual.dibujarTablero());
                }
            }
        } catch (NumberFormatException e) {
            salida.writeUTF("Posición inválida. Usa /mover [0-8].");
        } catch (SQLException e) {
            salida.writeUTF("Error del servidor al registrar el récord.");
            System.err.println("Error SQL al registrar récord: " + e.getMessage());
        }
    }

    private void manejarComandoTablero() throws IOException {
        if (juegoActual == null) {
            salida.writeUTF("No estás en un juego. Usa /jugar [usuario].");
        } else {
            salida.writeUTF(juegoActual.dibujarTablero());
        }
    }

    private void terminarJuego(String jugador1, String jugador2) {
        UnCliente cliente1 = buscarClientePorNombre(jugador1);
        if (cliente1 != null) cliente1.juegoActual = null;

        UnCliente cliente2 = buscarClientePorNombre(jugador2);
        if (cliente2 != null) cliente2.juegoActual = null;

        ServidorMulti.juegosActivos.remove(jugador1);
        ServidorMulti.juegosActivos.remove(jugador2);
    }

    private void terminarJuegoAbandono(String abandonador, JuegoGato juego) {
        String ganador = juego.getOponente(abandonador);

        try {
            AuthManager.actualizarRecord(ganador, "victoria");
            AuthManager.actualizarRecord(abandonador, "derrota");
        } catch (SQLException e) {
            System.err.println("Error SQL al registrar abandono: " + e.getMessage());
        }

        notificarOponente(ganador, "HAS GANADO! " + abandonador + " se desconectó y perdió por abandono.");

        UnCliente ganadorCliente = buscarClientePorNombre(ganador);
        if (ganadorCliente != null) ganadorCliente.juegoActual = null;

        ServidorMulti.juegosActivos.remove(ganador);
        ServidorMulti.juegosActivos.remove(abandonador);

        System.out.println("LOG: " + abandonador + " abandonó el juego contra " + ganador);
    }

    private void manejarComandoRanking() throws IOException {
        try {
            Map<String, Integer> ranking = AuthManager.obtenerRanking();
            if (ranking.isEmpty()) {
                salida.writeUTF("No hay datos de ranking disponibles.");
                return;
            }

            StringBuilder sb = new StringBuilder("\n--- RANKING DEL GATO (Puntos) ---\n");
            int i = 1;
            for (Map.Entry<String, Integer> entry : ranking.entrySet()) {
                sb.append(String.format("%d. %s (%d puntos)\n", i++, entry.getKey(), entry.getValue()));
            }
            sb.append("---------------------------------------\n");
            salida.writeUTF(sb.toString());

        } catch (SQLException e) {
            salida.writeUTF("Error al consultar el ranking.");
            System.err.println("Error SQL al obtener ranking: " + e.getMessage());
        }
    }

    private void manejarComandoEstadistica(String otroJugador) throws IOException {
        if (!AuthManager.usuarioExiste(otroJugador)) {
            salida.writeUTF("El usuario '" + otroJugador + "' no existe en el sistema.");
            return;
        }

        try {
            Map<String, Integer> statsUsuario = AuthManager.obtenerEstadisticasJugador(username);
            Map<String, Integer> statsOtro = AuthManager.obtenerEstadisticasJugador(otroJugador);

            int totalUsuario = statsUsuario.getOrDefault("partidas_jugadas", 0);
            int totalOtro = statsOtro.getOrDefault("partidas_jugadas", 0);

            double porcentajeUsuario = (totalUsuario > 0)
                    ? ((double)statsUsuario.getOrDefault("victorias", 0) / totalUsuario) * 100
                    : 0.0;
            double porcentajeOtro = (totalOtro > 0)
                    ? ((double)statsOtro.getOrDefault("victorias", 0) / totalOtro) * 100
                    : 0.0;

            StringBuilder sb = new StringBuilder(String.format("\n--- ESTADÍSTICAS vs. %s ---\n", otroJugador.toUpperCase()));

            sb.append(String.format(" %s:\n", username))
                    .append(String.format("   Victorias: %d / %d partidas (%.2f%%)\n",
                            statsUsuario.getOrDefault("victorias", 0), totalUsuario, porcentajeUsuario));

            sb.append(String.format(" %s:\n", otroJugador))
                    .append(String.format("   Victorias: %d / %d partidas (%.2f%%)\n",
                            statsOtro.getOrDefault("victorias", 0), totalOtro, porcentajeOtro));

            sb.append("---------------------------------------\n");
            salida.writeUTF(sb.toString());

        } catch (SQLException e) {
            salida.writeUTF("Error al consultar estadísticas.");
            System.err.println("Error SQL al obtener estadísticas: " + e.getMessage());
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

