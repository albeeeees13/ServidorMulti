package servidormulti;

import java.sql.*;

public class UsuarioManager {

    static {
        crearTablaUsuarios();
    }

    private static void crearTablaUsuarios() {
        try (Connection conn = ConexionBD.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT UNIQUE NOT NULL,
                    contrasena TEXT NOT NULL
                )
            """);
        } catch (SQLException e) {
            System.err.println("Error al crear tabla de usuarios: " + e.getMessage());
        }
    }

    public static synchronized boolean registrar(String nombre, String contrasena) {
        String sql = "INSERT INTO usuarios (nombre, contrasena) VALUES (?, ?)";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, contrasena);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println(" No se pudo registrar usuario: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean login(String nombre, String contrasena) {
        String sql = "SELECT * FROM usuarios WHERE nombre = ? AND contrasena = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, contrasena);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println(" Error al iniciar sesión: " + e.getMessage());
            return false;
        }
    }
}

