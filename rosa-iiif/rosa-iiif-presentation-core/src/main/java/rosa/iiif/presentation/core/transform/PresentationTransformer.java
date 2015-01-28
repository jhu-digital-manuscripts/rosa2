package rosa.iiif.presentation.core.transform;

import com.google.inject.Inject;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.impl.BasePresentationTransformer;
import rosa.iiif.presentation.core.transform.impl.CollectionTransformer;
import rosa.iiif.presentation.core.transform.impl.TransformerSet;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Sequence;

import java.util.List;

public class PresentationTransformer extends BasePresentationTransformer {
    private CollectionTransformer collectionTransformer;
    private TransformerSet transformers;

    @Inject
    public PresentationTransformer(IIIFRequestFormatter presRequestFormatter,
                                   ArchiveNameParser nameParser,
                                   TransformerSet transformers,
                                   CollectionTransformer collectionTransformer) {
        super(presRequestFormatter, nameParser);
        this.transformers = transformers;
        this.collectionTransformer = collectionTransformer;
    }

    public Manifest manifest(BookCollection collection, Book book) {
        return transformers.getTransformer(Manifest.class).transform(collection, book, null);
    }

    public Sequence sequence(BookCollection collection, Book book, String sequenceId) {
        return transformers.getTransformer(Sequence.class).transform(collection, book, sequenceId);
    }

    /**
     * @param collection book collection holding the book
     * @param book book containing the page
     * @param page page to manifest
     * @return the Canvas representation of a page
     */
    public Canvas canvas(BookCollection collection, Book book, String page) {
        return transformers.getTransformer(Canvas.class).transform(collection, book, page);
    }

    public Range buildRange(BookCollection collection, Book book, String name) {
        return transformers.getTransformer(Range.class).transform(collection, book, name);
    }

    public AnnotationList annotationList(BookCollection collection, Book book, String name) {
        return transformers.getTransformer(AnnotationList.class).transform(collection, book, name);
    }

    public Collection collection(BookCollection collection) {
        return collectionTransformer.collection(collection);
    }

    public Collection topCollection(List<BookCollection> collections) {
        return collectionTransformer.topCollection(collections);
    }


}
