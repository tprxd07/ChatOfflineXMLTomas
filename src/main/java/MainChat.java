import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainChat {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ServicioChat chat = new ServicioChat();
        int opcion = -1;

        do {
            System.out.println("\n=== CHAT OFFLINE XML ===");
            System.out.println("1. Registrar nuevo usuario");
            System.out.println("2. Entrar al chat (Enviar mensajes / Comandos)");
            System.out.println("3. Ver conversación entre dos usuarios");
            System.out.println("4. Estadísticas globales (Top 5 usuarios y palabras)");
            System.out.println("5. Ver lista de usuarios registrados");
            System.out.println("6. Eliminar usuario");
            System.out.println("0. Salir");
            System.out.print("Elige una opción: ");

            String entrada = sc.nextLine();
            try {
                opcion = Integer.parseInt(entrada.trim());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, introduce un número válido.");
                continue;
            }

            try {
                switch (opcion) {
                    case 1 -> {
                        System.out.print("Nombre del nuevo usuario: ");
                        String nombre = sc.nextLine();
                        chat.registrarUsuario(nombre);
                        System.out.println("Usuario registrado correctamente en usuarios.xml.");
                    }
                    case 2 -> {
                        System.out.print("Tu usuario (Emisor): ");
                        String entradaEmisor = sc.nextLine().trim();
                        System.out.print("Hablar con (Destinatario): ");
                        String entradaDestinatario = sc.nextLine().trim();

                        if (!chat.existeUsuario(entradaEmisor)) {
                            System.out.println("Aviso: El usuario emisor '" + entradaEmisor + "' no está registrado.");
                            break;
                        }
                        if (!chat.existeUsuario(entradaDestinatario)) {
                            System.out.println("Aviso: El usuario destinatario '" + entradaDestinatario + "' no está registrado.");
                            break;
                        }

                        //Recuperamos el nombre con las mayúsculas reales guardadas en XML
                        String emisor = chat.obtenerNombreReal(entradaEmisor);
                        String destinatario = chat.obtenerNombreReal(entradaDestinatario);

                        if (emisor.equalsIgnoreCase(destinatario)) {
                            System.out.println("Aviso: No puedes iniciar un chat contigo mismo.");
                            break;
                        }

                        //Mostrar conversación previa al entrar al chat
                        List<Mensaje> previos = chat.consultarConversacion(emisor, destinatario);
                        System.out.println("\n=======================================================");
                        System.out.println(" Chat con " + destinatario + " (escribe /help para ver los comandos)");
                        System.out.println("=======================================================");
                        if (previos.isEmpty()) {
                            System.out.println(" (Aún no hay mensajes en esta conversación)");
                        } else {
                            for (Mensaje m : previos) {
                                System.out.println(chat.formatearMensajeLinea(m));
                            }
                        }
                        System.out.println("-------------------------------------------------------");

                        //Bucle continuo del chat hasta pulsar Intro vacío
                        while (true) {
                            System.out.print(emisor + " (pulsa Intro para salir) > ");
                            String texto = sc.nextLine();

                            if (texto.isBlank()) {
                                System.out.println("Has salido de la conversación.");
                                break;
                            }

                            String comando = texto.trim();

                            if (comando.equalsIgnoreCase("/help")) {
                                System.out.println("\n--- CONTROLES DEL CHAT ---");
                                System.out.println("  /help      -> Muestra esta lista de comandos");
                                System.out.println("  /list      -> Estadísticas y Top 5 palabras de este chat");
                                System.out.println("  /archivo   -> Adjunta un archivo desde src/main/resources");
                                System.out.println("  /abrir     -> Abre o exporta un archivo adjunto de este chat");
                                System.out.println("  /exportar  -> Exporta esta conversación a un archivo .txt");
                                System.out.println("  /ver       -> Refresca el historial completo del chat");
                                System.out.println("  [Intro]    -> Deja la línea vacía y pulsa Intro para salir\n");

                            } else if (comando.equalsIgnoreCase("/ver")) {
                                List<Mensaje> historial = chat.consultarConversacion(emisor, destinatario);
                                System.out.println("\n--- Historial actualizado ---");
                                for (Mensaje m : historial) {
                                    System.out.println(chat.formatearMensajeLinea(m));
                                }
                                System.out.println("-----------------------------");

                            } else if (comando.equalsIgnoreCase("/list")) {
                                List<Mensaje> conversacion = chat.consultarConversacion(emisor, destinatario);
                                if (conversacion.isEmpty()) {
                                    System.out.println("  No hay mensajes aún en esta conversación.");
                                } else {
                                    System.out.println("\n--- Resumen de este chat ---");
                                    System.out.println("Total de mensajes: " + conversacion.size());

                                    System.out.println("Mensajes por usuario:");
                                    Map<String, Long> conteo = chat.contarMensajesPorUsuario(conversacion);
                                    conteo.forEach((u, cant) ->
                                            System.out.println(" - " + u + ": " + cant + " mensaje(s)")
                                    );

                                    System.out.println("Top 5 palabras más usadas en este chat:");
                                    var top5 = chat.obtenerTop5Palabras(conversacion);
                                    for (int i = 0; i < top5.size(); i++) {
                                        System.out.println(" " + (i + 1) + ". " + top5.get(i));
                                    }
                                    System.out.println("----------------------------\n");
                                }

                            } else if (comando.equalsIgnoreCase("/archivo")) {
                                try {
                                    List<String> disponibles = chat.listarArchivosResources();
                                    if (disponibles.isEmpty()) {
                                        System.out.println("  (No se detectaron archivos en src/main/resources, coloca alguno ahí primero)");
                                    } else {
                                        System.out.println("  Archivos disponibles en resources: " + String.join(", ", disponibles));
                                    }

                                    System.out.print("  Nombre del archivo en resources (ej. foto.png): ");
                                    String nombreArch = sc.nextLine().trim();
                                    if (nombreArch.isEmpty()) {
                                        System.out.println("  Operación cancelada.");
                                        continue;
                                    }

                                    System.out.print("  Mensaje para acompañar el archivo (Intro para adjuntarlo al último enviado): ");
                                    String comentario = sc.nextLine().trim();
                                    if (!comentario.isEmpty()) {
                                        chat.enviarMensaje(emisor, destinatario, comentario);
                                    }

                                    chat.adjuntarArchivoUltimoMensaje(emisor, destinatario, nombreArch);
                                    System.out.println("  [Archivo copiado a media/ y adjuntado al XML correctamente]");
                                } catch (Exception e) {
                                    System.out.println("  No se pudo adjuntar el archivo: " + e.getMessage());
                                }

                            } else if (comando.equalsIgnoreCase("/exportar")) {
                                try {
                                    System.out.print("  Nombre del archivo de salida (ej. chat_" + emisor + "_" + destinatario + ".txt): ");
                                    String salida = sc.nextLine().trim();
                                    if (salida.isEmpty()) {
                                        salida = "chat_" + emisor + "_" + destinatario + ".txt";
                                    } else if (!salida.endsWith(".txt")) {
                                        salida += ".txt";
                                    }
                                    chat.exportarConversacion(emisor, destinatario, Paths.get(salida));
                                    System.out.println("  [Conversación exportada a " + salida + "]");
                                } catch (Exception e) {
                                    System.out.println("  Error al exportar: " + e.getMessage());
                                }

                            } else if (comando.equalsIgnoreCase("/abrir")) {
                                try {
                                    List<Mensaje> conAdjunto = chat.consultarConversacion(emisor, destinatario).stream()
                                            .filter(Mensaje::tieneAdjunto)
                                            .toList();

                                    if (conAdjunto.isEmpty()) {
                                        System.out.println("  No hay archivos adjuntos en esta conversación.");
                                    } else {
                                        for (int i = 0; i < conAdjunto.size(); i++) {
                                            System.out.println("  " + (i + 1) + ". " + chat.formatearMensajeLinea(conAdjunto.get(i)));
                                        }
                                        System.out.print("  Selecciona el número de mensaje (ej. 1): ");
                                        String inputIdx = sc.nextLine().trim();
                                        int idx = Integer.parseInt(inputIdx) - 1;

                                        if (idx >= 0 && idx < conAdjunto.size()) {
                                            Mensaje seleccionado = conAdjunto.get(idx);
                                            System.out.print("  ¿Qué deseas hacer? (1 = Abrir archivo, 2 = Exportar a carpeta): ");
                                            String accion = sc.nextLine().trim();
                                            if (accion.equals("1")) {
                                                chat.abrirAdjunto(seleccionado);
                                                System.out.println("  Abriendo archivo...");
                                            } else if (accion.equals("2")) {
                                                System.out.print("  Carpeta de destino: ");
                                                Path destino = Paths.get(sc.nextLine().trim());
                                                chat.exportarOAlmacenarAdjunto(seleccionado, destino);
                                                System.out.println("  Adjunto exportado correctamente.");
                                            }
                                        } else {
                                            System.out.println("  Número de mensaje no válido.");
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("  Debes introducir un número válido (ej. 1).");
                                } catch (Exception e) {
                                    System.out.println("  Error al gestionar el adjunto: " + e.getMessage());
                                }

                            } else {
                                chat.enviarMensaje(emisor, destinatario, texto);
                            }
                        }
                    }
                    case 3 -> {
                        System.out.print("Primer usuario: ");
                        String u1 = sc.nextLine();
                        System.out.print("Segundo usuario: ");
                        String u2 = sc.nextLine();

                        List<Mensaje> mensajes = chat.consultarConversacion(u1, u2);
                        if (mensajes.isEmpty()) {
                            System.out.println("No hay mensajes registrados entre " + u1 + " y " + u2 + ".");
                        } else {
                            System.out.println("\n--- Historial de conversación ---");
                            for (int i = 0; i < mensajes.size(); i++) {
                                System.out.println((i + 1) + ". " + chat.formatearMensajeLinea(mensajes.get(i)));
                            }
                        }
                    }
                    case 4 -> {
                        List<Mensaje> todosLosMensajes = chat.obtenerTodosLosMensajes();
                        if (todosLosMensajes.isEmpty()) {
                            System.out.println("Aún no se han enviado mensajes en el sistema.");
                        } else {
                            System.out.println("\n=== ESTADÍSTICAS GLOBALES DEL CHAT ===");
                            System.out.println("Total global de mensajes en XML: " + todosLosMensajes.size());

                            System.out.println("\nTop 5 usuarios con más mensajes enviados:");
                            var topUsuarios = chat.obtenerTop5UsuariosGlobales();
                            for (int i = 0; i < topUsuarios.size(); i++) {
                                System.out.println(" " + (i + 1) + ". " + topUsuarios.get(i));
                            }

                            System.out.println("\nTop 5 palabras más repetidas globalmente:");
                            var topPalabras = chat.obtenerTop5Palabras(todosLosMensajes);
                            if (topPalabras.isEmpty()) {
                                System.out.println(" - No hay palabras registradas.");
                            } else {
                                for (int i = 0; i < topPalabras.size(); i++) {
                                    System.out.println(" " + (i + 1) + ". " + topPalabras.get(i));
                                }
                            }
                        }
                    }
                    case 5 -> {
                        List<Usuario> usuarios = chat.listarUsuarios();
                        if (usuarios.isEmpty()) {
                            System.out.println("No hay ningún usuario registrado todavía.");
                        } else {
                            System.out.println("\n--- Usuarios registrados (" + usuarios.size() + ") ---");
                            for (int i = 0; i < usuarios.size(); i++) {
                                System.out.println(" " + (i + 1) + ". " + usuarios.get(i).getNombre());
                            }
                        }
                    }
                    case 6 -> {
                        System.out.print("Introduce el nombre exacto del usuario a eliminar: ");
                        String nombreEliminar = sc.nextLine().trim();

                        if (!chat.existeUsuarioEstricto(nombreEliminar)) {
                            System.out.println("Aviso: No existe ningún usuario con el nombre '" + nombreEliminar + "' (revisa las mayúsculas).");
                            break;
                        }

                        System.out.print("¿Estás seguro de eliminar a '" + nombreEliminar + "' y todos sus mensajes? (s/n): ");
                        String confirmacion = sc.nextLine().trim();
                        if (confirmacion.equalsIgnoreCase("s")) {
                            chat.eliminarUsuario(nombreEliminar);
                            System.out.println("Usuario '" + nombreEliminar + "' y sus datos asociados eliminados correctamente.");
                        } else {
                            System.out.println("Operación cancelada.");
                        }
                    }
                    case 0 -> System.out.println("Cerrando aplicación...");
                    default -> System.out.println("Opción no válida.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Aviso: " + e.getMessage());
            } catch (JAXBException e) {
                System.out.println("Error al procesar el archivo XML: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error inesperado: " + e.getMessage());
            }

        } while (opcion != 0);

        sc.close();
    }
}