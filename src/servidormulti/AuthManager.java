
package servidormulti;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AuthManager {

    private static Connection getConnection() throws SQLException {
        return ConexionBD.getConnection();
    }


    static {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {


            String sqlUsuarios = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    username TEXT PRIMARY KEY,
                    password_hash TEXT NOT NULL
                )
                """;
            st.execute(sqlUsuarios);


            String sqlRecords = """
                CREATE TABLE IF NOT EXISTS gato_records (
                    jugador TEXT PRIMARY KEY,
                    puntos INTEGER DEFAULT 0,
                    victorias INTEGER DEFAULT 0,
                    empates INTEGER DEFAULT 0,
                    derrotas INTEGER DEFAULT 0,
                    partidas_jugadas INTEGER DEFAULT 0
                )
                """;
            st.execute(sqlRecords);

        } catch (SQLException e) {
            System.err.println("Error al crear tablas: " + e.getMessage());
        }
    }

    private static String hashPassword(String password) {
        return String.valueOf(password.hashCode());
    }


    public static synchronized boolean registrarUsuario(String usuario, String contrasena) throws SQLException {
        if (usuarioExiste(usuario)) {
            return false;
        }

        String sql = "INSERT INTO usuarios (username, password_hash) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario.toLowerCase());
            ps.setString(2, hashPassword(contrasena));
            ps.executeUpdate();
            return true;
        }
    }

    // --------------------------------------------------------------------------

    public static synchronized boolean validarUsuario(String usuario, String contrasena) throws SQLException {
        String sql = "SELECT password_hash FROM usuarios WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario.toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    return storedHash.equals(hashPassword(contrasena));
                }
            }
        }
        return false;
    }

    // --------------------------------------------------------------------------

    public static synchronized boolean usuarioExiste(String usuario) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}