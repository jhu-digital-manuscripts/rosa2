package rosa.archive.core.serialize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import rosa.archive.model.HasId;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public abstract class BaseSerializerTest<T extends HasId> {

    protected Serializer<T> serializer;

    @Test
    public abstract void readTest() throws IOException;

    @Test
    public abstract void writeTest() throws IOException;

    /**
     * @param name path of test resource
     * @return resource as an InputStream
     * @throws IOException
     */
    public InputStream getResourceAsStream(String name) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    /**
     *
     *
     * @param name path of test resource
     * @param errors list to store errors found while loading resource
     * @return the object
     * @throws IOException
     */
    public T loadResource(String name, List<String> errors) throws IOException {
        assertNotNull("Serializer not set.", serializer);

        try (InputStream in = getResourceAsStream(name)) {
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
     * @param name path of test resource
     * @return the object
     * @throws IOException
     */
    public T loadResource(String name) throws IOException {
        return loadResource(name, new ArrayList<String>());
    }

    /**
     * @param object object to write
     * @return list of all lines written
     * @throws IOException
     */
    public List<String> writeObjectAndGetWrittenLines(T object) throws IOException {
        String output = writeObjectAndGetContent(object);
        return Arrays.asList(output.split("\\n"));
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
