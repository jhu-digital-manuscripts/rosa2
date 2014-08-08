package rosa.archive.core.serialize;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public interface Serializer<E> {

    /**
     * Read an input stream to construct a data model object.
     *
     * @param is
     *          input stream
     * @return
     *          object from persistent store
     */
    E read(InputStream is);

    /**
     * Output a data model object to an output stream.
     *
     * @param object
     *          object to write
     * @param out
     *          output stream
     */
    void write(E object, OutputStream out);

}
