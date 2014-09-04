package rosa.archive.core.store;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 *
 */
public interface Store {

    String[] listBookCollections();

    /**
     *
     * @return
     *          array of items that exists in the collection
     */
    String[] listBooks(String collectionId);

    /**
     * From the collection id, load the collection from the archive and get a BookCollection
     * object.
     *
     * @param collectionId
     *          id of the collection
     * @return
     *          BookCollection object
     */
    BookCollection loadBookCollection(String collectionId);

    /**
     * Get a single book from the archive that exists in the specified collection.
     *
     * @param collectionId
     *          id of the collection that the book is a part of
     * @param bookId
     *          id of the book to get
     * @return
     *          Book object
     */
    Book loadBook(String collectionId, String bookId);

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
     * @param book book to check
     * @param checkBits check bit integrity?
     * @return TRUE if data checks complete with no errors, FALSE otherwise
     */
    boolean check(Book book, boolean checkBits);

    /**
     * See {@link #check(rosa.archive.model.Book, boolean)}
     *
     * @param collection collection to check
     * @param checkBits check bit integrity?
     * @return TRUE if data checks complete with no errors, FALSE otherwise
     */
    boolean check(BookCollection collection, boolean checkBits);

    // TODO the following methods will not be in first iteration!
    // updateBook(...)
    // addBook(...)
    // updateBaseData(...)?
    // updateDerivedData(...)?

}
