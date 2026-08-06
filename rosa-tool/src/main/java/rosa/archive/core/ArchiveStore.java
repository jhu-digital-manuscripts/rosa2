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
     * Generates a file map ({@code filemap.csv}) that maps existing image filenames
     * to new standardized filenames following archive naming conventions.
     *
     * <p>The generated file map allocates names sequentially: front cover and pastedown
     * (if present), frontmatter flyleaves, body pages in recto/verso pairs, endmatter
     * flyleaves, back pastedown and cover (if present), and misc images.
     *
     * @param collectionId   the collection identifier
     * @param bookId         the book identifier
     * @param newId          the new book ID to use in generated filenames
     * @param hasFrontCover  whether the book has a front cover and pastedown
     * @param hasBackCover   whether the book has a back cover and pastedown
     * @param numFrontmatter number of frontmatter flyleaf images (each produces recto+verso)
     * @param numEndmatter   number of endmatter flyleaf images (each produces recto+verso)
     * @param numMisc        number of miscellaneous images at the end
     * @param errors         list to collect error messages
     * @throws IOException if the book directory cannot be read or the file map cannot be written
     */
    void generateFileMap(String collectionId, String bookId, String newId, boolean hasFrontCover,
                         boolean hasBackCover, int numFrontmatter, int numEndmatter, int numMisc,
                         List<String> errors) throws IOException;

    /**
     * Renames image files in a book directory according to the file map ({@code filemap.csv}).
     *
     * <p>When {@code reverse} is false, renames old filenames to new filenames.
     * When {@code reverse} is true, renames new filenames back to old filenames.
     * When {@code changeId} is true, replaces the ID prefix in image filenames with
     * the book directory name (ignores the file map).
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param changeId     whether to rename by replacing the ID prefix only
     * @param reverse      whether to apply the file map in reverse (new → old)
     * @param errors       list to collect error messages
     * @throws IOException if the book directory cannot be read or files cannot be renamed
     */
    void renameImages(String collectionId, String bookId, boolean changeId, boolean reverse,
                      List<String> errors) throws IOException;

    /**
     * Renames AoR transcription XML files in a book directory according to the file map
     * ({@code filemap.csv}). Also updates the internal {@code page} element's
     * {@code filename} attribute to reference the new image filename.
     *
     * <p>When {@code reverse} is false, renames transcription files to match new image names.
     * When {@code reverse} is true, renames them back to match old image names.
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param reverse      whether to apply the renaming in reverse (new → old)
     * @param errors       list to collect error messages
     * @throws IOException if the book directory cannot be read or files cannot be renamed
     */
    void renameTranscriptions(String collectionId, String bookId, boolean reverse,
                              List<String> errors) throws IOException;

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
     * @param errors       list to collect error messages
     * @param warnings     list to collect warning messages
     * @throws IOException if the book directory cannot be read or the output file cannot be written
     */
    void generateTEITranscriptions(String collectionId, String bookId, List<String> errors,
                                   List<String> warnings) throws IOException;
}
