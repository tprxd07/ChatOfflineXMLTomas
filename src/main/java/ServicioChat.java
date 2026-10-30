import jakarta.xml.bind.JAXBException;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ServicioChat {

    private final GestorDatosXML gestorXML = new GestorDatosXML();

    private static final File CARPETA_MEDIA = new File("media");
    private static final File CARPETA_RESOURCES = new File("src/main/resources");
    private static final long TAMANIO_MAXIMO_BYTES = 10 * 1024 * 1024; //10 MB máximo
    private static final Set<String> EXTENSIONES_VALIDAS = Set.of(
            "jpg", "jpeg", "png", "gif", "pdf", "txt", "doc", "docx", "zip"
    );

    public boolean existeUsuario(String nombre) throws JAXBException {
        if (nombre == null) return false;
        String nombreBuscado = nombre.trim();
        ListaUsuarios lista = gestorXML.leerUsuarios();
        return lista.getUsuarios().stream()
                .anyMatch(u -> u.getNombre() != null && u.getNombre().equalsIgnoreCase(nombreBuscado));
    }

    //Recupera usuario contando las mayúsculas
    public String obtenerNombreReal(String nombre) throws JAXBException {
        if (nombre == null) return null;
        String nombreBuscado = nombre.trim();
        ListaUsuarios lista = gestorXML.leerUsuarios();
        return lista.getUsuarios().stream()
                .filter(u -> u.getNombre() != null && u.getNombre().equalsIgnoreCase(nombreBuscado))
                .map(Usuario::getNombre)
                .findFirst()
                .orElse(null);
    }

    public boolean existeUsuarioEstricto(String nombre) throws JAXBException {
        if (nombre == null) return false;
        String nombreBuscado = nombre.trim();
        ListaUsuarios lista = gestorXML.leerUsuarios();
        return lista.getUsuarios().stream()
                .anyMatch(u -> u.getNombre() != null && u.getNombre().equals(nombreBuscado));
    }


    public void registrarUsuario(String nombre) throws JAXBException {
        String nombreLimpio = nombre.trim();
        if (nombreLimpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío.");
        }
        if (existeUsuarioEstricto(nombreLimpio)) {
            throw new IllegalArgumentException("El usuario '" + nombreLimpio + "' ya existe (respeta las mayúsculas/minúsculas).");
        }

        ListaUsuarios lista = gestorXML.leerUsuarios();
        lista.getUsuarios().add(new Usuario(nombreLimpio));
        gestorXML.guardarUsuarios(lista);
    }

    public List<Usuario> listarUsuarios() throws JAXBException {
        return gestorXML.leerUsuarios().getUsuarios();
    }

    public List<Mensaje> obtenerTodosLosMensajes() throws JAXBException {
        return gestorXML.leerMensajes().getMensajes();
    }

    public Mensaje enviarMensaje(String emisor, String destinatario, String contenido) throws JAXBException {
        if (!existeUsuario(emisor)) {
            throw new IllegalArgumentException("El usuario emisor '" + emisor + "' no existe.");
        }
        if (!existeUsuario(destinatario)) {
            throw new IllegalArgumentException("El usuario destinatario '" + destinatario + "' no existe.");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío.");
        }

        String fechaActual = LocalDateTime.now().withNano(0).toString();
        Mensaje nuevo = new Mensaje(emisor.trim(), destinatario.trim(), contenido.trim(), fechaActual);

        ListaMensajes lista = gestorXML.leerMensajes();
        lista.getMensajes().add(nuevo);
        gestorXML.guardarMensajes(lista);

        return nuevo;
    }

    public List<Mensaje> consultarConversacion(String usuarioA, String usuarioB) throws JAXBException {
        ListaMensajes lista = gestorXML.leerMensajes();
        String uA = usuarioA.trim();
        String uB = usuarioB.trim();

        return lista.getMensajes().stream()
                .filter(m -> (m.getEmisor().equalsIgnoreCase(uA) && m.getDestinatario().equalsIgnoreCase(uB))
                        || (m.getEmisor().equalsIgnoreCase(uB) && m.getDestinatario().equalsIgnoreCase(uA)))
                .collect(Collectors.toList());
    }

    //Estadísticas de un chat concreto: mensajes por usuario usando Streams
    public Map<String, Long> contarMensajesPorUsuario(List<Mensaje> conversacion) {
        return conversacion.stream()
                .collect(Collectors.groupingBy(Mensaje::getEmisor, Collectors.counting()));
    }

    //Estadísticas globales: Top 5 usuarios con más mensajes enviados
    public List<String> obtenerTop5UsuariosGlobales() throws JAXBException {
        List<Mensaje> todos = obtenerTodosLosMensajes();
        return todos.stream()
                .collect(Collectors.groupingBy(Mensaje::getEmisor, Collectors.counting()))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(e -> e.getKey() + " (" + e.getValue() + " mensajes)")
                .collect(Collectors.toList());
    }

    //Top 5 palabras más usadas en una lista de mensajes (sirve tanto para un chat como global)
    public List<String> obtenerTop5Palabras(List<Mensaje> mensajes) {
        return mensajes.stream()
                .map(Mensaje::getContenido)
                .filter(texto -> texto != null && !texto.isBlank())
                .flatMap(texto -> Arrays.stream(texto.toLowerCase().split("[^a-záéíóúüñ0-9]+")))
                .filter(palabra -> !palabra.isEmpty())
                .collect(Collectors.groupingBy(palabra -> palabra, Collectors.counting()))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(e -> "\"" + e.getKey() + "\" (" + e.getValue() + " veces)")
                .collect(Collectors.toList());
    }

    public void exportarConversacion(String usuarioA, String usuarioB, Path rutaDestino)
            throws JAXBException, IOException {
        List<Mensaje> conversacion = consultarConversacion(usuarioA, usuarioB);
        if (conversacion.isEmpty()) {
            throw new IllegalArgumentException("No hay mensajes entre estos usuarios para exportar.");
        }

        List<String> lineas = conversacion.stream()
                .map(this::formatearMensajeLinea)
                .collect(Collectors.toList());

        Files.write(rutaDestino, lineas);
    }

    public String formatearMensajeLinea(Mensaje m) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(m.getFecha()).append("] ")
                .append(m.getEmisor()).append(" -> ").append(m.getDestinatario())
                .append(": ").append(m.getContenido());

        if (m.tieneAdjunto()) {
            File archivoFisico = new File(m.getAdjuntoRuta());
            if (archivoFisico.exists()) {
                sb.append(" [Adjunto: ").append(m.getAdjuntoNombre())
                        .append(" (").append(m.getAdjuntoTamanio()).append(" bytes, ")
                        .append(m.getAdjuntoTipo()).append(")]");
            } else {
                sb.append(" [AVISO: Archivo adjunto '").append(m.getAdjuntoNombre())
                        .append("' no encontrado en ").append(m.getAdjuntoRuta()).append("]");
            }
        }
        return sb.toString();
    }

    //Lista los archivos disponibles dentro de src/main/resources
    public List<String> listarArchivosResources() {
        if (!CARPETA_RESOURCES.exists() || !CARPETA_RESOURCES.isDirectory()) {
            return Collections.emptyList();
        }
        File[] archivos = CARPETA_RESOURCES.listFiles(File::isFile);
        if (archivos == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(archivos)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    //Busca el archivo en src/main/resources (o ruta directa si existiera)
    public Path resolverArchivoResources(String nombreArchivo) {
        Path enResources = Paths.get("src", "main", "resources", nombreArchivo.trim());
        if (Files.exists(enResources)) {
            return enResources;
        }
        return Paths.get(nombreArchivo.trim());
    }

    public void adjuntarArchivoUltimoMensaje(String emisor, String destinatario, String nombreArchivo)
            throws IOException, JAXBException {
        Path archivoOrigen = resolverArchivoResources(nombreArchivo);
        validarArchivoAdjunto(archivoOrigen);

        ListaMensajes lista = gestorXML.leerMensajes();
        List<Mensaje> todos = lista.getMensajes();

        Mensaje objetivo = null;
        for (int i = todos.size() - 1; i >= 0; i--) {
            Mensaje m = todos.get(i);
            if (m.getEmisor().equalsIgnoreCase(emisor.trim())
                    && m.getDestinatario().equalsIgnoreCase(destinatario.trim())) {
                objetivo = m;
                break;
            }
        }

        if (objetivo == null) {
            throw new IllegalArgumentException("Primero debes enviar o escribir un mensaje para poder adjuntarle el archivo.");
        }

        copiarYAsociarAdjunto(objetivo, archivoOrigen);
        gestorXML.guardarMensajes(lista);
    }

    private void validarArchivoAdjunto(Path archivoOrigen) {
        File archivo = archivoOrigen.toFile();
        if (!archivo.exists() || !archivo.isFile()) {
            throw new IllegalArgumentException("El archivo '" + archivo.getName() + "' no existe en src/main/resources.");
        }

        long tamanio = archivo.length();
        if (tamanio > TAMANIO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el límite permitido de 10 MB.");
        }

        String extension = obtenerExtension(archivo.getName());
        if (!EXTENSIONES_VALIDAS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Extensión no permitida (." + extension + "). Permitidas: " + EXTENSIONES_VALIDAS);
        }
    }

    private void copiarYAsociarAdjunto(Mensaje mensaje, Path archivoOrigen) throws IOException {
        if (!CARPETA_MEDIA.exists()) {
            CARPETA_MEDIA.mkdirs();
        }

        File origen = archivoOrigen.toFile();
        String nombreFinal = System.currentTimeMillis() + "_" + origen.getName();
        File destino = new File(CARPETA_MEDIA, nombreFinal);

        try (InputStream in = new FileInputStream(origen);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            while ((bytesLeidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLeidos);
            }
        }

        mensaje.setAdjuntoNombre(origen.getName());
        mensaje.setAdjuntoRuta("media/" + nombreFinal);
        mensaje.setAdjuntoTamanio(origen.length());
        mensaje.setAdjuntoTipo(obtenerExtension(origen.getName()).toUpperCase());
    }

    public void exportarOAlmacenarAdjunto(Mensaje mensaje, Path carpetaDestino) throws IOException {
        if (!mensaje.tieneAdjunto()) {
            throw new IllegalArgumentException("Este mensaje no tiene ningún archivo adjunto.");
        }
        File origen = new File(mensaje.getAdjuntoRuta());
        if (!origen.exists()) {
            throw new IOException("El archivo adjunto original ya no se encuentra en la carpeta media/.");
        }

        File carpeta = carpetaDestino.toFile();
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        File destino = new File(carpeta, mensaje.getAdjuntoNombre());
        try (InputStream in = new FileInputStream(origen);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, leidos);
            }
        }
    }

    public void abrirAdjunto(Mensaje mensaje) throws IOException {
        if (!mensaje.tieneAdjunto()) {
            throw new IllegalArgumentException("El mensaje no tiene adjunto.");
        }
        File archivo = new File(mensaje.getAdjuntoRuta());
        if (!archivo.exists()) {
            throw new IOException("El archivo físico no existe en: " + mensaje.getAdjuntoRuta());
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(archivo);
        } else {
            throw new UnsupportedOperationException("El sistema no permite abrir archivos automáticamente.");
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        int punto = nombreArchivo.lastIndexOf('.');
        if (punto == -1 || punto == nombreArchivo.length() - 1) {
            return "";
        }
        return nombreArchivo.substring(punto + 1);
    }

    public void eliminarUsuario(String nombre) throws JAXBException {
        String nombreLimpio = nombre.trim();
        if (!existeUsuarioEstricto(nombreLimpio)) {
            throw new IllegalArgumentException("El usuario '" + nombreLimpio + "' no existe exactamente con ese nombre (comprueba mayúsculas/minúsculas).");
        }

        ListaUsuarios listaUsuarios = gestorXML.leerUsuarios();
        boolean usuarioRemovido = listaUsuarios.getUsuarios().removeIf(u -> u.getNombre() != null && u.getNombre().equals(nombreLimpio));
        if (usuarioRemovido) {
            gestorXML.guardarUsuarios(listaUsuarios);
        }

        ListaMensajes listaMensajes = gestorXML.leerMensajes();
        boolean mensajesModificados = false;

        for (Mensaje m : listaMensajes.getMensajes()) {
            if (m.getEmisor() != null && m.getEmisor().equals(nombreLimpio)) {
                m.setEmisor("[Usuario Eliminado]");
                mensajesModificados = true;
            }
            if (m.getDestinatario() != null && m.getDestinatario().equals(nombreLimpio)) {
                m.setDestinatario("[Usuario Eliminado]");
                mensajesModificados = true;
            }
        }

        if (mensajesModificados) {
            gestorXML.guardarMensajes(listaMensajes);
        }
    }
}