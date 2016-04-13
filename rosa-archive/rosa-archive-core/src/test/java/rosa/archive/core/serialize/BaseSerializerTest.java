package rosa.archive.core.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import rosa.archive.model.HasId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public abstract class BaseSerializerTest<T extends HasId> {
    protected static final String COLLECTION_NAME = "valid";
    protected static final String BOOK_NAME = "LudwigXV7";

    protected Serializer<T> serializer;

    @Test
    public abstract void readTest() throws IOException;

    @Test
    public abstract void writeTest() throws IOException;

    @Test
    public void roundTripTest() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        T original = createObject();

        serializer.write(original, out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        List<String> errors = new ArrayList<>();

        T result = serializer.read(in, errors);

        assertTrue("Unexpected errors found while reading.", errors.isEmpty());
        assertEquals("Result and original were not equal.", original, result);
    }

    protected abstract T createObject();

    /**
     * @param path path of test resource
     * @return resource as an InputStream
     * @throws IOException
     */
    public InputStream getResourceAsStream(String path) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    /**
     *
     * @param collection name of collection
     * @param book name of book
     * @param name path of test resource
     * @param errors list to store errors found while loading resource
     * @return the object
     * @throws IOException
     */
    public T loadResource(String collection, String book, String name, List<String> errors) throws IOException {
        assertNotNull("Serializer not set.", serializer);

        String path = "archive/" + collection
                + (book == null || book.isEmpty() ? "" : "/" + book)
                + "/" + name;
        try (InputStream in = getResourceAsStream(path)) {
            T obj = serializer.read(in, errors);
            if (obj == null) {
                return null;
            }

            obj.setId(name);
            return obj;
        }
    }

    /**
     * Load a test resource by name, ignoring errors.
     *
     * @param collection name of collection
     * @param book name of book
     * @param name path of test resource
     * @return the object
     * @throws IOException
     */
    public T loadResource(String collection, String book, String name) throws IOException {
        return loadResource(collection, book, name, new ArrayList<String>());
    }

    /**
     * @param object object to write
     * @return list of all lines written
     * @throws IOException
     */
    public List<String> writeObjectAndGetWrittenLines(T object) throws IOException {
        String output = writeObjectAndGetContent(object);
        return Arrays.asList(output.split(System.lineSeparator()));
    }

    /**
     * @param object object to write
     * @return a String containing all written content
     * @throws IOException
     */
    public String writeObjectAndGetContent(T object) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serializer.write(object, out);
            return out.toString("UTF-8");
        }
    }

}
