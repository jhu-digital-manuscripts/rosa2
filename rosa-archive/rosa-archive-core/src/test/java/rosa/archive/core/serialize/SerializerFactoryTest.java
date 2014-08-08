package rosa.archive.core.serialize;

import org.junit.Test;
import rosa.archive.model.Blank;
import rosa.archive.model.Book;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class SerializerFactoryTest {

    @Test
    public void test() throws Exception {
        BookSerializer serializer = (BookSerializer) SerializerFactory.get(Book.class);

        assertNotNull(serializer);
        assertNull(SerializerFactory.get(Blank.class));

    }

}
