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
     * Check whether or not the data held in an object is the data that you expect.
     *
     * <p>
     *     This method compares the object on a bit level and compares it to the data
     *     in the archive. True will be returned only if the data model object exactly
     *     matches the data from the archive.
     * </p>
     *
     *
     * @param collection
     *          collection to check
     * @return
     *          TRUE if the data matches the data in the archive, FALSE otherwise
     */
    boolean checkBitIntegrity(BookCollection collection);

    /**
     * Checks whether or not the data in the Book matches the associated data in
     * the archive. See {@link #checkBitIntegrity(rosa.archive.model.BookCollection)}
     * for more description.
     *
     * @param book
     *          book to check
     * @return
     *          TRUE if the data matches the data in the archive, FALSE otherwise
     */
    boolean checkBitIntegrity(Book book);

    /**
     * Check the consistency of the data in a archive model object.
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
     *
     * @param collection
     *          collection to check
     * @return
     *          TRUE if the data is self consistent, FALSE otherwise
     */
    boolean checkContentConsistency(BookCollection collection);

    /**
     * Checks the consistency of the data of a Book. For more description, see
     * {@link #checkContentConsistency(rosa.archive.model.BookCollection)}.
     *
     * @param book
     *          a Book
     * @return
     *          TRUE if the data is self consistent, FALSE otherwise
     */
    boolean checkContentConsistency(Book book);

    // TODO the following methods will not be in first iteration!
    // updateBook(...)
    // addBook(...)
    // updateBaseData(...)?
    // updateDerivedData(...)?

}
