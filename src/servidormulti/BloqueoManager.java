package servidormulti;

public class BloqueoManager {
    private static final String ARCHIVO_BLOQUEOS = "bloqueos.txt";
    private static final Map<String, Set<String>> bloqueos = new HashMap<>();

    static {
        cargarBloqueos();
    }
    public static synchronized void bloquear(String quien, String aQuien) throws IOException {
        if (quien.equalsIgnoreCase(aQuien)) {
            throw new IOException("No puedes bloquearte a ti mismo.");
        }

        bloqueos.putIfAbsent(quien, new HashSet<>());
        if (!ServidorMulti.clientes.containsKey(aQuien)) {
            throw new IOException("El usuario '" + aQuien + "' no existe.");
        }

        if (bloqueos.get(quien).contains(aQuien)) {
            throw new IOException("Ya tienes bloqueado a " + aQuien);
        }

        bloqueos.get(quien).add(aQuien);
        guardarBloqueos();
    }