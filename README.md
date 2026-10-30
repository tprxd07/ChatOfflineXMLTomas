# ChatOfflineXMLTomas
Proyecto en Java para la gestión de un chat offline persistente mediante archivos XML y procesamiento con Streams.

## Funcionalidades
- Gestión de usuarios: Registro, listado y borrado estricto de usuarios (persistencia en usuarios.xml).
- Mensajería offline: Envío y consulta de historiales de conversación bidireccionales (mensajes.xml).
- Análisis con Streams: Estadísticas de mensajes y Top 5 de palabras más repetidas tanto por chat como a nivel global.
- Archivos adjuntos: Copia binaria de ficheros desde src/main/resources hacia la carpeta media/ usando InputStream y OutputStream.

## Comandos de consola dentro del chat:
- /help (ayuda)
- /list (resumen del chat actual)
- /archivo (adjuntar ficheros)
- /abrir (abrir o exportar adjuntos)
- /exportar (guardar chat en .txt)
- /ver (actualizar historial)
