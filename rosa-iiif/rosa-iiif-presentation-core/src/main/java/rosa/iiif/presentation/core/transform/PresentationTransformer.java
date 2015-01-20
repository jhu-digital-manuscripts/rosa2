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
    private static final String DEFAULT_SEQUENCE_LABEL = "reading-order";
    private static final String PAGE_REGEX = "\\d{1,3}(r|v|R|V)";
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

        return annotationList(collection, book, canvas, aPage, AnnotationListType.getType(listType));
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
        if (image.getId().contains("misc") || image.getId().contains("binding")) {
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
        for (AnnotationList list : otherContent(collection, book, canvas, book.getAnnotationPage(image.getPage()))) {
            Reference ref = new Reference();

            ref.setReference(list.getId());
            ref.setType(SC_ANNOTATION_LIST);
            ref.setLabel(list.getLabel());

            canvas.getOtherContent().add(ref);
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
        // Annotated page can be NULL if no transcriptions are present.
        if (aPage == null) {
            return null;
        }
        AnnotationList list = new AnnotationList();

        String label = annotationListName(canvas.getLabel("en"), listType.toString().toLowerCase());
        list.setId(urlId(collection.getId(), book.getId(), annotationListName(canvas.getLabel("en"),
                listType.toString().toLowerCase()), PresentationRequestType.ANNOTATION_LIST));
        list.setType(SC_ANNOTATION_LIST);
        list.setDescription("Annotation list for " + listType.toString().toLowerCase() + " on page "
                + canvas.getLabel("en"), "en");
        list.setLabel(label, "en");

        List<Annotation> annotations = list.getAnnotations();
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
            case ILLUSTRATION:
                break;
            default:
                break;
        }

        return list;
    }

    private String annotationListName(String page, String listType) {
        return page + (listType == null ? "" : "." + listType);
    }
}
