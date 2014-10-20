package rosa.iiif.image.core;

public class IIIFException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String param;

    public IIIFException(String message) {
        this(message, (String) null);
    }

    public IIIFException(String message, String parameter) {
        super(message);
        this.param = parameter;
    }

    public IIIFException(String message, Throwable cause) {
        super(message, cause);
        this.param = null;
    }

    public IIIFException(Throwable cause) {
        super(cause);
        this.param = null;
    }

    public String getParameter() {
        return param;
    }
}
