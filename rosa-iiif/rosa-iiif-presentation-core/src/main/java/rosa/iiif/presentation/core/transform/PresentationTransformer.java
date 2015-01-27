package rosa.iiif.presentation.core.transform;

import com.google.inject.Inject;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.Illustration;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.AnnotationListType;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.ViewingDirection;
import rosa.iiif.presentation.model.ViewingHint;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PresentationTransformer extends BasePresentationTransformer {
    private ImageIdMapper imageIdMapper;
    private RangeTransformer rangeTransformer;
    private AnnotationListTransformer annoListTransformer;

    @Inject
    public PresentationTransformer(IIIFRequestFormatter presRequestFormatter,
                                   rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter,
                                   ImageIdMapper imageIdMapper, ArchiveNameParser nameParser) {
        super(presRequestFormatter, imageRequestFormatter, nameParser);
        this.imageIdMapper = imageIdMapper;
        this.annoListTransformer = new AnnotationListTransformer(presRequestFormatter, imageRequestFormatter, nameParser);
        this.rangeTransformer = new RangeTransformer(presRequestFormatter, imageRequestFormatter);
    }

    public Collection collection(BookCollection collection) {
        return null;
    }

    public Collection topCollection(List<BookCollection> collections) {
        return null;
    }

    public Manifest manifest(BookCollection collection, Book book) {
        return buildManifest(collection, book);
    }

    public Sequence sequence(BookCollection collection, Book book, String sequenceId) {
        return buildSequence(collection, book, sequenceId, book.getImages());
    }

    /**
     * @param collection book collection holding the book
     * @param book book containing the page
     * @param page page to manifest
     * @return the Canvas representation of a page
     */
    public Canvas canvas(BookCollection collection, Book book, String page) {
        // Look for the image representing 'page'
        for (BookImage image : book.getImages()) {
            if (image.getPage().equals(page)) {
                return buildCanvas(collection, book, image);
            }
        }
        // Return NULL if the page was not found in the list of images
        return null;
    }

    public Range buildRange(BookCollection collection, Book book, String name) {
        return rangeTransformer.buildRange(collection, book, name);
    }

    public AnnotationList annotationList(BookCollection collection, Book book, String page, String type) {
        return annoListTransformer.transform(collection, book, page, type);
    }

    /**
     * Transform a Book in the archive to a IIIF manifest.
     *
     * @param collection book collection holding the book
     * @param book book to manifest
     * @return the manifest
     */
    private Manifest buildManifest(BookCollection collection, Book book) {
        Manifest manifest = new Manifest();

        manifest.setId(urlId(collection.getId(), book.getId(), null, PresentationRequestType.MANIFEST));
        manifest.setType(IIIFNames.SC_MANIFEST);
        manifest.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        manifest.setDefaultSequence(buildSequence(collection, book, DEFAULT_SEQUENCE_LABEL, book.getImages()));
        // setSequences(...) not used, as it sets references to other sequences

        String lc = "en";
        BookMetadata md = book.getBookMetadata(lc);
        manifest.setLabel(md.getCommonName(), lc);
        manifest.setDescription(md.getRepository() + ", " + md.getShelfmark(), lc);

        manifest.addAttribution(book.getPermission(lc).getPermission(), lc);
        manifest.setViewingHint(ViewingHint.PAGED);

        manifest.setMetadata(transformMetadata(book, new String[]{lc}));

        // Set manifest thumbnail, set to thumbnail for default sequence
        if (manifest.getDefaultSequence() != null) {
            manifest.setThumbnailUrl(manifest.getDefaultSequence().getThumbnailUrl());
            manifest.setThumbnailService(manifest.getDefaultSequence().getThumbnailService());
        }

        // TODO ranges
//        manifest.setRanges(buildTopRanges(collection, book));

        return manifest;
    }

    /**
     * Transform an archive image list into a IIIF sequence.
     *
     * @param imageList image list
     * @return sequence
     */
    private Sequence buildSequence(BookCollection collection, Book book, String label, ImageList imageList) {
        if (imageList == null) {
            return null;
        }

        Sequence sequence = new Sequence();
        sequence.setId(urlId(collection.getId(), book.getId(), label, PresentationRequestType.SEQUENCE));
        sequence.setType(IIIFNames.SC_SEQUENCE);
        sequence.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        sequence.setLabel(label, "en");

        List<Canvas> canvases = new ArrayList<>();
        int count = 0;
        boolean hasNotBeenSet = true;
        for (BookImage image : imageList) {
            canvases.add(buildCanvas(collection, book, image));

            // Set the starting point in the sequence to the first page
            // of printed material
            String page = image.getPage();
            if (hasNotBeenSet && page.matches(PAGE_REGEX)) {
                sequence.setStartCanvas(count);
                hasNotBeenSet = false;
            }

            count++;
        }
        sequence.setCanvases(canvases);

        // Set thumbnail for this sequence, set to the thumbnail for the start canvas
        if (sequence.getCanvases().size() > 0) {
            Canvas defaultCanvas = sequence.getCanvases().get(sequence.getStartCanvas());

            sequence.setThumbnailUrl(defaultCanvas.getThumbnailUrl());
            sequence.setThumbnailService(defaultCanvas.getThumbnailService());
        }

        return sequence;
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
        canvas.setType(IIIFNames.SC_CANVAS);
        canvas.setLabel(image.getPage(), "en");

        // Images of bindings or misc images will be displayed as individuals instead of openings
        if (image.getId().contains(IMAGE_RANGE_MISC_ID) || image.getId().contains(IMAGE_RANGE_BINDING_ID)) {
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
                image.isMissing() ? imageResource(collection, book, collection.getMissingImage(), canvas.getId()) :
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

        ann.setId(urlId(collection.getId(), book.getId(), image.getPage(), PresentationRequestType.ANNOTATION));
        ann.setWidth(image.getWidth());
        ann.setHeight(image.getHeight());
        ann.setMotivation(IIIFNames.SC_PAINTING);
        ann.setType(IIIFNames.OA_ANNOTATION);

        ann.setLabel(image.getPage(), "en");

        String id_in_image_server = imageRequestFormatter.format(imageIdMapper.mapId(collection, book, image.getId()));
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
        ref.setType(IIIFNames.SC_ANNOTATION_LIST);

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
            if (ill.getPage().equals(nameParser.page(page))) {
                return true;
            }
        }

        // No illustrations were found, so there no annotations were found for this page
        return false;
    }

}
