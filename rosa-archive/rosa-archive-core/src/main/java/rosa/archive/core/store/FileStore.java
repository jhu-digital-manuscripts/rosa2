package rosa.archive.core.store;

import com.google.inject.Inject;
import rosa.archive.core.check.Checker;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link rosa.archive.core.store.Store} based on the structure of the
 * Roman de la Rose Digital Library archive.
 *
 * The RRDL archive has a set file hierarchy with the top level directory containing
 * manuscript or book collections as directories. Each collection directory contains metadata
 * files and directories representing each individual book or manuscript.
 */
public class FileStore implements Store {
    private Path top;

    @Inject
    private Checker<Object> checker;

    public FileStore() {
        // TODO configure TOP
    }

    // Testing
    public FileStore(String top, Checker<Object> checker) {
        this.top = Paths.get(top);
        this.checker = checker;
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
        return checker.checkBits(collection);
    }

    @Override
    public boolean checkBitIntegrity(Book book) {
        return checker.checkBits(book);
    }

    @Override
    public boolean checkContentConsistency(BookCollection collection) {
        return checker.checkContent(collection);
    }

    @Override
    public boolean checkContentConsistency(Book book) {
        return checker.checkContent(book);
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
