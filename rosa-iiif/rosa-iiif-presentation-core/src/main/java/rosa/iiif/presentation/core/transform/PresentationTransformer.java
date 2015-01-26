package rosa.iiif.presentation.core.transform;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.meta.BiblioData;
import rosa.archive.model.meta.MultilangMetadata;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.model.Canvas;
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

    @Inject
    public PresentationTransformer(IIIFRequestFormatter presRequestFormatter,
                                   rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter,
                                   ImageIdMapper imageIdMapper) {
        super(presRequestFormatter, imageRequestFormatter, imageIdMapper);
    }

    public Manifest transform(BookCollection collection, Book book) {
        return buildManifest(collection, book);
    }

    public Sequence sequence(BookCollection collection, Book book, String sequenceId) {
        return buildSequence(collection, book, sequenceId, book.getImages());
    }

    /**
     * @param collection book collection holding the book
     * @param book book containing the page
     * @param page page to transform
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

    public Range buildRange(BookCollection col, Book book, String name) {
        String[] parts = name.split("\\.");

        if (parts.length != 2) {
            return null;
        }

        String type = parts[0];
        String id = parts[1];

        if (type.equals(ILLUSTRATION_RANGE_TYPE)) {
            return buildIllustrationRange(col, book, id);
        } else if (type.equals(IMAGE_RANGE_TYPE)) {
            return buildImageRange(col, book, id);
        } else if (type.equals(TEXT_RANGE_TYPE)) {
            return buildTextRange(col, book, id);
        } else {
            return null;
        }
    }

    /**
     * Transform a Book in the archive to a IIIF manifest.
     *
     * @param collection book collection holding the book
     * @param book book to transform
     * @return the manifest
     */
    private Manifest buildManifest(BookCollection collection, Book book) {
        Manifest manifest = new Manifest();

        manifest.setId(urlId(collection.getId(), book.getId(), null, PresentationRequestType.MANIFEST));
        manifest.setType(IIIFNames.SC_MANIFEST);
        manifest.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        manifest.setDefaultSequence(buildSequence(collection, book, DEFAULT_SEQUENCE_LABEL, book.getImages()));
        // setSequences(...) not used, as it sets references to other sequences

        MultilangMetadata mmd = book.getMultilangMetadata();
        String lc = "en";

        if (mmd == null) {
            BookMetadata md = book.getBookMetadata(lc);
            manifest.setLabel(md.getCommonName(), lc);
            manifest.setDescription(md.getRepository() + ", " + md.getShelfmark(), lc);
        } else {
            BiblioData bd = mmd.getBiblioDataMap().get(lc);
            manifest.setLabel(bd.getCommonName(), lc);
            manifest.setDescription(bd.getRepository() + ", " + bd.getShelfmark(), lc);
        }

        manifest.addAttribution(book.getPermission(lc).getPermission(), lc);
        manifest.setViewingHint(ViewingHint.PAGED);

        transformMetadata(book, new String[]{lc}, manifest);

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

        for (String lang : collection.getAllSupportedLanguages()) {
            sequence.setLabel(label, lang);
        }

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

    private String constructRangeName(String type, String id) {
        return type + "." + id;
    }

    private String constructRangeURI(BookCollection col, Book book, String range_type, String range_id) {
        return urlId(col.getId(), book.getId(), constructRangeName(range_type, range_id), PresentationRequestType.RANGE);
    }

    private List<Range> buildTopRanges(BookCollection col, Book book) {
        List<Range> result = new ArrayList<>();

        // TODO Looks like ranges need to be embedded, add nicer mechanism to generate all ranges
        result.add(buildRange(col, book, constructRangeName(IMAGE_RANGE_TYPE, TOP_RANGE_ID)));
        result.add(buildRange(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_FRONTMATTER_ID)));
        result.add(buildRange(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_BODYMATTER_ID)));
        result.add(buildRange(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_ENDMATTER_ID)));
        //result.add(buildRange(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_BINDING_ID)));
        //result.add(buildRange(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_MISC_ID)));

//        result.add(buildRange(col, book, constructRangeName(ILLUSTRATION_RANGE_TYPE, TOP_RANGE_ID)));
//        result.add(buildRange(col, book, constructRangeName(TEXT_RANGE_TYPE, TOP_RANGE_ID)));


        Range range = buildRange(col, book, constructRangeName(ILLUSTRATION_RANGE_TYPE, TOP_RANGE_ID));
        int index = 0;

        while (range != null) {
            result.add(range);
            range = buildRange(col, book, constructRangeName(ILLUSTRATION_RANGE_TYPE, "" + index++));
        }

        return result;
    }

    // TODO Better error handling in class
    // Range name is  RANGE_TYPE "." RANGE_ID

    // TODO
    private Range buildTextRange(BookCollection col, Book book, String range_id) {
        return null;
    }

    // TODO refactor image id parsing
    private Range buildImageRange(BookCollection col, Book book, String range_id) {
        Range result = new Range();

        result.setId(constructRangeURI(col, book, IMAGE_RANGE_TYPE, range_id));

        if (range_id.equals(TOP_RANGE_ID)) {
            result.setViewingHint(ViewingHint.TOP);
            result.setLabel(new TextValue("Image Type", "en"));

            List<String> ranges = new ArrayList<>();

            ranges.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_FRONTMATTER_ID));
            ranges.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_BODYMATTER_ID));
            ranges.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_ENDMATTER_ID));

            // TODO Ranges must nest?
            //ranges.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_BINDING_ID));
            //ranges.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_MISC_ID));

            result.setRanges(ranges);
        } else if (range_id.equals(IMAGE_RANGE_FRONTMATTER_ID)) {
            result.setLabel(new TextValue("Front matter", "en"));

            List<String> canvases = new ArrayList<>();

            for (BookImage image : book.getImages()) {
                if (image.getId().contains("frontmatter")) {
                    canvases.add(urlId(col.getId(), book.getId(), image.getPage(), PresentationRequestType.CANVAS));
                }
            }

            result.setCanvases(canvases);
        } else if (range_id.equals(IMAGE_RANGE_ENDMATTER_ID)) {
            result.setLabel(new TextValue("End matter", "en"));

            List<String> canvases = new ArrayList<>();

            for (BookImage image : book.getImages()) {
                if (image.getId().contains("endmatter")) {
                    canvases.add(urlId(col.getId(), book.getId(), image.getPage(), PresentationRequestType.CANVAS));
                }
            }

            result.setCanvases(canvases);
        } else if (range_id.equals(IMAGE_RANGE_BINDING_ID)) {
            result.setLabel(new TextValue("Binding", "en"));

            List<String> canvases = new ArrayList<>();

            for (BookImage image : book.getImages()) {
                if (image.getId().contains("binding")) {
                    canvases.add(urlId(col.getId(), book.getId(), image.getPage(), PresentationRequestType.CANVAS));
                }
            }

            result.setCanvases(canvases);
        } else if (range_id.equals(IMAGE_RANGE_BODYMATTER_ID)) {
            result.setLabel(new TextValue("Body matter", "en"));

            List<String> canvases = new ArrayList<>();

            for (BookImage image : book.getImages()) {
                if (image.getId().split("\\.").length == 3) {
                    canvases.add(urlId(col.getId(), book.getId(), image.getPage(), PresentationRequestType.CANVAS));
                }
            }

            result.setCanvases(canvases);
        } else if (range_id.equals(IMAGE_RANGE_MISC_ID)) {
            result.setLabel(new TextValue("Misc", "en"));
            List<String> canvases = new ArrayList<>();

            for (BookImage image : book.getImages()) {
                if (image.getId().contains("misc")) {
                    canvases.add(urlId(col.getId(), book.getId(), image.getPage(), PresentationRequestType.CANVAS));
                }
            }

            result.setCanvases(canvases);
        }

        return result;
    }

    private Range buildIllustrationRange(BookCollection col, Book book, String range_id) {
        IllustrationTagging tags = book.getIllustrationTagging();

        if (tags == null) {
            return null;
        }

        Range result = new Range();

        result.setId(constructRangeURI(col, book, ILLUSTRATION_RANGE_TYPE, range_id));

        if (range_id.equals(TOP_RANGE_ID)) {
            result.setViewingHint(ViewingHint.TOP);
            result.setLabel("Illustrations", "en");

            List<String> ranges = new ArrayList<>();

            for (int i = 0; i < tags.size(); i++) {
                ranges.add(constructRangeURI(col, book, ILLUSTRATION_RANGE_TYPE, "" + i));
            }

            result.setRanges(ranges);
        } else {
            int index;

            try {
                index = Integer.parseInt(range_id);
            } catch (NumberFormatException e) {
                return null;
            }

            if (index < 0 || index >= tags.size()) {
                return null;
            }

            Illustration illus = tags.getIllustrationData(index);

            IllustrationTitles titles = col.getIllustrationTitles();

            if (titles == null) {
                return null;
            }

            String label = "";

            for (String title_id: illus.getTitles()) {
                String title = titles.getTitleById(title_id);

                if (title != null) {
                    label += (label.isEmpty() ? "" : "; ") + title;
                }
            }

            List<String> canvases = new ArrayList<>();

            BookImage image = guessImage(book, illus.getPage());

            if (image == null) {
                return null;
            }

            canvases.add(urlId(col.getId(), book.getId(), image.getPage(), PresentationRequestType.CANVAS));

            result.setLabel(label, "en");
            result.setCanvases(canvases);
        }

        return result;
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
        for (String lang : collection.getAllSupportedLanguages()) {
            canvas.setLabel(image.getPage(), lang);
        }

        // Images of bindings or misc images will be displayed as individuals
        // instead of openings
        // Canvas elements *should not* have viewing hint = paged?
        if (image.getId().contains(IMAGE_RANGE_MISC_ID) || image.getId().contains(IMAGE_RANGE_BINDING_ID)) {
            canvas.setViewingHint(ViewingHint.NON_PAGED);
        }

        // If the image is less than 1200 px in either dimension, force the dimensions
        // of the canvas to be double that of the image.
        // TODO hack to prevent canvas dimensions from being ZERO
        int width = image.getWidth() == 0 ? 1 : image.getWidth();
        int height = image.getHeight() == 0 ? 1 : image.getHeight();

        boolean tooSmall = width < 1200 || height < 1200;
        canvas.setWidth(tooSmall ? width * 2 : width);
        canvas.setHeight(tooSmall ? height * 2 : height);

        // Set images to be the single image
        // Always needs to be at least 1 image with Mirador2
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

        for (String lang : collection.getAllSupportedLanguages()) {
            ann.setLabel(image.getPage(), lang);
        }

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
            BookImage image = guessImage(book, page);
            if (image != null && ill.getPage().equals(image.getPage())) {
                return true;
            }
        }

        // No illustrations were found, so there no annotations were found for this page
        return false;
    }

}
