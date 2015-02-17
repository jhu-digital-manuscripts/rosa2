package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

public interface Transformer<T> {
    public T transform(BookCollection collection, Book book, String name);
    public Class<T> getType();
}
