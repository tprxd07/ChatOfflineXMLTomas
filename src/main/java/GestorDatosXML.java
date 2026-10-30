import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;

public class GestorDatosXML {

    private static final File ARCHIVO_USUARIOS = new File("usuarios.xml");
    private static final File ARCHIVO_MENSAJES = new File("mensajes.xml");

    public void guardarUsuarios(ListaUsuarios lista) throws JAXBException {
        JAXBContext contexto = JAXBContext.newInstance(ListaUsuarios.class);
        Marshaller marshaller = contexto.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(lista, ARCHIVO_USUARIOS);
    }

    public ListaUsuarios leerUsuarios() throws JAXBException {
        if (!ARCHIVO_USUARIOS.exists()) {
            return new ListaUsuarios();
        }
        JAXBContext contexto = JAXBContext.newInstance(ListaUsuarios.class);
        Unmarshaller unmarshaller = contexto.createUnmarshaller();
        ListaUsuarios lista = (ListaUsuarios) unmarshaller.unmarshal(ARCHIVO_USUARIOS);
        if (lista.getUsuarios() == null) {
            lista.setUsuarios(new java.util.ArrayList<>());
        }
        return lista;
    }

    public void guardarMensajes(ListaMensajes lista) throws JAXBException {
        JAXBContext contexto = JAXBContext.newInstance(ListaMensajes.class);
        Marshaller marshaller = contexto.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(lista, ARCHIVO_MENSAJES);
    }

    public ListaMensajes leerMensajes() throws JAXBException {
        if (!ARCHIVO_MENSAJES.exists()) {
            return new ListaMensajes();
        }
        JAXBContext contexto = JAXBContext.newInstance(ListaMensajes.class);
        Unmarshaller unmarshaller = contexto.createUnmarshaller();
        ListaMensajes lista = (ListaMensajes) unmarshaller.unmarshal(ARCHIVO_MENSAJES);
        if (lista.getMensajes() == null) {
            lista.setMensajes(new java.util.ArrayList<>());
        }
        return lista;
    }
}