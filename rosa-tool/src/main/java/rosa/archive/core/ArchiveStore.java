package rosa.archive.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.nio.charset.Charset;
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
     * Loads a collection, reporting problems in its files instead of ignoring them.
     *
     * @param collectionId the collection identifier
     * @param errors       list to collect messages about unreadable or malformed files
     * @return the loaded collection
     * @throws IOException if the collection directory cannot be read
     */
    BookCollection loadCollection(String collectionId, List<String> errors) throws IOException;

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
     * Loads a book, reporting problems in its files instead of ignoring them.
     *
     * @param collection the parent collection
     * @param bookId     the book identifier
     * @param errors     list to collect messages about unreadable or malformed files
     * @return the loaded book
     * @throws IOException if the book directory cannot be read
     */
    Book loadBook(BookCollection collection, String bookId, List<String> errors) throws IOException;

    /**
     * Resolves the path of a file in a book directory. The file need not exist.
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param fileName     the file name within the book directory
     * @return the path of the file
     */
    Path bookFile(String collectionId, String bookId, String fileName);

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
     * Checks a book, optionally skipping the image file checks (for shallow copies).
     *
     * @param collection  the parent collection
     * @param book        the book
     * @param checkBits   whether to verify SHA1 checksums
     * @param checkImages whether to verify that listed images exist on disk
     * @return the check result
     */
    CheckResult check(BookCollection collection, Book book, boolean checkBits, boolean checkImages);

    /**
     * Copies metadata files (XML, TXT, HTML, CSV) from the archive to
     * the destination, preserving directory structure and excluding images.
     *
     * @param destination the destination directory
     * @throws IOException if files cannot be copied
     */
    void shallowCopy(Path destination) throws IOException;

    /**
     * Recomputes SHA-1 checksums for all files at the collection level
     * (collection-level CSVs and reference sheets) and writes the updated
     * checksum file.
     *
     * <p>When {@code force} is false, only files whose last-modified time is newer
     * than the existing checksum file, or files missing from the existing checksum
     * file, are recomputed. When {@code force} is true, all files are recomputed
     * unconditionally.
     *
     * @param collectionId the collection identifier
     * @param force        whether to recompute all checksums regardless of modification date
     * @param errors       list to collect error messages
     * @throws IOException if the collection directory cannot be read or the checksum file cannot be written
     */
    void updateChecksum(String collectionId, boolean force, List<String> errors) throws IOException;

    /**
     * Recomputes SHA-1 checksums for all files in a book directory and writes
     * the updated checksum file.
     *
     * <p>When {@code force} is false, only files whose last-modified time is newer
     * than the existing checksum file, or files missing from the existing checksum
     * file, are recomputed. When {@code force} is true, all files are recomputed
     * unconditionally.
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param force        whether to recompute all checksums regardless of modification date
     * @param errors       list to collect error messages
     * @throws IOException if the book directory cannot be read or the checksum file cannot be written
     */
    void updateChecksum(String collectionId, String bookId, boolean force, List<String> errors) throws IOException;

    /**
     * Scans the book directory for image files ({@code .tif}, {@code .jpg}), reads
     * their dimensions, and writes the image list CSV ({@code <bookId>.images.csv}).
     *
     * <p>When {@code force} is false and the image list file already exists, this
     * method does nothing.
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param force        whether to overwrite an existing image list
     * @param errors       list to collect error messages
     * @throws IOException if the book directory cannot be read or the CSV cannot be written
     */
    void generateAndWriteImageList(String collectionId, String bookId, boolean force, List<String> errors) throws IOException;

    /**
     * Crops images in the book directory based on crop data stored in
     * {@code <bookId>.crop.txt} and writes the cropped versions to a
     * {@code cropped/} subdirectory.
     *
     * <p>When {@code force} is false and a cropped image already exists, that
     * image is skipped.
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param force        whether to overwrite existing cropped images
     * @param errors       list to collect error messages
     * @throws IOException if the book directory cannot be read or images cannot be written
     */
    void cropImages(String collectionId, String bookId, boolean force, List<String> errors) throws IOException;

    /**
     * Generates the cropped image list CSV ({@code <bookId>.images.crop.csv})
     * by reading dimensions from images in the {@code cropped/} subdirectory.
     *
     * <p>When {@code force} is false and the cropped image list already exists,
     * this method does nothing.
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param force        whether to overwrite an existing cropped image list
     * @param errors       list to collect error messages
     * @throws IOException if the book directory cannot be read or the CSV cannot be written
     */
    void generateAndWriteCropList(String collectionId, String bookId, boolean force, List<String> errors) throws IOException;

    /**
     * Generates a TEI P5 XML transcription file ({@code BookId.transcription.xml})
     * by combining per-page text transcription files ({@code BookId.transcription.NNNr.txt}).
     *
     * <p>Books that already have a hand-authored {@code .transcription.xml} or that use
     * only AoR annotation XML do not need this step. If no per-page text files are found,
     * this method returns without producing output.
     *
     * @param collectionId the collection containing the book
     * @param bookId       the book to process
     * @param force        replace an existing transcription XML
     * @param inputCharset the character encoding of the per-page text files
     * @param errors       list to collect error messages
     * @param warnings     list to collect warning messages
     * @throws IOException if the book directory cannot be read or the output file cannot be written
     */
    void generateTEITranscriptions(String collectionId, String bookId, boolean force, Charset inputCharset,
                                   List<String> errors, List<String> warnings) throws IOException;

    /**
     * Generates a TEI transcription reading the text files as
     * {@link TranscriptionConverter#DEFAULT_INPUT_CHARSET} (Windows-1252, the encoding of the
     * archive's text files).
     *
     * @see #generateTEITranscriptions(String, String, boolean, Charset, List, List)
     */
    default void generateTEITranscriptions(String collectionId, String bookId, boolean force, List<String> errors,
                                           List<String> warnings) throws IOException {
        generateTEITranscriptions(collectionId, bookId, force, TranscriptionConverter.DEFAULT_INPUT_CHARSET,
                errors, warnings);
    }
}
