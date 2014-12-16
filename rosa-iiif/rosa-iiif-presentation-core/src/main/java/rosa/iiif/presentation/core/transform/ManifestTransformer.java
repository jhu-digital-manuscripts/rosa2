package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.ViewingDirection;
import rosa.iiif.presentation.model.ViewingHint;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.SvgSelector;
import rosa.iiif.presentation.model.selector.SvgType;
import rosa.iiif.presentation.model.util.TextValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO All IDs should be resolvable IIIF resource IDs!
// TODO handle HTML sanitization!
public class ManifestTransformer {

    public ManifestTransformer() {}

    /**
     *
     *
     * @param collection book collection holding the book
     * @param book book to transform
     * @return manifest
     */
    public Manifest transform(BookCollection collection, Book book) {
        return buildManifest(collection, book);
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

        manifest.setId(book.getId());
        manifest.setType(IIIFNames.SC_MANIFEST);
        manifest.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        manifest.setSequences(
                Arrays.asList(buildSequence(book, book.getImages(), collection.getAllSupportedLanguages()))
        );
        manifest.setDefaultSequence(0);

        for (String lang : collection.getAllSupportedLanguages()) {
            manifest.addAttribution(book.getPermission(lang).getPermission(), lang);
        }
        manifest.setViewingHint(ViewingHint.PAGED);
        transformMetadata(book, collection.getAllSupportedLanguages(), manifest);

        return manifest;
    }

    /**
     * Handle the book's structured metadata and transform it into Manifest metadata.
     *
     * @param book book
     * @param languages languages available
     * @param manifest manifest to add the metadata
     */
    private void transformMetadata(Book book, String[] languages, Manifest manifest) {
        Map<String, TextValue> map = new HashMap<>();

        for (String lang : languages) {
            BookMetadata metadata = book.getBookMetadata(lang);

            map.put("currentLocation", new TextValue(metadata.getCurrentLocation(), lang));
            map.put("repository", new TextValue(metadata.getRepository(), lang));
            map.put("shelfmark", new TextValue(metadata.getShelfmark(), lang));
            map.put("origin", new TextValue(metadata.getOrigin(), lang));

            if (metadata.getWidth() != -1) {
                map.put("width", new TextValue(metadata.getWidth() + "", lang));
            }
            if (metadata.getHeight() != -1) {
                map.put("height", new TextValue(metadata.getHeight() + "", lang));
            }
            if (metadata.getYearStart() != -1) {
                map.put("yearStart", new TextValue(metadata.getYearStart() + "", lang));
            }
            if (metadata.getYearEnd() != -1) {
                map.put("yearEnd", new TextValue(metadata.getYearEnd() + "", lang));
            }
            if (metadata.getNumberOfPages() != -1) {
                map.put("numberOfPages", new TextValue(metadata.getNumberOfPages() + "", lang));
            }
            if (metadata.getNumberOfIllustrations() != -1) {
                map.put("numberOfIllustrations", new TextValue(metadata.getNumberOfIllustrations() + "", lang));
            }
            if (metadata.getTitle() != null) {
                map.put("title", new TextValue(metadata.getTitle(), lang));
            }
            if (metadata.getDate() != null) {
                map.put("date", new TextValue(metadata.getDate(), lang));
            }
            if (metadata.getDimensions() != null) {
                map.put("dimensions", new TextValue(metadata.getDimensions(), lang));
            }
            if (metadata.getDimensionUnits() != null) {
                map.put("dimensionUnits", new TextValue(metadata.getDimensionUnits(), lang));
            }
            if (metadata.getType() != null) {
                map.put("type", new TextValue(metadata.getType(), lang));
            }
            if (metadata.getCommonName() != null) {
                map.put("commonName", new TextValue(metadata.getCommonName(), lang));
            }
            if (metadata.getMaterial() != null) {
                map.put("material", new TextValue(metadata.getMaterial(), lang));
            }

            // TODO book texts
        }

        manifest.setMetadata(map);
    }

    /**
     * Transform an archive image list into a IIIF sequence.
     *
     * @param imageList image list
     * @return sequence
     */
    private Sequence buildSequence(Book book, ImageList imageList, String[] langs) {
        if (imageList == null) {
            return null;
        }

        Sequence sequence = new Sequence();
        // TODO make URL
        sequence.setId(imageList.getId());
        sequence.setType(IIIFNames.SC_SEQUENCE);
        sequence.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);

        for (String lang : langs) {
            sequence.setLabel("Default", lang);
        }


        List<Canvas> canvases = new ArrayList<>();
        int count = 0;
        boolean hasNotBeenSet = true;
        for (BookImage image : imageList) {
            canvases.add(buildCanvas(book, image));

            // Set the starting point in the sequence to the first page
            // of printed material
            String page = image.getPage();
            if (hasNotBeenSet && page.matches("\\d{1,3}(r|v|R|V)")) {
                sequence.setStartCanvas(count);
                hasNotBeenSet = false;
            }

            count++;
        }
        sequence.setCanvases(canvases);

        return sequence;
    }

    /**
     * Transform an archive book image into a IIIF canvas.
     *
     * @param image image object
     * @return canvas
     */
    private Canvas buildCanvas(Book book, BookImage image) {
        if (image == null) {
            return null;
        }
        Canvas canvas = new Canvas();
        canvas.setId(image.getId());
        canvas.setType(IIIFNames.SC_CANVAS);

        canvas.setWidth(image.getWidth());
        canvas.setHeight(image.getHeight());
        canvas.setImages(Arrays.asList(imageResource(image, canvas.getId())));

        // Set default target of this image to this Canvas
        canvas.setImages(Arrays.asList(imageResource(image, canvas.getId())));

        List<Annotation> aorAnnotations = annotationsFromAoR(canvas,
                book.getAnnotationPage(image.getPage()));
        canvas.setOtherContent(aorAnnotations);

        return canvas;
    }

    /**
     * Transform an image in the archive into an image annotation.
     *
     * @param image an image in the archive
     * @param canvasId ID of the canvas that the image belongs to
     * @return archive image as an annotation
     */
    private Annotation imageResource(BookImage image, String canvasId) {
        if (image == null) {
            return null;
        }

        Annotation ann = new Annotation();

        ann.setId(image.getId());
        ann.setWidth(image.getWidth());
        ann.setHeight(image.getHeight());
        ann.setMotivation(IIIFNames.SC_PAINTING);
        ann.setType(IIIFNames.OA_ANNOTATION);

        IIIFImageService imageService = new IIIFImageService();
        AnnotationSource source = new AnnotationSource(
                "URI", "dcterms:Image", "EX: image/tiff"
        );
        source.setService(imageService);

        // Can set target when building Canvas (to the Canvas URI)?
        AnnotationTarget target = new AnnotationTarget(canvasId);

        ann.setDefaultSource(source);
        ann.setDefaultTarget(target);

        return ann;
    }

    /**
     * Transform the AoR transcription data into a list of annotations that
     * are associated with a canvas
     *
     * @param canvas the Canvas that will hold the annotations
     * @param aPage the annotated page containing the data
     * @return annotated data as annotations
     */
    private List<Annotation> annotationsFromAoR(Canvas canvas, AnnotatedPage aPage) {
        if (aPage == null) {
            return null;
        }

        List<Annotation> annotations = new ArrayList<>();
        for (Marginalia marg : aPage.getMarginalia()) {
            annotations.addAll(adaptMarginalia(marg, canvas));
        }
        for (rosa.archive.model.aor.Annotation mark : aPage.getMarks()) {
            annotations.add(adaptAnnotation(mark, canvas));
        }
        for (rosa.archive.model.aor.Annotation symbol : aPage.getSymbols()) {
            annotations.add(adaptAnnotation(symbol, canvas));
        }
        for (rosa.archive.model.aor.Annotation underline : aPage.getUnderlines()) {
            annotations.add(adaptAnnotation(underline, canvas));
        }
        for (rosa.archive.model.aor.Annotation numeral : aPage.getNumerals()) {
            annotations.add(adaptAnnotation(numeral, canvas));
        }
        for (rosa.archive.model.aor.Annotation errata : aPage.getErrata()) {
            annotations.add(adaptAnnotation(errata, canvas));
        }

        return annotations;
    }

    /**
     * Transform an archive annotation into a Presentation annotation.
     *
     * @param anno an archive annotation
     * @param canvas the canvas
     * @return a IIIF presentation API annotation
     */
    private Annotation adaptAnnotation(rosa.archive.model.aor.Annotation anno, Canvas canvas) {
        Annotation a = new Annotation();

        a.setId("ID");
        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.SC_PAINTING);
        a.setDefaultSource(new AnnotationSource(
                "URI", IIIFNames.DC_TEXT, "text/html", anno.toPrettyString(), "en"
        ));

        a.setDefaultTarget(locationOnCanvas(canvas, anno.getLocation()));

        return a;
    }

    /**
     * Transform marginalia data into a list of annotations that are associated
     * with a canvas.
     *
     * @param marg AoR marginalia
     * @param canvas the canvas
     * @return list of annotations
     */
    private List<Annotation> adaptMarginalia(Marginalia marg, Canvas canvas) {
        List<Annotation> annotations = new ArrayList<>();
        for (MarginaliaLanguage lang : marg.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                Annotation anno = new Annotation();

                anno.setId("ID");
                anno.setMotivation(IIIFNames.SC_PAINTING);
                anno.setDefaultSource(new AnnotationSource(
                        "URI", IIIFNames.DC_TEXT, "text/html",
                        pos.getTexts().toString(), lang.getLang()
                ));
                anno.setDefaultTarget(locationOnCanvas(canvas, pos.getPlace()));

                annotations.add(anno);
            }
        }

        return annotations;
    }

    /**
     * Guess at the location on a canvas based on the limited location information.
     *
     * @param canvas canvas
     * @param location location on the canvas
     * @return the annotation target
     */
    protected AnnotationTarget locationOnCanvas(Canvas canvas, Location location) {
        double margin_guess = 0.10;

        int x = 0;
        int y = 0;
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        AnnotationTarget target = new AnnotationTarget(canvas.getId());

        // TODO should use a fragment selector for rectangles!
        switch (location) {
            case HEAD:
                h = (int) (canvas.getHeight() * margin_guess);
                break;
            case TAIL:
                y = (int) (canvas.getHeight() * (1 - margin_guess));
                h = (int) (canvas.getHeight() * margin_guess);
                break;
            case LEFT_MARGIN:
                w = (int) (canvas.getWidth() * margin_guess);
                break;
            case RIGHT_MARGIN:
                x = (int) (canvas.getWidth() * (1 - margin_guess));
                w = (int) (canvas.getWidth() * margin_guess);
                break;
            case INTEXT:
                x = (int) (canvas.getWidth() * margin_guess);
                y = (int) (canvas.getHeight() * margin_guess);
                w = (int) (canvas.getWidth() * (1 - 2 * margin_guess));
                h = (int) (canvas.getHeight() * (1 - 2 * margin_guess));
                break;
            case FULL_PAGE:
                return new AnnotationTarget(canvas.getId(), null);
            default:
                throw new IllegalArgumentException("Invalid Location. [" + location + "]");
        }

//        int[][] points = {{x, y}, {x+w, y}, {x+w, y+h}, {x, y+h}, {x, y}};
        int[][] points = {{x, y}, {w, h}};
        target.setSelector(new SvgSelector(SvgType.RECT, points));

        return target;
    }

}
