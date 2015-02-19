package rosa.archive.core;

import java.io.IOException;
import java.util.List;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 *
 */
public interface Store {

    /**
     * List all of the book collections that are present in the archive.
     *
     * @return array of collection names that exist in the archive
     * @throws IOException
     *          if the Store is configured incorrectly, and the archive is
     *          set to a bad location
     */
    String[] listBookCollections() throws IOException;

    /**
     * List all of the books in a particular collection in the archive.
     *
     * @param collectionId
     *          ID of collection to investigate
     * @return
     *          array of items that exists in the collection
     * @throws java.io.IOException
     *          if the specified collection does not exist
     */
    String[] listBooks(String collectionId) throws IOException;

    /**
     * From the collection id, load the collection from the archive and get a BookCollection
     * object.
     *
     * @param collectionId
     *          id of the collection
     * @param errors
     *          list of errors encountered while loading
     * @return
     *          BookCollection object
     * @throws IOException
     *          if the collection does not exist
     */
    BookCollection loadBookCollection(String collectionId, List<String> errors) throws IOException;

    /**
     * Get a single book from the archive that exists in the specified collection.
     *
     * @param collection
     *          id of the collection
     * @param bookId
     *          id of the book to get
     * @param errors
     *          list of errors encountered while loading
     * @return
     *          Book object
     * @throws IOException
     *          if the book or collection does not exist
     */
    Book loadBook(BookCollection collection, String bookId, List<String> errors) throws IOException;

    /**
     * Check the internal data consistency and bit integrity of an archive within this Store.
     *
     * <p>
     *     This method checks an archive data model object for the correct data structure.
     *     Certain necessary objects within a data model object are checked to exist, and
     *     it is ensured that the underlying data exists in the archive. If the object
     *     holds references to other items in the archive, those references are followed
     *     to make sure the items are readable. The bit level content of these items are not
     *     checked, instead this method only checks to see if all necessary items exist and
     *     are readable.
     * </p>
     * <p>
     *     If {@code checkBits} is TRUE, the bit values of each item in the archive is checked
     *     against known values to ensure that the bits that you read are the bits that you
     *     want, and that all of the data is valid.
     * </p>
     *
     * @param collection book collection
     * @param book book to check
     * @param checkBits check bit integrity?
     * @param errors list of errors encountered while checking
     * @param warnings list of errors encountered while checking
     * @return TRUE if data checks complete with no errors, FALSE otherwise
     */
    boolean check(BookCollection collection, Book book, boolean checkBits, List<String> errors, List<String> warnings);

    /**
     * See {@link #check(rosa.archive.model.BookCollection, rosa.archive.model.Book,
     * boolean, java.util.List, java.util.List)}
     *
     * @param collection collection to check
     * @param checkBits check bit integrity?
     * @param errors list of errors encountered while checking
     * @param warnings list of errors encountered while checking
     * @return TRUE if data checks complete with no errors, FALSE otherwise
     */
    boolean check(BookCollection collection, boolean checkBits, List<String> errors, List<String> warnings);

    /**
     * Update the checksum data in the archive for the specified collection.
     * This checksum data consists of items kept by the collection, but exist
     * outside of any book.
     *
     * @param collection name of the collection to update
     * @param force force the update? If false, any items that are unchanged
     *              are skipped
     * @param errors list of errors encountered while updating
     * @return TRUE if the update was successful, FALSE otherwise
     * @throws IOException
     *          if the collection does not exist, or the Store fails to load
     *          an item in the archive
     */
    boolean updateChecksum(String collection, boolean force, List<String> errors) throws IOException;

    /**
     * Update the checksum data in the archive for the specified collection.
     * This checksum data consists of items kept by the collection, but exist
     * outside of any book.
     *
     * @param collection book collection to update
     * @param force force the update? If false, any items that are unchanged
     *              will be skipped
     * @param errors list of errors encountered while updating
     * @return TRUE if the update was successful, FALSE otherwise
     * @throws IOException
     *          if the collection does not exist, or the Store fails to load an
     *          item in the archive
     */
    boolean updateChecksum(BookCollection collection, boolean force, List<String> errors) throws IOException;

    /**
     * Update the checksum data in the archive for the specified book in a
     * collection. If the update operation is NOT forced, then checksum
     * values will only be calculated and updated for those items that have
     * been touched since the last update.
     *
     * @param collection book collection
     * @param book book to update
     * @param force force the operation? If false, any item that is unchanged since
     *              last update will be skipped.
     * @param errors list of errors encountered while updating
     * @return TRUE if the update was successful, FALSE otherwise
     * @throws IOException
     *          if the collection or book does not exist, or the Store fails to
     *          load an item in the archive
     */
    boolean updateChecksum(String collection, String book, boolean force, List<String> errors) throws IOException;

    /**
     * Update the checksum data in the archive for the specified book in a
     * collection. If the update operation is NOT forced, then checksum
     * values will only be calculated and updated for those items that have
     * been touched since the last update.
     *
     * @param collection book collection
     * @param book book to update
     * @param force force the operation? If false, any item that is unchanged since
     *              last update will be skipped.
     * @param errors list of errors encountered while updating
     * @return TRUE if the update was successful, FALSE otherwise
     * @throws IOException
     *          if the collection or book does not exist, or the Store fails to
     *          load an item in the archive
     */
    boolean updateChecksum(BookCollection collection, Book book, boolean force, List<String> errors) throws IOException;

    /**
     * Generate an image list of images in the book. This image list records
     * the file name, dimensions, and ordering of the images.
     *
     * @param collection id of the collection
     * @param book id of the book
     * @param force force the operation?
     * @param errors list of errors
     * @throws IOException
     *          if the collection or book do not exist, or the Store fails
     *          to write the image list to the archive
     */
    void generateAndWriteImageList(String collection, String book, boolean force, List<String> errors) throws IOException;

    /**
     * Generate an image list for cropped images.
     *
     * @param collection id of the collection
     * @param book id of the book
     * @param force force the operation?
     * @param errors list of errors
     * @throws IOException
     *          if the book or collection do not exist, or the Store fails
     *          to write the image list to the archive
     */
    void generateAndWriteCropList(String collection, String book, boolean force, List<String> errors) throws IOException;

    /**
     * Crop images in a book. If cropping information is available for the images
     * of a book, this operation will copy the images into the book's 'cropped'
     * location and crop those images. ImageMagick is required for this
     * operation.
     *
     * @param collection id of the collection
     * @param book id of the book
     * @param force force the operation to complete?
     * @param errors list of errors found while cropping
     * @throws IOException
     *          if the book or collection do not exist, or if the Store fails to
     *          crop images
     */
    void cropImages(String collection, String book, boolean force, List<String> errors) throws IOException;

    /**
     * <p>
     * Generate a file map to map file names of images. The file map associates original image names
     * with new image names that are formatted to work with the archive tools and API. The
     * naming scheme that this tool follows can be seen at
     *
     * <a href="https://github.com/jhu-digital-manuscripts/rosa2/wiki/Rose-File-Naming">
     *      https://github.com/jhu-digital-manuscripts/rosa2/wiki/Rose-File-Naming
     * </a>
     * </p>
     *
     * <p>
     * This function makes certain assumptions about the original naming scheme of the images.
     * <ul>
     *   <li>All images are in normal/reading order.</li>
     *   <li>Front cover will be the first image, if it exists.</li>
     *   <li>If there is a front cover, it will always be followed by a front matter pastedown.</li>
     *   <li>Misc images are last to appear in the image list, if at all.</li>
     *   <li>If a back cover exists, it will always be proceeded by an end matter pastedown.</li>
     *   <li>Back cover will be the image immediately before any misc images, or the last
     * image if no misc images exist.</li>
     * </ul>
     *
     * <p>
     * If these conditions are true, then this tool should work just fine. However, it may
     * be common that these conditions are not necessarily true. If this is the case, the
     * images can be manually changed in order to fit these conditions.
     * </p>
     *
     * <p>
     * For example, if image one in the original naming scheme is a misc image of a color bar and
     * image two in the original naming scheme is an image of the spine, and the rest of
     * the images fit the above conditions, the first two images can be manually moved to
     * a temp location, outside of the book location. The tool can then be run on the remaining
     * images in the book. After the file map is generated, it can be manually edited to
     * include the two images that were moved. These last two images must be moved back
     * into the book location and renamed by hand.
     * </p>
     *
     * @param collection name of collection
     * @param book name of book
     * @param newId new ID to use when renaming files
     * @param hasFrontCover does the book have images of the front cover + pastedown?
     * @param hasBackCover does the book have images of the back cover + pastedown?
     * @param numFrontmatter number of front matter flyleaves
     * @param numEndmatter number of end matter flyleaves
     * @param numMisc number of misc items
     * @param errors list of errors found while generating file map
     * @throws IOException
     *          if the book or collection do not exist or if the Store fails to
     *          write the file map to the archive
     */
    void generateFileMap(String collection, String book, String newId, boolean hasFrontCover, boolean hasBackCover,
                         int numFrontmatter, int numEndmatter, int numMisc, List<String> errors) throws IOException;

    /**
     * Validate AoR transcriptions against the schema.
     *
     * @param collection id of the collection
     * @param book id of the book
     * @param errors list of errors found during validation
     * @param warnings list of warnings found during validation
     * @throws IOException
     *          if the book or collection do not exist, or if one or more
     *          XML files fail to load
     */
    void validateXml(String collection, String book, List<String> errors, List<String> warnings) throws IOException;

    /**
     * Rename images of a book according to a file map. Does nothing if the file map
     * does not exist.
     *
     * @param collection id of the collection
     * @param book id of the book
     * @param dryRun .
     * @param changeId .
     * @param reverse .
     * @param errors list of errors
     * @throws IOException
     *          if the collection or book do not exist, or if the Store fails
     *          to rename images
     */
    void renameImages(String collection, String book, boolean changeId, boolean reverse, List<String> errors)
            throws IOException;

    /**
     * Rename the AoR transcription files according to the file map. The files are renamed
     * and the file references in the transcriptions are also changed according to
     * the information in the file map. Does nothing if no file map is found.
     *
     * @param collection id of the collection
     * @param book id of the book
     * @param errors list of errors found while renaming
     * @throws IOException
     *          if the collection or book do not exist
     */
    void renameTranscriptions(String collection, String book, List<String> errors) throws IOException;
}
