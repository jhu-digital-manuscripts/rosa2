package rosa.archive.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Provides access to archive data stored as a directory hierarchy of collections and books.
 * Each collection contains shared metadata and a set of books. Each book contains
 * images, metadata, annotations, and transcription files.
 */
public sealed interface ArchiveStore permits FileSystemArchiveStore {

    /**
     * Lists all collection identifiers in the archive.
     *
     * @return the collection identifiers
     * @throws IOException if the archive directory cannot be read
     */
    List<String> listCollections() throws IOException;

    /**
     * Lists all book identifiers within a collection.
     *
     * @param collectionId the collection identifier
     * @return the book identifiers
     * @throws IOException if the collection directory cannot be read
     */
    List<String> listBooks(String collectionId) throws IOException;

    /**
     * Loads a collection with all its shared metadata (character names,
     * illustration titles, narrative sections, reference sheets, config).
     *
     * @param collectionId the collection identifier
     * @return the loaded collection
     * @throws IOException if the collection cannot be read
     */
    BookCollection loadCollection(String collectionId) throws IOException;

    /**
     * Loads a book with all its data (images, metadata, annotations, permissions).
     *
     * @param collection the parent collection (provides language info and missing image data)
     * @param bookId     the book identifier
     * @return the loaded book
     * @throws IOException if the book cannot be read
     */
    Book loadBook(BookCollection collection, String bookId) throws IOException;

    /**
     * Checks the structural consistency of a book within its collection.
     *
     * @param collection the parent collection
     * @param book       the book to check
     * @param checkBits  whether to verify SHA-1 checksums
     * @return the check result containing errors and warnings
     */
    CheckResult check(BookCollection collection, Book book, boolean checkBits);

    /**
     * Validates AoR transcription XML files for a book against the schema.
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param errors       list to collect error messages
     * @param warnings     list to collect warning messages
     * @throws IOException if files cannot be read
     */
    void validateXml(String collectionId, String bookId, List<String> errors, List<String> warnings) throws IOException;

    /**
     * Copies metadata files (XML, TXT, HTML, CSV) from the archive to
     * the destination, preserving directory structure and excluding images.
     *
     * @param destination the destination directory
     * @throws IOException if files cannot be copied
     */
    void shallowCopy(Path destination) throws IOException;
}
