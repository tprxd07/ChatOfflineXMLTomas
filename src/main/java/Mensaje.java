import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Mensaje {
    private String emisor;
    private String destinatario;
    private String contenido;
    private String fecha;
    private String adjuntoRuta;
    private String adjuntoNombre;
    private long adjuntoTamanio;
    private String adjuntoTipo;

    public Mensaje() {
    }

    public Mensaje(String emisor, String destinatario, String contenido, String fecha) {
        this.emisor = emisor;
        this.destinatario = destinatario;
        this.contenido = contenido;
        this.fecha = fecha;
    }

    public boolean tieneAdjunto() {
        return adjuntoRuta != null && !adjuntoRuta.isBlank();
    }

    public String getEmisor() { return emisor; }
    public void setEmisor(String emisor) { this.emisor = emisor; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getAdjuntoRuta() { return adjuntoRuta; }
    public void setAdjuntoRuta(String adjuntoRuta) { this.adjuntoRuta = adjuntoRuta; }

    public String getAdjuntoNombre() { return adjuntoNombre; }
    public void setAdjuntoNombre(String adjuntoNombre) { this.adjuntoNombre = adjuntoNombre; }

    public long getAdjuntoTamanio() { return adjuntoTamanio; }
    public void setAdjuntoTamanio(long adjuntoTamanio) { this.adjuntoTamanio = adjuntoTamanio; }

    public String getAdjuntoTipo() { return adjuntoTipo; }
    public void setAdjuntoTipo(String adjuntoTipo) { this.adjuntoTipo = adjuntoTipo; }
}