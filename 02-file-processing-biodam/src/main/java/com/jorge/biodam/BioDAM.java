package com.jorge.biodam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * BioDAM: sistema de ingesta y auditoría de diagnósticos clínicos.
 *
 * <p>Flujo: crear log de auditoría → validar la ruta recibida (Path Traversal) →
 * comprobar que el fichero existe → procesar el CSV con Streams → registrar el
 * cierre → mover el fichero a {@code salida/procesados/}.</p>
 *
 * @author Jorge González Gómez
 */
public class BioDAM {

    /** Única carpeta desde la que se permite leer ficheros. */
    private static final Path DIRECTORIO_PERMITIDO = Path.of("datos");

    /** Carpeta donde se guardan los logs de auditoría. */
    private static final Path DIRECTORIO_LOGS = Path.of("salida", "sistema_logs");

    /** Carpeta a la que se mueven los ficheros ya procesados. */
    private static final Path DIRECTORIO_PROCESADOS = Path.of("salida", "procesados");

    /** Formato del nombre del log (sin ":" porque Windows no lo permite en nombres). */
    private static final DateTimeFormatter FORMATO_NOMBRE_LOG = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** Formato del timestamp de cada línea del log, con milisegundos. */
    private static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Separador de columnas del CSV. */
    private static final String SEPARADOR = ";";

    /** Posiciones de las columnas: ID_PACIENTE;EDAD;DIAGNOSTICO;ESTADO. */
    private static final int COL_EDAD = 1;
    private static final int COL_DIAGNOSTICO = 2;
    private static final int COL_ESTADO = 3;



    /** Ruta del log de ESTA ejecución (se rellena en {@link #crearArchivoLog()}). */
    private static Path archivoLog;

    /** Niveles de severidad admitidos en el log. */
    private enum Nivel { INFO, WARN, ERROR }

    /**
     * Punto de entrada: orquesta el flujo completo llamando a cada método en orden.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        // Ruta que "envía" un usuario externo. Para probar el ataque: "datos/../../etc/passwd"
        String rutaUsuario = "datos/diagnosticos_2026.csv";

        try {
            crearArchivoLog();

            Path fichero = validarRuta(rutaUsuario);

            if (!Files.exists(fichero)) {
                log(Nivel.ERROR, "El fichero no existe: " + fichero);
                return;
            }

            procesarFichero(fichero);

            log(Nivel.INFO, "Importación finalizada correctamente: " + fichero.getFileName());
            moverAProcesados(fichero);

        } catch (SecurityException e) {
            System.err.println("Acceso bloqueado: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de entrada/salida: " + e.getMessage());
        }
    }

    // ===================== BLOQUE 1: LOG =====================

    /**
     * Crea la carpeta de logs si no existe y fija el nombre único del log de esta
     * ejecución: {@code auditoria_yyyyMMddHHmmss.log}.
     *
     * @throws IOException si no se puede crear la carpeta o escribir el log
     */
    private static void crearArchivoLog() throws IOException {
        Files.createDirectories(DIRECTORIO_LOGS);
        String nombre = "auditoria_" + LocalDateTime.now().format(FORMATO_NOMBRE_LOG) + ".log";
        archivoLog = DIRECTORIO_LOGS.resolve(nombre);
        log(Nivel.INFO, "Aplicación iniciada. Log: " + archivoLog);
    }

    /**
     * Añade una línea al log con el formato {@code [yyyy-MM-dd HH:mm:ss.SSS] NIVEL mensaje}
     * y la muestra también por consola. La hora se calcula en cada llamada.
     *
     * @param nivel   severidad del mensaje (INFO, WARN o ERROR)
     * @param mensaje texto a registrar
     * @throws IOException si no se puede escribir en el fichero de log
     */
    private static void log(Nivel nivel, String mensaje) throws IOException {
        String timestamp = LocalDateTime.now().format(FORMATO_TIMESTAMP);
        String linea = "[" + timestamp + "] " + nivel + " " + mensaje;
        Files.writeString(archivoLog, linea + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.println(linea);
    }

    // ===================== BLOQUE 2: SEGURIDAD =====================

    /**
     * Comprueba que la ruta recibida está dentro de {@link #DIRECTORIO_PERMITIDO}.
     * Ambas rutas se pasan a absolutas y se normalizan (se resuelven los "..")
     * antes de compararlas carpeta a carpeta con {@link Path#startsWith(Path)}.
     *
     * @param rutaUsuario ruta tal y como llega del exterior
     * @return la ruta absoluta y normalizada, ya validada
     * @throws SecurityException si la ruta intenta salir del directorio permitido
     * @throws IOException       si falla la escritura del aviso en el log
     */
    private static Path validarRuta(String rutaUsuario) throws IOException {
        Path base = DIRECTORIO_PERMITIDO.toAbsolutePath().normalize();
        Path candidata = Path.of(rutaUsuario).toAbsolutePath().normalize();

        if (!candidata.startsWith(base)) {
            log(Nivel.WARN, "Intento de Path Traversal bloqueado: " + rutaUsuario);
            throw new SecurityException("Ruta fuera del directorio permitido: " + rutaUsuario);
        }
        return candidata;
    }

    // ===================== BLOQUE 3: STREAMS =====================

    /**
     * Lee el CSV ({@code ID_PACIENTE;EDAD;DIAGNOSTICO;ESTADO}) y muestra por consola:
     * el número de registros, los diagnósticos únicos de pacientes ACTIVO ordenados
     * alfabéticamente y la edad media de los pacientes con "Diabetes".
     * El fichero se abre una sola vez en un try-with-resources y las filas se guardan
     * en una lista, porque un Stream solo puede recorrerse una vez.
     *
     * @param fichero CSV ya validado
     * @throws IOException si no se puede leer el fichero
     */
    private static void procesarFichero(Path fichero) throws IOException {
        List<String[]> filas;
        try (Stream<String> lineas = Files.lines(fichero)) {
            filas = lineas.skip(1)                          // fuera la cabecera
                    .filter(linea -> !linea.isBlank())      // fuera líneas vacías
                    .map(linea -> linea.split(SEPARADOR))   // "101;62;..." -> ["101","62",...]
                    .toList();
        }


        // 1) Total de registros (sin cabecera)
        int total = filas.size();

        // 2) Diagnósticos únicos de pacientes ACTIVO, ordenados alfabéticamente
        List<String> diagnosticosActivos = filas.stream()
                .filter(f -> f[COL_ESTADO].trim().equals("ACTIVO"))
                .map(f -> f[COL_DIAGNOSTICO].trim())
                .distinct()
                .sorted()
                .toList();

        // 3) Edad media de los pacientes con "Diabetes" (contains: también "Diabetes tipo 2")
        double edadMediaDiabetes = filas.stream()
                .filter(f -> f[COL_DIAGNOSTICO].contains("Diabetes"))
                .mapToInt(f -> Integer.parseInt(f[COL_EDAD].trim()))
                .average()
                .orElse(0.0);

        System.out.println("Registros procesados: " + total);
        System.out.println("Diagnósticos activos: " + diagnosticosActivos);
        System.out.println("Edad media Diabetes: " + edadMediaDiabetes);
    }



    // ===================== BLOQUE 4: CIERRE =====================

    /**
     * Mueve el fichero procesado a {@code salida/procesados/}, sobrescribiendo
     * explícitamente si ya existe uno con el mismo nombre.
     *
     * @param fichero CSV ya procesado
     * @throws IOException si no se puede crear la carpeta o mover el fichero
     */
    private static void moverAProcesados(Path fichero) throws IOException {
        Files.createDirectories(DIRECTORIO_PROCESADOS);
        Path destino = DIRECTORIO_PROCESADOS.resolve(fichero.getFileName());
        Files.move(fichero, destino, StandardCopyOption.REPLACE_EXISTING);
        log(Nivel.INFO, "Fichero movido a " + destino);
    }
}
