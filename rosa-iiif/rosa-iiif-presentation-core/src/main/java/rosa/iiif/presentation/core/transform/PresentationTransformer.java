package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.meta.BiblioData;
import rosa.archive.model.meta.MultilangMetadata;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.AnnotationListType;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Layer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationRequest;
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
import rosa.iiif.presentation.model.selector.FragmentSelector;
import rosa.iiif.presentation.model.HtmlValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

// TODO handle HTML sanitization!
public class PresentationTransformer implements IIIFNames {
    private static final String IMAGE_RANGE_MISC_ID = "misc";
    private static final String IMAGE_RANGE_BODYMATTER_ID = "bodymatter";
    private static final String IMAGE_RANGE_BINDING_ID = "binding";
    private static final String IMAGE_RANGE_ENDMATTER_ID = "endmatter";
    private static final String IMAGE_RANGE_FRONTMATTER_ID = "frontmatter";
    private static final String DEFAULT_SEQUENCE_LABEL = "reading-order";
    private static final String PAGE_REGEX = "\\d{1,3}(r|v|R|V)";
    private static final String TOP_RANGE_ID = "top";
    private static final String ILLUSTRATION_RANGE_TYPE = "illus";
    private static final String IMAGE_RANGE_TYPE = "image";
    private static final String TEXT_RANGE_TYPE = "text";
    
    private static int annotation_counter = 0;    

    private final IIIFRequestFormatter requestFormatter;
    private final rosa.iiif.image.core.IIIFRequestFormatter imageFormatter;
    private final ImageIdMapper imageIdMapper;

    @Inject
    public PresentationTransformer(IIIFRequestFormatter requestFormatter,
                                   rosa.iiif.image.core.IIIFRequestFormatter imageFormatter,
                                   ImageIdMapper imageIdMapper) {
        this.requestFormatter = requestFormatter;
        this.imageFormatter = imageFormatter;
        this.imageIdMapper = imageIdMapper;
    }

    /**
     *
     *
     * @param collection book collection holding the book
     * @param book book to transform
     * @return manifest
     */
    public Manifest manifest(BookCollection collection, Book book) {
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

    public AnnotationList annotationList(BookCollection collection, Book book, String page, String listType) {
        Canvas canvas = canvas(collection, book, page);
        AnnotatedPage aPage = book.getAnnotationPage(page);
        AnnotationListType type = AnnotationListType.getType(listType);

        if (type == AnnotationListType.ALL) {
            return annotationList(collection, book, canvas, aPage);
        }
        return annotationList(collection, book, canvas, aPage, type);
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

//        manifest.setRanges(buildTopRanges(collection, book));
        
        return manifest;
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
    
    // TODO Put this image name stuff somewhere else
    

    public BookImage guessImage(Book book, String frag) {
            frag = frag.trim();

            if (frag.matches("\\d+")) {
                    frag += "r";
            }

            if (frag.matches("\\d[rRvV]")) {
                    frag = "00" + frag;
            } else if (frag.matches("\\d\\d[rRvV]")) {
                    frag = "0" + frag;
            }

            if (!frag.endsWith(".tif")) {
                    frag += ".tif";
            }

            if (!frag.startsWith(book.getId())) {
                    frag = book.getId() + "." + frag;
            }

            for (BookImage image: book.getImages()) {
                if (image.getId().equalsIgnoreCase(frag)) {
                    return image;
                }
            }

            return null;
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
     * Handle the book's structured metadata and transform it into Manifest metadata.
     *
     * @param book book
     * @param languages languages available
     * @param manifest manifest to add the metadata
     */
    private void transformMetadata(Book book, String[] languages, Manifest manifest) {
        Map<String, HtmlValue> map = new HashMap<>();

        for (String lang : languages) {
            BookMetadata metadata = book.getBookMetadata(lang);

            map.put("currentLocation", new HtmlValue(metadata.getCurrentLocation(), lang));
            map.put("repository", new HtmlValue(metadata.getRepository(), lang));
            map.put("shelfmark", new HtmlValue(metadata.getShelfmark(), lang));
            map.put("origin", new HtmlValue(metadata.getOrigin(), lang));

            if (metadata.getWidth() != -1) {
                map.put("width", new HtmlValue(metadata.getWidth() + "", lang));
            }
            if (metadata.getHeight() != -1) {
                map.put("height", new HtmlValue(metadata.getHeight() + "", lang));
            }
            if (metadata.getYearStart() != -1) {
                map.put("yearStart", new HtmlValue(metadata.getYearStart() + "", lang));
            }
            if (metadata.getYearEnd() != -1) {
                map.put("yearEnd", new HtmlValue(metadata.getYearEnd() + "", lang));
            }
            if (metadata.getNumberOfPages() != -1) {
                map.put("numberOfPages", new HtmlValue(metadata.getNumberOfPages() + "", lang));
            }
            if (metadata.getNumberOfIllustrations() != -1) {
                map.put("numberOfIllustrations", new HtmlValue(metadata.getNumberOfIllustrations() + "", lang));
            }
            if (metadata.getTitle() != null) {
                map.put("title", new HtmlValue(metadata.getTitle(), lang));
            }
            if (metadata.getDate() != null) {
                map.put("date", new HtmlValue(metadata.getDate(), lang));
            }
            if (metadata.getDimensions() != null) {
                map.put("dimensions", new HtmlValue(metadata.getDimensions(), lang));
            }
            if (metadata.getDimensionUnits() != null) {
                map.put("dimensionUnits", new HtmlValue(metadata.getDimensionUnits(), lang));
            }
            if (metadata.getType() != null) {
                map.put("type", new HtmlValue(metadata.getType(), lang));
            }
            if (metadata.getCommonName() != null) {
                map.put("commonName", new HtmlValue(metadata.getCommonName(), lang));
            }
            if (metadata.getMaterial() != null) {
                map.put("material", new HtmlValue(metadata.getMaterial(), lang));
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
        AnnotationList otherContent = annotationList(collection, book, canvas, book.getAnnotationPage(image.getPage()));
        if (otherContent != null) {
            Reference ref = new Reference();

            ref.setReference(otherContent.getId());
            ref.setLabel(otherContent.getLabel());
            ref.setType(IIIFNames.SC_ANNOTATION_LIST);

            canvas.setOtherContent(Arrays.asList(ref));
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

        String id_in_image_server = imageFormatter.format(imageIdMapper.mapId(collection, book, image.getId()));
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
     *
     *
     * @param collection book collection holding the book
     * @param book book containing the page
     * @param canvas the Canvas that will hold the annotations
     * @param aPage the annotated page containing the data
     * @return other content, annotations on a page
     */
    private List<AnnotationList> otherContent(BookCollection collection, Book book, Canvas canvas, AnnotatedPage aPage) {
        List<AnnotationList> otherContent = new ArrayList<>();

        for (AnnotationListType type : AnnotationListType.values()) {
            AnnotationList list = annotationList(collection, book, canvas, aPage, type);
            // Add this list to 'otherContent' if it exists and contains annotations
            if (list != null && !list.getAnnotations().isEmpty()) {
                otherContent.add(list);
            }
        }

        return otherContent;
    }

    /**
     * Transform an archive annotation into a Presentation annotation.
     *
     * @param anno an archive annotation
     * @param canvas the canvas
     * @return a IIIF presentation API annotation
     */
    private Annotation adaptAnnotation(BookCollection collection, String book, rosa.archive.model.aor.Annotation anno,
                                       Canvas canvas) {
        Annotation a = new Annotation();
        String annoName = getCanvasLabel(canvas, collection.getAllSupportedLanguages())
                + "_" + annotation_counter++;

        a.setId(urlId(collection.getId(), book, annoName, PresentationRequestType.ANNOTATION));
        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.SC_PAINTING);
        a.setDefaultSource(new AnnotationSource(
                "URI", IIIFNames.DC_TEXT, "text/html", anno.toPrettyString(), "en"
        )); // TODO ask about this, we might not need to make these resolvable

        a.setDefaultTarget(locationOnCanvas(canvas, anno.getLocation()));

        for (String lang : collection.getAllSupportedLanguages()) {
            a.setLabel(annoName, lang);
        }

        return a;
    }

    /**
     * Transform marginalia data into a list of annotations that are associated
     * with a canvas.
     *
     * Marginalia is split into potentially several languages, each of which are
     * split into potentially several locations. Currently, each piece is treated
     * as a new and separate IIIF annotation. TODO these pieces must be linked somehow
     *
     * @param marg AoR marginalia
     * @param canvas the canvas
     * @return list of annotations
     */
    private List<Annotation> adaptMarginalia(BookCollection collection, String book, Marginalia marg, Canvas canvas) {
        List<Annotation> annotations = new ArrayList<>();
        for (MarginaliaLanguage lang : marg.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                Annotation anno = new Annotation();
                String label = getCanvasLabel(canvas, collection.getAllSupportedLanguages())
                        + "_" + annotation_counter++;

                anno.setId(urlId(collection.getId(), book, label, PresentationRequestType.ANNOTATION)); // TODO name
                anno.setMotivation(IIIFNames.SC_PAINTING);
                anno.setDefaultSource(new AnnotationSource(
                        "URI", IIIFNames.DC_TEXT, "text/html",
                        pos.getTexts().toString(), lang.getLang()
                )); // TODO ask about this, we might not need to make these resolvable
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

        target.setSelector(new FragmentSelector(x, y, w, h));

        return target;
    }

    /**
     * @param canvas the canvas
     * @param langs possible languages
     * @return the first label encountered, or an null if list of languages is empty
     */
    private String getCanvasLabel(Canvas canvas, String[] langs) {
        if (langs.length > 0) {
            return canvas.getLabel(langs[0]);
        }
        return null;
    }

    private String urlId(String collection, String book, String name, PresentationRequestType type) {
        return requestFormatter.format(presentationRequest(collection, book, name, type));
    }

    private String presentationId(String collection, String book) {
        return collection + (book == null || book.isEmpty() ? "" : "." + book);
    }

    private PresentationRequest presentationRequest(String collection, String book, String name,
                                                    PresentationRequestType type) {
        return new PresentationRequest(presentationId(collection, book), name, type);
    }

    public Collection transform(BookCollection col) {
        Collection result = new Collection();

        result.setId(urlId(col.getId(), null, col.getId(), PresentationRequestType.COLLECTION));
        result.setLabel(col.getId(), "en");
        result.setType(SC_COLLECTION);
        
        for (String book_id: col.books()) {
            String manifest = requestFormatter.format(presentationRequest(col.getId(), book_id, null, PresentationRequestType.MANIFEST));
            
            Reference ref = new Reference();
            ref.setType(SC_MANIFEST);
            ref.setReference(manifest);
            ref.setLabel(new TextValue(book_id, "en"));
            
            result.getManifests().add(ref);
        }
        
        return result;
    }

    public Collection transform(List<BookCollection> collections) {
        Collection result = new Collection();

        result.setId(urlId("top", null, "top", PresentationRequestType.COLLECTION));
        result.setLabel("top", "en");
        result.setDescription("Top level collection, collecting all other collections in this repository.", "en");
        result.setType(SC_COLLECTION);

        List<Reference> refs = result.getCollections();
        for (BookCollection col : collections) {
            Reference r = new Reference();

            r.setLabel(new TextValue(col.getId(), "en"));
            r.setReference(urlId(col.getId(), null, col.getId(), PresentationRequestType.COLLECTION));
            r.setType(SC_COLLECTION);

            refs.add(r);
        }

        return result;
    }

    private Layer layer(BookCollection collection, Book book, String name) {
        Layer layer = new Layer();

        layer.setType(SC_LAYER);
        layer.setLabel("Layer for " + name, "en");



        return null;
    }

    private AnnotationList annotationList(BookCollection collection, Book book, Canvas canvas, AnnotatedPage aPage,
                                          AnnotationListType listType) {
        AnnotationList list = new AnnotationList();

        String label = annotationListName(canvas.getLabel("en"), listType.toString().toLowerCase());
        list.setId(urlId(collection.getId(), book.getId(), annotationListName(canvas.getLabel("en"),
                listType.toString().toLowerCase()), PresentationRequestType.ANNOTATION_LIST));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + listType.toString().toLowerCase() + " on page "
                + canvas.getLabel("en"), "en");
        list.setLabel(label, "en");

        List<Annotation> annotations = list.getAnnotations();

        // Illustrations annotations do not need Annotated Page TODO refactor to have less confusing returns
        if (listType == AnnotationListType.ILLUSTRATION) {
            List<Annotation> anns = illustrationForPage(collection, book, canvas);
            if (anns == null || anns.isEmpty()) {
                return null;
            }

            annotations.addAll(anns);
            return list;
        }

        // Annotated page can be NULL if no transcriptions are present.
        if (aPage == null) {
            return null;
        }
        switch (listType) {
            case MARGINALIA:
                for (Marginalia marg : aPage.getMarginalia()) {
                    annotations.addAll(adaptMarginalia(collection, book.getId(), marg, canvas));
                }
                break;
            case MARK:
                for (rosa.archive.model.aor.Annotation ann : aPage.getMarks()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, canvas));
                }
                break;
            case SYMBOL:
                for (rosa.archive.model.aor.Annotation ann : aPage.getSymbols()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, canvas));
                }
                break;
            case UNDERLINE:
                for (rosa.archive.model.aor.Annotation ann : aPage.getUnderlines()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, canvas));
                }
                break;
            case NUMBERAL:
                for (rosa.archive.model.aor.Annotation ann : aPage.getNumerals()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, canvas));
                }
                break;
            case ERRATA:
                for (rosa.archive.model.aor.Annotation ann : aPage.getErrata()) {
                    annotations.add(adaptAnnotation(collection, book.getId(), ann, canvas));
                }
                break;
            default:
                break;
        }

        return list;
    }

    private List<Annotation> illustrationForPage(BookCollection collection, Book book, Canvas canvas) {
        String page = canvas.getLabel("en");
        if (book.getIllustrationTagging() == null) {
            return null;
        }

        List<Annotation> anns = new ArrayList<>();
        for (Illustration ill : book.getIllustrationTagging()) {
            String illusPage = guessImage(book, ill.getPage()).getPage();
            if (!illusPage.equals(page)) {
                continue;
            }
            String anno_name = page + ".illustration_" + ill.getId();

            Annotation ann = new Annotation();
            ann.setLabel("Illustration(s) on " + page, "en");
            ann.setId(urlId(collection.getId(), book.getId(), anno_name, PresentationRequestType.ANNOTATION));
            ann.setMotivation(SC_PAINTING);
            ann.setType(OA_ANNOTATION);

            CharacterNames names = collection.getCharacterNames();
            IllustrationTitles titles = collection.getIllustrationTitles();

            // Resolve character name IDs (should be done in archive layer)
            StringBuilder sb_names = new StringBuilder();
            for (String name_id : ill.getCharacters()) {
                String name = names.getNameInLanguage(name_id, "en");

                sb_names.append(name == null ? name_id : name);
                if (!sb_names.toString().isEmpty()) {
                    sb_names.append(", ");
                } else {
                    sb_names.append(' ');
                }
            }

            // Resolve illustration title IDs (should be done in archive layer)
            StringBuilder sb_titles = new StringBuilder();
            for (String title_id : ill.getTitles()) {
                String title = titles.getTitleById(title_id);

                sb_titles.append(title == null ? title_id : title);
                if (!sb_titles.toString().isEmpty()) {
                    sb_titles.append(", ");
                } else {
                    sb_titles.append(' ');
                }
            }

            String text = "<p><b>Illustration</b><br/>" +
                    (ill.getTitles() == null || ill.getTitles().length == 0 ?
                            "" : "  <i>titles</i>: " + sb_titles.toString()) +
                    (ill.getTextualElement() == null || ill.getTextualElement().isEmpty() ?
                            "" : "  <i>textual elements</i>: '" + ill.getTextualElement() + "'<br/>") +
                    (ill.getCostume() == null || ill.getCostume().isEmpty() ?
                            "" : "  <i>costume</i>: '" + ill.getCostume() + "'<br/>") +
                    (ill.getInitials() == null || ill.getInitials().isEmpty() ?
                        "" : "  <i>initials</i>: '" + ill.getInitials() + "'<br/>") +
                    (ill.getObject() == null || ill.getObject().isEmpty() ?
                            "" : "  <i>object</i>: '" + ill.getObject() + "'<br/>") +
                    (ill.getLandscape() == null || ill.getLandscape().isEmpty() ?
                            "" : "  <i>landscape</i>: '" + ill.getLandscape() + "'<br/>") +
                    (ill.getArchitecture() == null || ill.getArchitecture().isEmpty() ?
                            "" : "  <i>architecture</i>: '" + ill.getArchitecture() + "'<br/>") +
                    (ill.getOther() == null || ill.getOther().isEmpty() ?
                            "" : "  <i>other</i>: '" + ill.getObject() + "'<br/>") +
                    (ill.getCharacters() == null || ill.getCharacters().length == 0 ?
                            "" : "  <i>characters</i>: " + sb_names.toString()) +
                    "</p>";

            ann.setDefaultSource(new AnnotationSource("ID", IIIFNames.DC_TEXT, "text/html",
                    text, "en"));
            ann.setDefaultTarget(locationOnCanvas(canvas, Location.INTEXT));

            anns.add(ann);
        }

        return anns;
    }

    private AnnotationList annotationList(BookCollection collection, Book book, Canvas canvas, AnnotatedPage aPage) {
        AnnotationList list = new AnnotationList();

        for (AnnotationListType type : AnnotationListType.values()) {
            AnnotationList l = annotationList(collection, book, canvas, aPage, type);

            if (l != null) {
                list.getAnnotations().addAll(l.getAnnotations());
            }
        }
        String type = AnnotationListType.ALL.toString().toLowerCase();
        String name = annotationListName(canvas.getLabel("en"), type);

        list.setId(urlId(collection.getId(), book.getId(), name, PresentationRequestType.ANNOTATION_LIST));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + type + " on page " + canvas.getLabel("en"), "en");
        list.setLabel(name, "en");

        return list;
    }

    private String annotationListName(String page, String listType) {
        return page + (listType == null ? "" : "." + listType);
    }
}
