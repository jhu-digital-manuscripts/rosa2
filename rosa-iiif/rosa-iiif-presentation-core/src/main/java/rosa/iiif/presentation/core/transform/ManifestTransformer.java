package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.ImageList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.IIIFImageService;
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
        manifest.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        manifest.setSequences(
                Arrays.asList(buildSequence(book, book.getImages()))
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

            map.get("width").addValue(metadata.getWidth()+"", lang);
            map.get("height").addValue(metadata.getHeight()+"", lang);
            map.get("yearStart").addValue(metadata.getYearStart()+"", lang);
            map.get("yearEnd").addValue(metadata.getYearEnd()+"", lang);
            map.get("numberOfPages").addValue(metadata.getNumberOfPages()+"", lang);
            map.get("numberOfIllustrations").addValue(metadata.getNumberOfIllustrations()+"", lang);
            map.get("title").addValue(metadata.getTitle(), lang);
            map.get("date").addValue(metadata.getDate(), lang);
            map.get("currentLocation").addValue(metadata.getCurrentLocation(), lang);
            map.get("repository").addValue(metadata.getRepository(), lang);
            map.get("shelfmark").addValue(metadata.getShelfmark(), lang);
            map.get("origin").addValue(metadata.getOrigin(), lang);
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
    private Sequence buildSequence(Book book, ImageList imageList) {
        if (imageList == null) {
            return null;
        }

        Sequence sequence = new Sequence();
        sequence.setId(imageList.getId());

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

        canvas.setWidth(image.getWidth());
        canvas.setHeight(image.getHeight());
        canvas.setImages(Arrays.asList(imageResource(image)));

        // Set default target of this image to this Canvas
        Annotation defaultImage = imageResource(image);
        defaultImage.getDefaultTarget().setUri(canvas.getId());

        book.getAnnotationPage(image.getPage());

        return canvas;
    }

    private Annotation imageResource(BookImage image) {
        if (image == null) {
            return null;
        }

        Annotation ann = new Annotation();

        ann.setId(image.getId());
        ann.setWidth(image.getWidth());
        ann.setHeight(image.getHeight());
        ann.setMotivation("sc:painting");
        ann.setType("oa:Annotation");

        IIIFImageService imageService = new IIIFImageService();
        AnnotationSource source = new AnnotationSource(
                "URI", "dcterms:Image", "EX: image/tiff"
        );
        source.setService(imageService);

        // Can set target when building Canvas (to the Canvas URI)?
        AnnotationTarget target = new AnnotationTarget("URI");

        ann.setDefaultSource(source);
        ann.setDefaultTarget(target);

        return ann;
    }

}
