
package servidormulti;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class BloqueoManager {

    private static Connection getConnection() throws SQLException {
        return ConexionBD.getConnection();
    }

    static {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS bloqueos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    quien TEXT NOT NULL,
                    a_quien TEXT NOT NULL
                )
                """;
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error al crear tabla de bloqueos: " + e.getMessage());
        }
    }

    public static synchronized void bloquear(String quien, String aQuien) throws SQLException {
        if (quien.equalsIgnoreCase(aQuien)) {
            throw new SQLException("No puedes bloquearte a ti mismo.");
        }


        if (!AuthManager.usuarioExiste(aQuien)) {
            throw new SQLException("El usuario '" + aQuien + "' no existe en el sistema.");
        }

        if (estaBloqueado(quien, aQuien)) {
            throw new SQLException("Ya tienes bloqueado a " + aQuien);
        }

        String sql = "INSERT INTO bloqueos (quien, a_quien) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quien.toLowerCase());
            ps.setString(2, aQuien.toLowerCase());
            ps.executeUpdate();
        }
    }

    public static synchronized boolean estaBloqueado(String receptor, String emisor) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bloqueos WHERE quien = ? AND a_quien = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receptor.toLowerCase());
            ps.setString(2, emisor.toLowerCase());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public static synchronized Set<String> verBloqueados(String quien) throws SQLException {
        Set<String> bloqueados = new HashSet<>();
        String sql = "SELECT a_quien FROM bloqueos WHERE quien = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quien.toLowerCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bloqueados.add(rs.getString("a_quien"));
            }
        }
        return bloqueados;
    }

    public static synchronized void desbloquear(String quien, String aQuien) throws SQLException {
        String sql = "DELETE FROM bloqueos WHERE quien = ? AND a_quien = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quien.toLowerCase());
            ps.setString(2, aQuien.toLowerCase());
            ps.executeUpdate();
        }
    }
}
