package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.serialize.AORAnnotatedPageConstants;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.util.logging.Logger;

public class AnnotationTransformer extends BasePresentationTransformer implements Transformer<Annotation>,
        AORAnnotatedPageConstants {
    private static final Logger logger = Logger.getLogger(AnnotationTransformer.class.toString());

    private ArchiveNameParser imageNameParser;

    @Inject
    public AnnotationTransformer(IIIFRequestFormatter presRequestFormatter, ArchiveNameParser imageNameParser) {
        super(presRequestFormatter);
        this.imageNameParser = imageNameParser;
    }

    @Override
    public Annotation transform(BookCollection collection, Book book, String name) {
        // Find annotation in book

        // Transform archive anno -> iiif anno
        return null;
    }

    @Override
    public Class<Annotation> getType() {
        return Annotation.class;
    }



}
