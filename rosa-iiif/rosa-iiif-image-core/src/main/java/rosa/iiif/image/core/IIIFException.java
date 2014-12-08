package rosa.iiif.image.core;

/**
 * Represents an error attempting to excecute a IIIF request. Each error must
 * have a HTTP code.
 */
public class IIIFException extends Exception {
    private static final long serialVersionUID = 1L;
    private final int http_code;

    public IIIFException(Throwable cause, int http_code) {
        super(cause);
        this.http_code = http_code;
    }

    public IIIFException(String message, Throwable cause, int http_code) {
        super(message, cause);
        this.http_code = http_code;
    }

    public IIIFException(String message, int http_code) {
        super(message);
        this.http_code = http_code;
    }

    public int getHttpCode() {
        return http_code;
    }
}
