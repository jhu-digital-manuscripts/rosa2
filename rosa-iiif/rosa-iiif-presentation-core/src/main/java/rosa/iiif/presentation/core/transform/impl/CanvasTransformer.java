package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.Illustration;
import rosa.archive.model.ImageType;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.ViewingHint;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;

import java.util.Arrays;

public class CanvasTransformer extends BasePresentationTransformer implements Transformer<Canvas> {
    private ImageIdMapper idMapper;
    private rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter;

    @Inject
    public CanvasTransformer(IIIFRequestFormatter presRequestFormatter,
                             rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter,
                             ArchiveNameParser nameParser,
                             ImageIdMapper idMapper) {
        super(presRequestFormatter, nameParser);
        this.idMapper = idMapper;
        this.imageRequestFormatter = imageRequestFormatter;
    }

    /**
     * @param collection book collection holding the book
     * @param book book containing the page
     * @param name page to manifest
     * @return the Canvas representation of a page
     */
    @Override
    public Canvas transform(BookCollection collection, Book book, String name) {
        // Look for the image representing 'page'
        for (BookImage image : book.getImages()) {
            if (image.getPage().equals(name)) {
                return buildCanvas(collection, book, image);
            }
        }
        // Return NULL if the page was not found in the list of images
        return null;
    }

    @Override
    public Class<Canvas> getType() {
        return Canvas.class;
    }

    /**
     * Transform an archive book image into a IIIF canvas.
     *
     * @param image image object
     * @return canvas
     */
    private Canvas buildCanvas(BookCollection collection, Book book, BookImage image) {
        if (image == null) {
            return null;
        }
        Canvas canvas = new Canvas();
        canvas.setId(urlId(collection.getId(), book.getId(), image.getPage(), PresentationRequestType.CANVAS));
        canvas.setType(SC_CANVAS);
        canvas.setLabel(image.getPage(), "en");

        // Images of bindings or misc images will be displayed as individuals instead of openings
        if (nameParser.type(image) == ImageType.MISC || nameParser.type(image) == ImageType.BINDING) {
            canvas.setViewingHint(ViewingHint.NON_PAGED);
        }

        // If the image is less than 1200 px in either dimension, force the dimensions
        // of the canvas to be double that of the image. TODO hack to prevent canvas dimensions from being ZERO
        int width = image.getWidth() == 0 ? 1 : image.getWidth();
        int height = image.getHeight() == 0 ? 1 : image.getHeight();

        boolean tooSmall = width < 1200 || height < 1200;
        canvas.setWidth(tooSmall ? width * 2 : width);
        canvas.setHeight(tooSmall ? height * 2 : height);

        // Set images to be the single image. Always needs to be at least 1 image with Mirador2
        canvas.setImages(Arrays.asList(
                image.isMissing() ? imageResource(collection, null, collection.getMissingImage(), canvas.getId()) :
                        imageResource(collection, book, image, canvas.getId())));

        // Set 'other content' to be AoR transcriptions as IIIF annotations
        Reference otherContent = annotationList(collection, book, canvas.getLabel("en"));
        if (otherContent != null) {
            canvas.setOtherContent(Arrays.asList(otherContent));
        }

        // TODO add rosa transcriptions as annotations!

        // Add a thumbnail for this canvas, set to its default image
        if (canvas.getImages().size() > 0) {
            Annotation defaultImage = canvas.getImages().get(0);

            canvas.setThumbnailUrl(defaultImage.getDefaultSource().getUri());
            canvas.setThumbnailService(defaultImage.getDefaultSource().getService());
        }

        return canvas;
    }

    /**
     * Transform an image in the archive into an image annotation.
     *
     * @param image an image in the archive
     * @param canvasId ID of the canvas that the image belongs to
     * @return archive image as an annotation
     */
    private Annotation imageResource(BookCollection collection, Book book, BookImage image, String canvasId) {
        if (image == null) {
            return null;
        }

        Annotation ann = new Annotation();

        ann.setId(urlId(collection.getId(), book == null ? "" : book.getId(), image.getPage(),
                PresentationRequestType.ANNOTATION));
        ann.setWidth(image.getWidth());
        ann.setHeight(image.getHeight());
        ann.setMotivation(SC_PAINTING);
        ann.setType(OA_ANNOTATION);

        ann.setLabel(image.getPage(), "en");

        String id_in_image_server = imageRequestFormatter.format(idMapper.mapId(collection, book, image.getId()));
        AnnotationSource source = new AnnotationSource(id_in_image_server, "dcterms:Image", "image/tiff");
        // Can set target when building Canvas (to the Canvas URI)?
        AnnotationTarget target = new AnnotationTarget(canvasId);

        source.setService(new IIIFImageService(IIIF_IMAGE_CONTEXT, id_in_image_server, IIIF_IMAGE_PROFILE_LEVEL2,
                -1, -1, -1, -1, null));

        ann.setDefaultSource(source);
        ann.setDefaultTarget(target);

        return ann;
    }

    /**
     * @param collection archive collection containing the book
     * @param book a book in the archive
     * @param page the page of the book
     * @return a reference to the annotation for a page
     */
    private Reference annotationList(BookCollection collection, Book book, String page) {
        if (!hasAnnotations(book, page)) {
            return null;
        }

        Reference ref = new Reference();

        String name = page + ".all";
        ref.setReference(urlId(collection.getId(), book.getId(), name, PresentationRequestType.ANNOTATION_LIST));
        ref.setLabel(new TextValue(name, "en"));
        ref.setType(SC_ANNOTATION_LIST);

        return ref;
    }

    /**
     * @param book archive book object
     * @param page page in the book in question
     * @return does this page contain annotations?
     */
    private boolean hasAnnotations(Book book, String page) {
        AnnotatedPage aPage = book.getAnnotationPage(page);
        // If the page contains at least one annotation transcription, return true
        if (aPage != null &&
                (!aPage.getMarginalia().isEmpty()
                        || !aPage.getMarks().isEmpty()
                        || !aPage.getSymbols().isEmpty()
                        || !aPage.getNumerals().isEmpty()
                        || !aPage.getErrata().isEmpty()
                        || !aPage.getUnderlines().isEmpty())) {
            return true;
        }

        // If there is no annotated page transcriptions, check for illustrations
        if (book.getIllustrationTagging() == null) {
            return false;
        }

        for (Illustration ill : book.getIllustrationTagging()) {
            // If one illustration is found for this page, there is at least 1 annotation
            if (nameParser.page(ill.getPage()).equals(page)) {
                return true;
            }
        }

        // No illustrations were found, so there no annotations were found for this page
        return false;
    }

}
