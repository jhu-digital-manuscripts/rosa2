package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.Layer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Sequence;

import java.util.List;

public class PresentationTransformerImpl extends BasePresentationTransformer implements PresentationTransformer {
    private CollectionTransformer collectionTransformer;
    private TransformerSet transformers;

    @Inject
    public PresentationTransformerImpl(IIIFRequestFormatter presRequestFormatter,
                                       TransformerSet transformers,
                                       CollectionTransformer collectionTransformer) {
        super(presRequestFormatter);
        this.transformers = transformers;
        this.collectionTransformer = collectionTransformer;
    }

    @Override
    public Manifest manifest(BookCollection collection, Book book) {
        return transformers.getTransformer(Manifest.class).transform(collection, book, null);
    }

    @Override
    public Sequence sequence(BookCollection collection, Book book, String sequenceId) {
        return transformers.getTransformer(Sequence.class).transform(collection, book, sequenceId);
    }

    @Override
    public Canvas canvas(BookCollection collection, Book book, String page) {
        return transformers.getTransformer(Canvas.class).transform(collection, book, page);
    }

    @Override
    public Range range(BookCollection collection, Book book, String name) {
        return transformers.getTransformer(Range.class).transform(collection, book, name);
    }

    @Override
    public Layer layer(BookCollection collection, Book book, String name) {
        return transformers.getTransformer(Layer.class).transform(collection, book, name);
    }

    @Override
    public AnnotationList annotationList(BookCollection collection, Book book, String name) {
        return transformers.getTransformer(AnnotationList.class).transform(collection, book, name);
    }

    @Override
    public Collection collection(BookCollection collection) {
        return collectionTransformer.collection(collection);
    }

    @Override
    public Collection topCollection(List<BookCollection> collections) {
        return collectionTransformer.topCollection(collections);
    }
}
