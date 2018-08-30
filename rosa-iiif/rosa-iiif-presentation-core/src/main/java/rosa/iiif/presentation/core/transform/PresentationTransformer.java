package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Sequence;


public interface PresentationTransformer {
    Collection collection(BookCollection collection);
    Manifest manifest(BookCollection collection, Book book);
    Sequence sequence(BookCollection collection, Book book, String name);
    Canvas canvas(BookCollection collection, Book book, String name);
    Range range(BookCollection collection, Book book, String name);
    AnnotationList annotationList(BookCollection bookCollection, Book book, String name);
}
