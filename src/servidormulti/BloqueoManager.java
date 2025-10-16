package servidormulti;

import java.io.*;
import java.util.*;

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

    public static synchronized boolean estaBloqueado(String receptor, String emisor) {
        return bloqueos.containsKey(receptor) && bloqueos.get(receptor).contains(emisor);
    }

    public static synchronized Set<String> verBloqueados(String quien) {
        return bloqueos.getOrDefault(quien, new HashSet<>());
    }

    private static void cargarBloqueos() {
        File archivo = new File(ARCHIVO_BLOQUEOS);
        if (!archivo.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    bloqueos.putIfAbsent(partes[0], new HashSet<>());
                    bloqueos.get(partes[0]).add(partes[1]);
                }
            }
        } catch (IOException ignored) {}
    }

    private static void guardarBloqueos() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO_BLOQUEOS))) {
            for (Map.Entry<String, Set<String>> entry : bloqueos.entrySet()) {
                for (String bloqueado : entry.getValue()) {
                    bw.write(entry.getKey() + ":" + bloqueado);
                    bw.newLine();
                }
            }
        }
    }
}