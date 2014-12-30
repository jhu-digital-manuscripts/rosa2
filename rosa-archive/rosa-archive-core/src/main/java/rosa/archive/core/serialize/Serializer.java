package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import rosa.archive.core.ArchiveConstants;

/**
 *
 */
public interface Serializer<T> extends ArchiveConstants {

    /**
     * Read an input stream to construct a data model object.
     * 
     * @param is
     *            input stream
     * @return object from persistent store
     * @throws java.io.IOException
     */
    T read(InputStream is, List<String> errors) throws IOException;

    /**
     * Output a data model object to an output stream.
     * 
     * @param object
     *            object to write
     * @param out
     *            output stream
     * @throws java.io.IOException
     */
    void write(T object, OutputStream out) throws IOException;

    /**
     * @return supported object type
     */
    Class<T> getObjectType();
}
