package rosa.archive.core.store;

import rosa.archive.core.check.DataChecker;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.serialize.SerializerFactory;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 *
 */
public class FileStore implements Store {

    private DataChecker dataChecker = new DataChecker();

    @Override
    public BookCollection loadBookCollection(String collectionId) {
        return null;
    }

    @Override
    public Book loadBook(String collectionId, String bookId) {
        Serializer<Book> serializer = SerializerFactory.get(Book.class);
        Book b = serializer.read(null);

        return null;
    }

    @Override
    public boolean checkBitIntegrity(Object o) {
        return dataChecker.checkBits(o);
    }

    @Override
    public boolean checkContentConsistency(Object o) {
        return dataChecker.checkContent(o);
    }

}
