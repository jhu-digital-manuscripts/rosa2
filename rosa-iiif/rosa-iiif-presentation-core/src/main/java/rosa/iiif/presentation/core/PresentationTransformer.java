package rosa.iiif.presentation.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.annotation.Annotation;

public interface PresentationTransformer {
    // TODO need an ID scheme for URLs! collection ID + book ID?
    Book loadBook(String collection, String book);
    BookCollection loadCollection(String collection);

    Manifest manifest(String collection, String book);
    Sequence sequence(String collection, String book, String sequence);
    Canvas canvas(String collection, String book, String canvas);
    Annotation imageResource(String collection, String book, String image);
    Annotation annotation(String collection, String book, String annotation);

//    String toJson(Manifest manifest);
//    String toJson(Sequence sequence);
//    String toJson(Canvas canvas);
//    String toJson(Annotation annotation);
}
