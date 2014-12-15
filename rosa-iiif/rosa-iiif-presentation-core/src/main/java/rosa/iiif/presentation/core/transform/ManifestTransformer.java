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
import rosa.iiif.presentation.model.util.MultiLangValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO All IDs should be resolvable IIIF resource IDs!
public class ManifestTransformer {
    private static final Map<String, Manifest> cache;

    static {
        cache = new HashMap<>();
    }

    public ManifestTransformer() {}

    /**
     *
     *
     * @param collection book collection holding the book
     * @param book book to transform
     * @return manifest
     */
    public Manifest transform(BookCollection collection, Book book) {
        // Check cache
        String id = book.getId();
        if (cache.containsKey(id)) {
            return cache.get(id);
        }

        Manifest manifest = buildManifest(collection, book);
        cache.put(id, manifest);
        return manifest;
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
        String[] fields = {
                "width", "height", "title", "date", "yearStart", "yearEnd", "currentLocation",
                "repository", "shelfmark", "origin", "dimensions", "dimensionUnits", "type",
                "commonName", "material", "numberOfIllustrations", "numberOfPages"
        };
        Map<String, MultiLangValue> map = prepareMetadataMap(fields);

        for (String lang : languages) {
            BookMetadata metadata = book.getBookMetadata(lang);

            map.get("currentLocation").addValue(metadata.getCurrentLocation(), lang);
            map.get("repository").addValue(metadata.getRepository(), lang);
            map.get("shelfmark").addValue(metadata.getShelfmark(), lang);
            map.get("origin").addValue(metadata.getOrigin(), lang);

            map.get("width").addValue(metadata.getWidth()+"", lang);
            map.get("height").addValue(metadata.getHeight()+"", lang);
            map.get("yearStart").addValue(metadata.getYearStart()+"", lang);
            map.get("yearEnd").addValue(metadata.getYearEnd()+"", lang);
            map.get("numberOfPages").addValue(metadata.getNumberOfPages()+"", lang);
            map.get("numberOfIllustrations").addValue(metadata.getNumberOfIllustrations()+"", lang);
            map.get("title").addValue(metadata.getTitle(), lang);
            map.get("date").addValue(metadata.getDate(), lang);
            map.get("dimensions").addValue(metadata.getDimensions(), lang);
            map.get("dimensionUnits").addValue(metadata.getDimensionUnits(), lang);
            map.get("type").addValue(metadata.getType(), lang);
            map.get("commonName").addValue(metadata.getCommonName(), lang);
            map.get("material").addValue(metadata.getMaterial(), lang);

            // TODO book texts
        }

        manifest.setMetadata(map);
    }

    /**
     * @param fields all fields
     * @return map to hold metadata
     */
    private Map<String, MultiLangValue> prepareMetadataMap(String[] fields) {
        Map<String, MultiLangValue> map = new HashMap<>();

        for (String str : fields) {
            map.put(str, new MultiLangValue());
        }

        return map;
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
            sequence.addLabel("Default", lang);
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

    private AnnotationTarget locationOnCanvas(Canvas canvas, Location location) {
        // TODO
        // For now, everything will default to FULL_PAGE (selector == null)
        return new AnnotationTarget(canvas.getId(), null);
    }

}
