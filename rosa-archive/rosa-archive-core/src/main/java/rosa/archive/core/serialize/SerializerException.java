package rosa.archive.core.serialize;

/**
 *
 */
public class SerializerException extends Exception {

    SerializerException() { super(); }
    SerializerException(String message) { super(message); }
    SerializerException(Throwable cause) { super(cause); }
    SerializerException(String message, Throwable cause) { super(message, cause); }
    SerializerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
