package rosa.archive.core.store;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 *
 */
public interface Store {

    // TODO listBookCollection()
    // return array of strings of IDs?

    // Open the collection directory
    // Use BookCollectionSerializer to read collection directory and build BookCollection
    public BookCollection loadBookCollection(String collectionId);

    // Open the book archive directory
    // Use BookSerializer and any other associated serializers to build Book
    public Book loadBook(String collectionId, String bookId);

    // TODO checkBitIntegrity
    public boolean checkBitIntegrity(Object o);

    // TODO checkContent
    public boolean checkContentConsistency(Object o);

    // TODO the following methods will not be in first iteration!
    // updateBook(...)
    // addBook(...)
    // updateBaseData(...)?
    // updateDerivedData(...)?

}
