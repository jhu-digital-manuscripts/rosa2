package rosa.archive.core.serialize;

import java.io.IOException;
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
     * @throws java.io.IOException
     */
    E read(InputStream is) throws IOException;

    /**
     * Output a data model object to an output stream.
     *
     * @param object
     *          object to write
     * @param out
     *          output stream
     * @throws java.io.IOException
     */
    void write(E object, OutputStream out) throws IOException ;

}
