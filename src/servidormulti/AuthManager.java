
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

            inicializarRecord(usuario);

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

    private static void inicializarRecord(String usuario) throws SQLException {
        String sql = "INSERT OR IGNORE INTO gato_records (jugador) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.toLowerCase());
            ps.executeUpdate();
        }
    }

    public static synchronized void actualizarRecord(String jugador, String resultado) throws SQLException {
        int puntosGanados = 0;
        String columnaAIncrementar = "";

        if ("victoria".equals(resultado)) {
            puntosGanados = 2;
            columnaAIncrementar = "victorias";
        } else if ("empate".equals(resultado)) {
            puntosGanados = 1;
            columnaAIncrementar = "empates";
        } else if ("derrota".equals(resultado)) {
            puntosGanados = 0;
            columnaAIncrementar = "derrotas";
        } else {
            return;
        }

        String sql = String.format("""
                UPDATE gato_records 
                SET puntos = puntos + ?, 
                    %s = %s + 1, 
                    partidas_jugadas = partidas_jugadas + 1
                WHERE jugador = ?
                """, columnaAIncrementar, columnaAIncrementar);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, puntosGanados);
            ps.setString(2, jugador.toLowerCase());
            ps.executeUpdate();
        }
    }

    public static synchronized Map<String, Integer> obtenerRanking() throws SQLException {
        Map<String, Integer> ranking = new HashMap<>();
        String sql = "SELECT jugador, puntos FROM gato_records ORDER BY puntos DESC, victorias DESC";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                ranking.put(rs.getString("jugador"), rs.getInt("puntos"));
            }
        }
        return ranking;
    }

    public static synchronized Map<String, Integer> obtenerEstadisticasJugador(String jugador) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT victorias, empates, derrotas, partidas_jugadas FROM gato_records WHERE jugador = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugador.toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("victorias", rs.getInt("victorias"));
                    stats.put("empates", rs.getInt("empates"));
                    stats.put("derrotas", rs.getInt("derrotas"));
                    stats.put("partidas_jugadas", rs.getInt("partidas_jugadas"));
                }
            }
        }
        return stats;
    }
}

