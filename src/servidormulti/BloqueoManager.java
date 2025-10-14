package servidormulti;

public class BloqueoManager {
    private static final String ARCHIVO_BLOQUEOS = "bloqueos.txt";
    private static final Map<String, Set<String>> bloqueos = new HashMap<>();

    static {
        cargarBloqueos();
    }
