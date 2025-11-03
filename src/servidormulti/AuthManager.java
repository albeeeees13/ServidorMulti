package servidormulti;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

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

            String sqlGrupos = """
                CREATE TABLE IF NOT EXISTS grupos (
                    nombre TEXT PRIMARY KEY,
                    miembros TEXT NOT NULL,
                    creador TEXT NOT NULL
                )
                """;
            st.execute(sqlGrupos);

            String sqlMensajesOffline = """
                CREATE TABLE IF NOT EXISTS mensajes_offline (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    grupo TEXT NOT NULL,
                    emisor TEXT NOT NULL,
                    contenido TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    visto_por TEXT DEFAULT ''
                )
                """;
            st.execute(sqlMensajesOffline);

            String sqlInsertTodos = "INSERT OR IGNORE INTO grupos (nombre, miembros, creador) VALUES ('Todos', ',', 'SERVER')";
            st.execute(sqlInsertTodos);

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

    public static synchronized boolean crearGrupo(String nombre, String creador) throws SQLException {
        if (grupoExiste(nombre)) return false;

        String sql = "INSERT INTO grupos (nombre, miembros, creador) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, "," + creador.toLowerCase() + ",");
            ps.setString(3, creador.toLowerCase());
            ps.executeUpdate();
            return true;
        }
    }

    public static synchronized boolean borrarGrupo(String nombre) throws SQLException {
        String sql = "DELETE FROM grupos WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            return ps.executeUpdate() > 0;
        }
    }

    public static synchronized boolean grupoExiste(String nombre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM grupos WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public static synchronized String obtenerCreadorGrupo(String nombre) throws SQLException {
        String sql = "SELECT creador FROM grupos WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("creador") : null;
        }
    }

    public static synchronized void actualizarMiembrosGrupo(String grupo, String miembros) throws SQLException {
        String sql = "UPDATE grupos SET miembros = ? WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, miembros);
            ps.setString(2, grupo);
            ps.executeUpdate();
        }
    }

    public static synchronized String obtenerMiembrosString(String grupo) throws SQLException {
        String sql = "SELECT miembros FROM grupos WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, grupo);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("miembros") : null;
        }
    }

    public static synchronized Set<String> obtenerNombresGrupos() throws SQLException {
        Set<String> grupos = new HashSet<>();
        String sql = "SELECT nombre FROM grupos";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                grupos.add(rs.getString("nombre"));
            }
        }
        return grupos;
    }

    public static synchronized void almacenarMensajeOffline(String grupo, String emisor, String contenido) throws SQLException {
        String sql = "INSERT INTO mensajes_offline (grupo, emisor, contenido) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, grupo);
            ps.setString(2, emisor);
            ps.setString(3, contenido);
            ps.executeUpdate();
        }
    }

    public static synchronized Map<String, List<String>> obtenerMensajesNoVistos(String usuario) throws SQLException {
        Map<String, List<String>> mensajesPorGrupo = new HashMap<>();

        String usuarioBuscado = "," + usuario.toLowerCase() + ",";

        String sql = """
            SELECT m.id, m.grupo, m.emisor, m.contenido, m.timestamp 
            FROM mensajes_offline m
            JOIN grupos g ON m.grupo = g.nombre
            WHERE m.visto_por NOT LIKE ? AND g.miembros LIKE ?
            ORDER BY m.timestamp ASC
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + usuarioBuscado + "%");
            ps.setString(2, "%" + usuarioBuscado + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String grupo = rs.getString("grupo");
                    String emisor = rs.getString("emisor");
                    String contenido = rs.getString("contenido");
                    String timestamp = rs.getString("timestamp");

                    String mensajeCompleto = String.format("%s [%s]: %s", timestamp.substring(11, 16), emisor, contenido);

                    mensajesPorGrupo.computeIfAbsent(grupo, k -> new ArrayList<>()).add(mensajeCompleto);
                }
            }
        }
        return mensajesPorGrupo;
    }

    public static synchronized void marcarMensajesComoVistos(String usuario) throws SQLException {
        String usuarioBuscado = "," + usuario.toLowerCase() + ",";

        String sqlSelectIds = """
            SELECT m.id FROM mensajes_offline m
            JOIN grupos g ON m.grupo = g.nombre
            WHERE m.visto_por NOT LIKE ? AND g.miembros LIKE ?
            """;

        List<Long> ids = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlSelectIds)) {

            ps.setString(1, "%" + usuarioBuscado + "%");
            ps.setString(2, "%" + usuarioBuscado + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
        }

        if (ids.isEmpty()) return;

        String sqlUpdateVisto = "UPDATE mensajes_offline SET visto_por = visto_por || ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpdateVisto)) {

            String usuarioConComa = usuario.toLowerCase() + ",";

            for (long id : ids) {
                ps.setString(1, usuarioConComa);
                ps.setLong(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
