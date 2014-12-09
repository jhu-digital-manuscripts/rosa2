package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.model.annotation.Annotation;

public class AnnotationTransformer implements Transformer<Annotation> {
    @Override
    public Annotation transform(BookCollection collection, Book book, String id) {
        return null;
    }
}
