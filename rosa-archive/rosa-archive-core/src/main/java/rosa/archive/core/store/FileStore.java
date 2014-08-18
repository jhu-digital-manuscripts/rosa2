package rosa.archive.core.store;

import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.check.Checker;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.serialize.SerializerFactory;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookScene;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FileStore implements Store {
    private Path top;

    private Checker<BookCollection> collectionChecker;
    private Checker<Book> bookChecker;

    public FileStore() {
        // TODO configure TOP

        // TODO dependency injection would be nice....
        this.collectionChecker = new BookCollectionChecker();
        this.bookChecker = new BookChecker();
    }

    // Testing
    public FileStore(String top, Checker<BookCollection> collectionChecker, Checker<Book> bookChecker) {
        this.top = Paths.get(top);
        this.collectionChecker = collectionChecker;
        this.bookChecker = bookChecker;
    }

    @Override
    public String[] listBookCollections() {
        // List the names of all directories in top
        List<String> archives = getSubDirectoryNames(top);
        return archives.toArray(new String[archives.size()]);
    }



    @Override
    public String[] listBooks(String collectionId) {
        Path collection = relativeTo(collectionId, top);

        // Make sure this Path is a directory
        if (!Files.isDirectory(collection)) {
            throw new RuntimeException("[" + collectionId + "] is not a directory!");
        }

        // List the names of all directories in the collection directory
        List<String> books = getSubDirectoryNames(collection);
        return books.toArray(new String[books.size()]);
    }

    @Override
    public BookCollection loadBookCollection(String collectionId) {
        return null;
    }

    @Override
    public Book loadBook(String collectionId, String bookId) {

        Path collectionPath = relativeTo(collectionId, top);
        Path bookPath = relativeTo(bookId, collectionPath);

        if (!Files.isDirectory(bookPath)) {
            return null;
        }

//        Files.newInputStream();

        InputStream input = null;

//        Serializer<Book> serializer = SerializerFactory.serializer(Book.class);
//        return serializer.read(input);
        return null;
    }

    @Override
    public boolean checkBitIntegrity(BookCollection collection) {
        return false;
    }

    @Override
    public boolean checkBitIntegrity(Book book) {
        return false;
    }

    @Override
    public boolean checkContentConsistency(BookCollection collection) {
        return false;
    }

    @Override
    public boolean checkContentConsistency(Book book) {
        return false;
    }

    private Path relativeTo(String id, Path parent) {
        return parent.resolve(id);
    }

    /**
     *
     * @param directory
     *          Path
     * @return
     *          a list of names of all directories in the path
     */
    private List<String> getSubDirectoryNames(Path directory) {
        List<String> children = new ArrayList<>();
        try {

            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory);
            for (Path child : directoryStream) {
                if (Files.isDirectory(child)) {
                    children.add(child.getFileName().toString());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to get collections.", e);
        }
        return children;
    }

}
