package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

public interface Transformer<T> {
    T transform(BookCollection collection, Book book, String id);
//    String toJson(T obj);
}
