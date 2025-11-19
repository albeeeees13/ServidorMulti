package servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorMulti {


    public static HashMap<String, UnCliente> clientes = new HashMap<>();


    public static final ConcurrentHashMap<String, JuegoGato> juegosActivos = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<String, String> invitacionesPendientes = new ConcurrentHashMap<>();

    private static void listarUsuarios() {
        String sql = "SELECT username FROM usuarios";
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- USUARIOS REGISTRADOS EN BD ---");
            if (!rs.isBeforeFirst()) {
                System.out.println("No hay usuarios registrados.");
                return;
            }
            while (rs.next()) {
                System.out.println("- " + rs.getString("username"));
            }
            System.out.println("----------------------------------");
        } catch (SQLException e) {
            System.err.println("Error SQL al listar usuarios: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int puerto = 8080;
        int contador = 0;

        // Hilo de la Consola del Servidor
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Consola de control: Escribe 'verusuarios' o 'salir'.");
            while (true) {
                if (scanner.hasNextLine()) {
                    String comando = scanner.nextLine().trim();
                    if (comando.equalsIgnoreCase("verusuarios")) {
                        listarUsuarios();
                    } else if (comando.equalsIgnoreCase("salir")) {
                        System.out.println("Apagando servidor...");
                        System.exit(0);
                    }
                }
            }
        }).start();

        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);

            while (true) {
                Socket socket = servidorSocket.accept();
                UnCliente uncliente = new UnCliente(socket);
                Thread hilo = new Thread(uncliente);
                String idCliente = Integer.toString(contador);
                clientes.put(idCliente, uncliente);
                hilo.start();
                System.out.println("se conectó el chango #" + contador);
                contador++;
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }
}