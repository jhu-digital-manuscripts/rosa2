package rosa.archive.iiif;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import rosa.archive.core.ArchiveStore;
import rosa.archive.core.util.IllustrationRefs;
import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.HTMLAnnotations;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.ImageList;
import rosa.archive.model.ObjectRef;
import rosa.archive.model.Permission;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;

/**
 * Generates IIIF Presentation API 3.0 static JSON files from archive data.
 *
 * <p>Produces a hierarchy of Collection, Manifest, Canvas, and AnnotationPage
 * resources conforming to the IIIF Presentation API 3.0 specification.
 */
public final class IIIFPresentationGenerator {

    private static final String CONTEXT = "http://iiif.io/api/presentation/3/context.json";
    /** Width of thumbnails, in pixels. */
    private static final int THUMBNAIL_WIDTH = 80;

    /** File name of the published TEI book description, next to manifest.json. */
    public static final String DESCRIPTION_FILE = "description.xml";

    /** Language of the published book description. */
    public static final String DESCRIPTION_LANGUAGE = "en";

    /** seeAlso format and profile of the TEI book description. */
    public static final String TEI_FORMAT = "application/tei+xml";
    public static final String TEI_PROFILE = "http://www.tei-c.org/ns/1.0";

    private final IIIFJsonWriter writer;
    private final ObjectMapper mapper;

    /** URL template for the annotation stylesheet, or null for none. */
    private String annotationStylesheetUrl;

    /** Cached transcription conversion for the most recent book. */
    private String cachedTranscriptionKey;
    private Map<String, List<TeiTranscriptionHtml.Column>> cachedTranscriptionPages = Map.of();

    /**
     * Creates a new generator with the given JSON writer.
     *
     * @param writer the IIIF JSON writer for serialization
     */
    public IIIFPresentationGenerator(IIIFJsonWriter writer) {
        this.writer = writer;
        this.mapper = writer.getObjectMapper();
    }

    /**
     * Generates all IIIF Presentation 3.0 files for the archive.
     *
     * <p>Writes a top-level collection, per-collection sub-collections, and per-book manifests
     * to the output directory hierarchy.
     *
     * @param store           the archive store to read data from
     * @param outputDir       the root output directory
     * @param baseUrl         the base URL prefix for resource IDs, or {@code null} for relative IDs
     * @param imageBaseUrl    the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @param imageApiVersion the IIIF Image API version (2 or 3)
     * @throws IOException if an I/O error occurs during generation
     */
    public void generate(ArchiveStore store, Path outputDir, String baseUrl, String imageBaseUrl, int imageApiVersion) throws IOException {
        generate(store, outputDir, baseUrl, imageBaseUrl, imageApiVersion, null);
    }

    /**
     * Generates all IIIF Presentation 3.0 files for the archive, optionally including
     * JHSearch service references and service/jhsearch.json files.
     *
     * <p>Writes a top-level collection, per-collection sub-collections, and per-book manifests
     * to the output directory hierarchy. When opensearchUrl is provided, also generates
     * service/jhsearch.json per collection and adds JHSearchService2 service references to
     * sub-collections and manifests.
     *
     * @param store           the archive store to read data from
     * @param outputDir       the root output directory
     * @param baseUrl         the base URL prefix for resource IDs, or {@code null} for relative IDs
     * @param imageBaseUrl    the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @param imageApiVersion the IIIF Image API version (2 or 3)
     * @param opensearchUrl   the Opensearch _search endpoint URL for JHSearch service, or {@code null} to skip
     * @throws IOException if an I/O error occurs during generation
     */
    public void generate(ArchiveStore store, Path outputDir, String baseUrl, String imageBaseUrl, int imageApiVersion, String opensearchUrl) throws IOException {
        List<String> collectionIds = store.listCollections();

        // Load all collections upfront to determine hierarchy
        Map<String, BookCollection> loadedCollections = new LinkedHashMap<>();
        Set<String> childCollectionIds = new HashSet<>();
        for (String collectionId : collectionIds) {
            BookCollection collection = store.loadCollection(collectionId);
            loadedCollections.put(collectionId, collection);
            // A collection is a child if it has non-empty parents
            String[] parents = collection.getParentCollections();
            if (parents != null && parents.length > 0) {
                childCollectionIds.add(collectionId);
            }
        }

        // Generate top-level collection (excluding child collections from items)
        ObjectNode topCollection = generateTopCollection(collectionIds, baseUrl, childCollectionIds);
        writer.write(topCollection, outputDir.resolve("collection.json"));

        // Generate per-collection sub-collections and manifests
        for (String collectionId : collectionIds) {
            BookCollection collection = loadedCollections.get(collectionId);
            List<String> bookIds = store.listBooks(collectionId);

            // Process books first to collect labels and first image IDs
            Map<String, String> bookLabels = new LinkedHashMap<>();
            Map<String, String> bookFirstImages = new LinkedHashMap<>();
            Map<String, Boolean> bookCroppedFlags = new LinkedHashMap<>();

            for (String bookId : bookIds) {
                Book book = store.loadBook(collection, bookId);
                ImageList images = book.getImages();

                if (images == null || images.getImages().isEmpty()) {
                    System.err.println("WARNING: Skipping book '" + bookId + "' in collection '"
                            + collectionId + "': no images");
                    continue;
                }

                // Collect label for sub-collection
                bookLabels.put(bookId, resolveManifestLabel(collection, book));

                // Collect first image ID for sub-collection thumbnail
                bookFirstImages.put(bookId, images.getImages().get(0).getId());

                // Determine the painting image list: prefer cropped images if available
                ImageList croppedImages = book.getCroppedImages();
                boolean bookHasCropped = croppedImages != null && !croppedImages.getImages().isEmpty();
                bookCroppedFlags.put(bookId, bookHasCropped);
                List<BookImage> paintingImageList = bookHasCropped
                        ? croppedImages.getImages() : images.getImages();

                // Generate annotation pages first to know if reference is needed
                boolean[] hasAnnotationsArray = new boolean[paintingImageList.size()];

                for (int i = 0; i < paintingImageList.size(); i++) {
                    BookImage image = paintingImageList.get(i);
                    ObjectNode annotationPage = generateAnnotationPage(collection, book, image, i, baseUrl);
                    hasAnnotationsArray[i] = (annotationPage != null);

                    if (annotationPage != null) {
                        // Add @context for standalone serving
                        annotationPage.put("@context", CONTEXT);
                        writer.write(annotationPage, outputDir.resolve(collectionId).resolve(bookId)
                                .resolve("canvas").resolve(String.valueOf(i)).resolve("annotations.json"));
                    }
                }

                // Generate manifest with hasAnnotations info
                ObjectNode manifest = generateManifest(collection, book, baseUrl, imageBaseUrl, imageApiVersion, hasAnnotationsArray);

                // Add JHSearch service reference to manifest if opensearchUrl is provided
                if (opensearchUrl != null) {
                    addJHSearchService(manifest, baseUrl, collectionId);
                }

                writer.write(manifest, outputDir.resolve(collectionId).resolve(bookId).resolve("manifest.json"));

                if (hasDisplayableDescription(book)) {
                    Path source = store.bookFile(collectionId, bookId,
                            book.getDescription(DESCRIPTION_LANGUAGE).getId());
                    Path target = outputDir.resolve(collectionId).resolve(bookId).resolve(DESCRIPTION_FILE);
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Write sub-collection AFTER processing books so labels are available
            ObjectNode subCollection = generateSubCollection(collection, bookLabels, bookFirstImages, bookCroppedFlags, baseUrl, imageBaseUrl);

            // Add JHSearch service reference to sub-collection and generate service/jhsearch.json
            if (opensearchUrl != null) {
                addJHSearchService(subCollection, baseUrl, collectionId);

                JHSearchInfoGenerator searchInfoGenerator = new JHSearchInfoGenerator(mapper);
                ObjectNode searchInfo = searchInfoGenerator.generate(collectionId, opensearchUrl);
                writer.write(searchInfo, outputDir.resolve(collectionId).resolve("service").resolve("jhsearch.json"));
            }

            writer.write(subCollection, outputDir.resolve(collectionId).resolve("collection.json"));
        }
    }

    /**
     * Generates the top-level IIIF Collection listing all sub-collections.
     *
     * @param collectionIds the sub-collection identifiers
     * @param baseUrl       the base URL prefix, or {@code null} for relative IDs
     * @return the top-level Collection as a JSON ObjectNode
     */
    public ObjectNode generateTopCollection(List<String> collectionIds, String baseUrl) {
        return generateTopCollection(collectionIds, baseUrl, Set.of());
    }

    /**
     * Generates the top-level IIIF Collection listing only root sub-collections.
     * Child collections (those with non-empty parents) are excluded from the items array.
     *
     * @param collectionIds      the sub-collection identifiers
     * @param baseUrl            the base URL prefix, or {@code null} for relative IDs
     * @param childCollectionIds set of collection IDs that are children (have parents) and should be excluded
     * @return the top-level Collection as a JSON ObjectNode
     */
    public ObjectNode generateTopCollection(List<String> collectionIds, String baseUrl, Set<String> childCollectionIds) {
        ObjectNode node = mapper.createObjectNode();
        node.put("@context", CONTEXT);
        node.put("id", buildId(baseUrl, "collection"));
        node.put("type", "Collection");
        node.set("label", languageMap("none", "rosa2 Archive"));

        ArrayNode items = mapper.createArrayNode();
        for (String collectionId : collectionIds) {
            // Skip child collections — they belong under their parent's sub-collection
            if (childCollectionIds.contains(collectionId)) {
                continue;
            }
            ObjectNode item = mapper.createObjectNode();
            item.put("id", buildId(baseUrl, collectionId + "/collection"));
            item.put("type", "Collection");
            item.set("label", languageMap("none", collectionId));
            items.add(item);
        }
        node.set("items", items);
        return node;
    }

    /**
     * Generates a sub-collection listing all manifests within a collection.
     * If the collection has child collections, they are included as Collection-type items.
     * This backward-compatible overload does not generate thumbnails for manifest items.
     *
     * @param collection the book collection
     * @param bookLabels a map of bookId to label for each book in this collection
     * @param baseUrl    the base URL prefix, or {@code null} for relative IDs
     * @return the sub-collection as a JSON ObjectNode
     */
    public ObjectNode generateSubCollection(BookCollection collection, Map<String, String> bookLabels, String baseUrl) {
        return generateSubCollection(collection, bookLabels, null, baseUrl, null);
    }

    /**
     * Generates a sub-collection listing all manifests within a collection.
     * If the collection has child collections, they are included as Collection-type items.
     * When bookFirstImages is provided, each manifest item includes a thumbnail referencing the book's first image.
     *
     * @param collection      the book collection
     * @param bookLabels      a map of bookId to label for each book in this collection
     * @param bookFirstImages a map of bookId to first image ID (e.g., "LudwigXV7" → "LudwigXV7.001r.tif"), or {@code null} for no thumbnails
     * @param baseUrl         the base URL prefix, or {@code null} for relative IDs
     * @param imageBaseUrl    the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @return the sub-collection as a JSON ObjectNode
     */
    public ObjectNode generateSubCollection(BookCollection collection, Map<String, String> bookLabels,
                                            Map<String, String> bookFirstImages, String baseUrl, String imageBaseUrl) {
        return generateSubCollection(collection, bookLabels, bookFirstImages, null, baseUrl, imageBaseUrl);
    }

    /**
     * Generates a sub-collection listing all manifests within a collection.
     * If the collection has child collections, they are included as Collection-type items.
     * When bookFirstImages is provided, each manifest item includes a thumbnail referencing the book's first image.
     * When bookCroppedFlags is provided, thumbnails use the cropped image path where applicable.
     *
     * @param collection       the book collection
     * @param bookLabels       a map of bookId to label for each book in this collection
     * @param bookFirstImages  a map of bookId to first image ID, or {@code null} for no thumbnails
     * @param bookCroppedFlags a map of bookId to whether cropped images are available, or {@code null}
     * @param baseUrl          the base URL prefix, or {@code null} for relative IDs
     * @param imageBaseUrl     the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @return the sub-collection as a JSON ObjectNode
     */
    public ObjectNode generateSubCollection(BookCollection collection, Map<String, String> bookLabels,
                                            Map<String, String> bookFirstImages, Map<String, Boolean> bookCroppedFlags,
                                            String baseUrl, String imageBaseUrl) {
        String collectionId = collection.getId();
        String lang = getCollectionLanguage(collection);

        ObjectNode node = mapper.createObjectNode();
        node.put("@context", CONTEXT);
        node.put("id", buildId(baseUrl, collectionId + "/collection"));
        node.put("type", "Collection");

        String label = collection.getLabel() != null ? collection.getLabel() : collectionId;
        node.set("label", languageMap(lang, label));

        ArrayNode items = mapper.createArrayNode();

        // Add child collection items for parent collections
        String[] children = collection.getChildCollections();
        if (children != null && children.length > 0) {
            for (String childId : children) {
                ObjectNode item = mapper.createObjectNode();
                item.put("id", buildId(baseUrl, childId + "/collection"));
                item.put("type", "Collection");
                item.set("label", languageMap(lang, childId));
                items.add(item);
            }
        }

        // Add manifest items for books
        for (Map.Entry<String, String> entry : bookLabels.entrySet()) {
            String bookId = entry.getKey();
            ObjectNode item = mapper.createObjectNode();
            item.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/manifest.json"));
            item.put("type", "Manifest");
            item.set("label", languageMap(lang, entry.getValue()));

            // Add thumbnail if first image data is available
            if (bookFirstImages != null && bookFirstImages.containsKey(bookId)) {
                String firstImageId = bookFirstImages.get(bookId);
                boolean cropped = bookCroppedFlags != null && Boolean.TRUE.equals(bookCroppedFlags.get(bookId));
                String imageServiceId = buildImageServiceId(baseUrl, imageBaseUrl, collectionId, bookId, firstImageId, cropped);
                // Only the image id is known here, so the thumbnail has no size
                item.set("thumbnail", thumbnailArray(imageServiceId, 0, 0));
            }

            items.add(item);
        }
        node.set("items", items);
        return node;
    }

    /**
     * Generates a Manifest for a single book with all metadata, canvases, and ranges.
     *
     * @param collection      the parent collection
     * @param book            the book to generate a manifest for
     * @param baseUrl         the base URL prefix, or {@code null} for relative IDs
     * @param imageBaseUrl    the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @param imageApiVersion the IIIF Image API version (2 or 3)
     * @return the Manifest as a JSON ObjectNode
     */
    public ObjectNode generateManifest(BookCollection collection, Book book, String baseUrl, String imageBaseUrl, int imageApiVersion) {
        return generateManifest(collection, book, baseUrl, imageBaseUrl, imageApiVersion, null);
    }

    /**
     * Generates a Manifest for a single book with all metadata, canvases, and ranges.
     *
     * @param collection         the parent collection
     * @param book               the book to generate a manifest for
     * @param baseUrl            the base URL prefix, or {@code null} for relative IDs
     * @param imageBaseUrl       the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @param imageApiVersion    the IIIF Image API version (2 or 3)
     * @param hasAnnotationsArray per-canvas flag indicating whether annotations exist, or {@code null} to auto-detect
     * @return the Manifest as a JSON ObjectNode
     */
    public ObjectNode generateManifest(BookCollection collection, Book book, String baseUrl, String imageBaseUrl, int imageApiVersion, boolean[] hasAnnotationsArray) {
        String collectionId = collection.getId();
        String bookId = book.getId();
        String lang = getCollectionLanguage(collection);

        ObjectNode node = mapper.createObjectNode();
        node.put("@context", CONTEXT);
        node.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/manifest.json"));
        node.put("type", "Manifest");

        // Label from BiblioData commonName or title
        String manifestLabel = resolveManifestLabel(collection, book);
        node.set("label", languageMap(lang, manifestLabel));

        // Metadata
        ArrayNode metadataArray = generateMetadataArray(collection, book, lang);
        if (metadataArray.size() > 0) {
            node.set("metadata", metadataArray);
        }

        // requiredStatement for attribution/permission (prefer English)
        Permission permission = book.getPermission("en");
        if (permission == null) {
            Permission[] perms = book.getPermissionsInAllLanguages();
            if (perms != null && perms.length > 0) {
                permission = perms[0];
            }
        }
        if (permission != null && permission.getPermission() != null && !permission.getPermission().isEmpty()) {
            ObjectNode requiredStatement = mapper.createObjectNode();
            requiredStatement.set("label", languageMap("en", "Attribution"));
            requiredStatement.set("value", languageMap("en", permission.getPermission()));
            node.set("requiredStatement", requiredStatement);
        }

        // rights - license URL from metadata (IIIF 3.0: must be CC or RightsStatements.org URI)
        String licenseUrl = book.getLicenseUrl();
        if (licenseUrl != null && !licenseUrl.isEmpty()) {
            node.put("rights", licenseUrl);
        }

        // seeAlso: the English TEI book description, copied to description.xml (recipe 0053)
        if (hasDisplayableDescription(book)) {
            ObjectNode seeAlso = mapper.createObjectNode();
            seeAlso.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/" + DESCRIPTION_FILE));
            seeAlso.put("type", "Dataset");
            seeAlso.set("label", languageMap("en", "Book description (TEI)"));
            seeAlso.put("format", TEI_FORMAT);
            seeAlso.put("profile", TEI_PROFILE);
            node.set("seeAlso", mapper.createArrayNode().add(seeAlso));
        }

        // viewingDirection and behavior for multi-image books
        List<BookImage> images = book.getImages().getImages();
        if (images.size() > 1) {
            node.put("viewingDirection", "left-to-right");
            ArrayNode behavior = mapper.createArrayNode();
            behavior.add("paged");
            node.set("behavior", behavior);
        }

        // Determine if cropped images are available
        ImageList croppedImageList = book.getCroppedImages();
        boolean hasCropped = croppedImageList != null && !croppedImageList.getImages().isEmpty();
        List<BookImage> paintingImages = hasCropped ? croppedImageList.getImages() : images;

        // Thumbnail from first image, sized from the image it shows (the cropped one if used)
        if (!images.isEmpty()) {
            BookImage firstImage = images.get(0);
            BookImage shown = hasCropped
                    ? paintingImages.stream().filter(i -> firstImage.getId().equals(i.getId())).findFirst().orElse(null)
                    : firstImage;
            String imageServiceId = buildImageServiceId(baseUrl, imageBaseUrl, collectionId, bookId, firstImage.getId(), hasCropped);
            node.set("thumbnail", thumbnailArray(imageServiceId,
                    shown != null ? shown.getWidth() : 0, shown != null ? shown.getHeight() : 0));
        }

        // Canvas items - use cropped images if available
        ArrayNode canvasItems = mapper.createArrayNode();
        for (int i = 0; i < paintingImages.size(); i++) {
            BookImage image = paintingImages.get(i);
            boolean hasAnnotations = hasAnnotationsArray != null && i < hasAnnotationsArray.length ? hasAnnotationsArray[i] : false;
            ObjectNode canvas = generateCanvas(collection, book, image, i, baseUrl, imageBaseUrl, imageApiVersion, hasAnnotations, hasCropped);
            canvasItems.add(canvas);
        }
        node.set("items", canvasItems);

        // Ranges (structures)
        ArrayNode ranges = generateRanges(collection, book, baseUrl);
        if (ranges.size() > 0) {
            ObjectNode structures = mapper.createObjectNode();
            structures.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/range/top"));
            structures.put("type", "Range");
            structures.set("label", languageMap(lang, "Table of Contents"));
            structures.set("items", ranges);
            ArrayNode structuresArray = mapper.createArrayNode();
            structuresArray.add(structures);
            node.set("structures", structuresArray);
        }

        return node;
    }

    /**
     * Generates a Canvas for a single book image.
     *
     * @param collection      the parent collection
     * @param book            the book containing the image
     * @param image           the book image
     * @param index           the zero-based index of the image in the book
     * @param baseUrl         the base URL prefix, or {@code null} for relative IDs
     * @param imageBaseUrl    the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @param imageApiVersion the IIIF Image API version (2 or 3)
     * @param hasAnnotations  whether this canvas has an associated annotation page
     * @return the Canvas as a JSON ObjectNode
     */
    public ObjectNode generateCanvas(BookCollection collection, Book book, BookImage image,
                                     int index, String baseUrl, String imageBaseUrl, int imageApiVersion,
                                     boolean hasAnnotations) {
        return generateCanvas(collection, book, image, index, baseUrl, imageBaseUrl, imageApiVersion, hasAnnotations, false);
    }

    /**
     * Generates a Canvas for a single book image, optionally using cropped image paths.
     *
     * @param collection      the parent collection
     * @param book            the book containing the image
     * @param image           the book image
     * @param index           the zero-based index of the image in the book
     * @param baseUrl         the base URL prefix, or {@code null} for relative IDs
     * @param imageBaseUrl    the base URL for IIIF Image API services, or {@code null} to use baseUrl
     * @param imageApiVersion the IIIF Image API version (2 or 3)
     * @param hasAnnotations  whether this canvas has an associated annotation page
     * @param cropped         whether to use cropped image paths
     * @return the Canvas as a JSON ObjectNode
     */
    public ObjectNode generateCanvas(BookCollection collection, Book book, BookImage image,
                                     int index, String baseUrl, String imageBaseUrl, int imageApiVersion,
                                     boolean hasAnnotations, boolean cropped) {
        String collectionId = collection.getId();
        String bookId = book.getId();
        String lang = getCollectionLanguage(collection);

        ObjectNode canvas = mapper.createObjectNode();
        String canvasId = buildId(baseUrl, collectionId + "/" + bookId + "/canvas/" + index);
        canvas.put("id", canvasId);
        canvas.put("type", "Canvas");

        // Label: prefer AoR pagination, then signature, then name
        String label = resolveCanvasLabel(book, image);
        canvas.set("label", languageMap(lang, label));

        // Dimensions
        int width = image.getWidth() > 0 ? image.getWidth() : 1000;
        int height = image.getHeight() > 0 ? image.getHeight() : 1000;
        canvas.put("width", width);
        canvas.put("height", height);

        // Painting annotation with Image Service
        // When the image is missing, reference the collection's missing_image placeholder
        String imageServiceId;
        if (image.isMissing()) {
            BookImage missingImage = collection.getMissingImage();
            String missingId = missingImage != null ? missingImage.getId() : "missing_image.tif";
            imageServiceId = buildImageServiceId(baseUrl, imageBaseUrl, collectionId, null, missingId, false);
        } else {
            imageServiceId = buildImageServiceId(baseUrl, imageBaseUrl, collectionId, bookId, image.getId(), cropped);
        }
        ObjectNode paintingAnnoPage = mapper.createObjectNode();
        paintingAnnoPage.put("id", canvasId + "/page");
        paintingAnnoPage.put("type", "AnnotationPage");

        ArrayNode annotations = mapper.createArrayNode();
        ObjectNode paintingAnnotation = mapper.createObjectNode();
        paintingAnnotation.put("id", canvasId + "/page/annotation");
        paintingAnnotation.put("type", "Annotation");
        paintingAnnotation.put("motivation", "painting");

        // Body with image service
        ObjectNode body = mapper.createObjectNode();
        body.put("id", imageServiceId + "/full/full/0/default.jpg");
        body.put("type", "Image");
        body.put("format", "image/jpeg");
        body.put("width", width);
        body.put("height", height);

        // Service array
        ArrayNode serviceArray = mapper.createArrayNode();
        ObjectNode service = mapper.createObjectNode();
        service.put("id", imageServiceId);
        String serviceType = imageApiVersion == 3 ? "ImageService3" : "ImageService2";
        service.put("type", serviceType);
        String profile = imageApiVersion == 3
                ? "level2"
                : "http://iiif.io/api/image/2/level2.json";
        service.put("profile", profile);
        serviceArray.add(service);
        body.set("service", serviceArray);

        paintingAnnotation.set("body", body);
        paintingAnnotation.put("target", canvasId);
        annotations.add(paintingAnnotation);

        paintingAnnoPage.set("items", annotations);
        ArrayNode items = mapper.createArrayNode();
        items.add(paintingAnnoPage);
        canvas.set("items", items);

        // Thumbnail, sized from the image. A missing image shows the placeholder,
        // whose size is not this image's.
        canvas.set("thumbnail", image.isMissing()
                ? thumbnailArray(imageServiceId, 0, 0)
                : thumbnailArray(imageServiceId, image.getWidth(), image.getHeight()));

        // Annotation page reference (commenting annotations) - only if annotations exist
        if (hasAnnotations) {
            ArrayNode annotationsRef = mapper.createArrayNode();
            ObjectNode apRef = mapper.createObjectNode();
            apRef.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/canvas/" + index + "/annotations.json"));
            apRef.put("type", "AnnotationPage");
            annotationsRef.add(apRef);
            canvas.set("annotations", annotationsRef);
        }

        return canvas;
    }

    /**
     * Generates an annotation page for a canvas/page, including AoR annotations,
     * illustration tagging, and transcriptions.
     *
     * @param collection the parent collection
     * @param book       the book containing the page
     * @param image      the book image for this page
     * @param index      the zero-based index of the image in the book
     * @param baseUrl    the base URL prefix, or {@code null} for relative IDs
     * @return the AnnotationPage as a JSON ObjectNode, or {@code null} if there is no annotation content
     */
    public ObjectNode generateAnnotationPage(BookCollection collection, Book book, BookImage image,
                                             int index, String baseUrl) {
        String collectionId = collection.getId();
        String bookId = book.getId();
        String canvasId = buildId(baseUrl, collectionId + "/" + bookId + "/canvas/" + index);
        String annotationPageId = buildId(baseUrl, collectionId + "/" + bookId + "/canvas/" + index + "/annotations");
        String stylesheet = resolveAnnotationStylesheet(collection);
        String collectionLang = AnnotationHtml.normalizeLanguage(getCollectionLanguage(collection));
        String imageId = image.getId();

        List<AnnotationItem> content = new ArrayList<>();

        // 1. AoR annotations, page header first then grouped by place
        AnnotatedPage annotatedPage = findAnnotatedPage(book, imageId);
        if (annotatedPage != null) {
            content.addAll(AorAnnotationFormatter.format(annotatedPage));
        }

        // 2. Illustration tagging
        IllustrationTagging tagging = book.getIllustrationTagging();
        if (tagging != null && imageId != null) {
            for (int illusIdx : tagging.findImageIndices(book, imageId)) {
                Illustration illus = tagging.getIllustrationData(illusIdx);
                String html = illustrationHtml(collection, book, illus);
                content.add(new AnnotationItem(List.of("rosa-anno", "dlmm-illustration"), html, collectionLang));
            }
        }

        // 3. Transcription, one annotation per column
        for (TeiTranscriptionHtml.Column column : TeiTranscriptionHtml.findPage(transcriptionPages(collection, book), imageId)) {
            List<String> classes = new ArrayList<>(List.of("rosa-anno", "dlmm-transcription"));
            if (column.name() != null) {
                classes.add("dlmm-column-" + column.name().replaceAll("[^A-Za-z0-9_-]", "_"));
            }
            content.add(new AnnotationItem(classes, column.html(), null));
        }

        // 4. Collection-level HTML commentary
        HTMLAnnotations htmlAnnotations = collection.getHTMLAnnotations();
        if (htmlAnnotations != null && imageId != null) {
            String htmlContent = htmlAnnotations.getAnnotation(imageId);
            if (htmlContent != null && !htmlContent.isBlank()) {
                content.add(new AnnotationItem(List.of("rosa-anno", "dlmm-commentary"), htmlContent.trim(), collectionLang));
            }
        }

        if (content.isEmpty()) {
            return null;
        }

        ArrayNode items = mapper.createArrayNode();
        int annotationCounter = 0;
        for (AnnotationItem item : content) {
            items.add(createTextAnnotation(annotationPageId + "/annotation/" + annotationCounter++,
                    item, stylesheet, canvasId));
        }

        ObjectNode page = mapper.createObjectNode();
        page.put("id", annotationPageId);
        page.put("type", "AnnotationPage");
        page.set("items", items);
        return page;
    }

    /**
     * Builds the HTML for an illustration tagging entry.
     */
    private String illustrationHtml(BookCollection collection, Book book, Illustration illus) {
        StringBuilder html = new StringBuilder("<p><b>Illustration</b></p>");
        if (illus.getTitles() != null && illus.getTitles().length > 0) {
            html.append(illustrationField("Titles", String.join("; ", IllustrationRefs.resolveTitles(collection, book, illus))));
        }
        if (illus.getCharacters() != null && illus.getCharacters().length > 0) {
            html.append(illustrationField("Characters",
                    String.join(", ", IllustrationRefs.resolveCharacterDisplayNames(collection, book, illus))));
        }
        html.append(illustrationField("Textual elements", illus.getTextualElement()));
        html.append(illustrationField("Costume", illus.getCostume()));
        html.append(illustrationField("Initials", illus.getInitials()));
        html.append(illustrationField("Objects", illus.getObject()));
        html.append(illustrationField("Landscape", illus.getLandscape()));
        html.append(illustrationField("Architecture", illus.getArchitecture()));
        html.append(illustrationField("Other", illus.getOther()));
        return html.toString();
    }

    private static String illustrationField(String label, String value) {
        if (AnnotationHtml.blank(value)) {
            return "";
        }
        return "<p><b>" + label + ":</b> " + AnnotationHtml.esc(AnnotationHtml.clean(value)) + "</p>";
    }

    /**
     * Whether the book has an English TEI description with at least one non-empty
     * {@code notesStmt} note, which is published as {@link #DESCRIPTION_FILE}.
     *
     * @param book the book
     * @return true if the description is published
     */
    public static boolean hasDisplayableDescription(Book book) {
        rosa.archive.model.BookDescription desc = book.getDescription(DESCRIPTION_LANGUAGE);
        return desc != null && desc.getId() != null
                && desc.getNotes().values().stream().anyMatch(t -> t != null && !t.isBlank());
    }

    /**
     * Returns the converted transcription pages for a book, caching the most recent book
     * since annotation pages are generated book by book.
     */
    private Map<String, List<TeiTranscriptionHtml.Column>> transcriptionPages(BookCollection collection, Book book) {
        Transcription transcription = book.getTranscription();
        if (transcription == null || transcription.getXML() == null || transcription.getXML().isBlank()) {
            return Map.of();
        }
        String key = collection.getId() + "/" + book.getId();
        if (!key.equals(cachedTranscriptionKey)) {
            Map<String, List<TeiTranscriptionHtml.Column>> pages;
            try {
                pages = TeiTranscriptionHtml.convert(transcription.getXML());
            } catch (IllegalArgumentException e) {
                System.err.println("WARNING: Cannot convert transcription for " + key + ": " + e.getMessage());
                pages = Map.of();
            }
            cachedTranscriptionKey = key;
            cachedTranscriptionPages = pages;
        }
        return cachedTranscriptionPages;
    }

    /**
     * Sets the URL template of the stylesheet referenced by commenting annotations
     * (IIIF cookbook recipe 0045). {@code {collection}} is replaced by the id of the
     * collection's parent, or the collection itself if it has no parent. When null,
     * annotations have no {@code stylesheet}.
     *
     * @param template the URL template, e.g. {@code https://example.org/styles/{collection}.css}
     */
    public void setAnnotationStylesheetUrl(String template) {
        this.annotationStylesheetUrl = (template == null || template.isBlank()) ? null : template.trim();
    }

    /**
     * Resolves the annotation stylesheet URL for a collection, or null if not configured.
     *
     * @param collection the collection
     * @return the stylesheet URL, or null
     */
    public String resolveAnnotationStylesheet(BookCollection collection) {
        if (annotationStylesheetUrl == null) {
            return null;
        }
        String id = collection.getId();
        String[] parents = collection.getParentCollections();
        if (parents != null) {
            for (String parent : parents) {
                if (parent != null && !parent.isBlank()) {
                    id = parent.trim();
                    break;
                }
            }
        }
        return annotationStylesheetUrl.replace("{collection}", id);
    }

    /**
     * Creates a commenting Annotation whose body is a SpecificResource with a styleClass
     * wrapping an HTML TextualBody (IIIF cookbook recipes 0019 and 0045).
     */
    private ObjectNode createTextAnnotation(String id, AnnotationItem item, String stylesheet, String target) {
        ObjectNode annotation = mapper.createObjectNode();
        annotation.put("id", id);
        annotation.put("type", "Annotation");
        annotation.put("motivation", "commenting");
        if (stylesheet != null) {
            annotation.put("stylesheet", stylesheet);
        }

        ObjectNode source = mapper.createObjectNode();
        source.put("type", "TextualBody");
        source.put("value", item.html());
        source.put("format", "text/html");
        if (item.language() != null) {
            source.put("language", item.language());
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("type", "SpecificResource");
        body.put("styleClass", String.join(" ", item.styleClasses()));
        body.set("source", source);
        annotation.set("body", body);

        annotation.put("target", target);
        return annotation;
    }

    /**
     * Finds the AnnotatedPage matching a given image ID.
     */
    private AnnotatedPage findAnnotatedPage(Book book, String imageId) {
        if (imageId == null) return null;
        String baseName = imageId.replace(".tif", "");
        for (AnnotatedPage ap : book.getAnnotatedPages()) {
            if (ap.getPage() != null && ap.getPage().contains(baseName)) {
                return ap;
            }
        }
        return null;
    }

    /**
     * Generates structural Ranges for a book: image location ranges, text ranges, and illustration ranges.
     *
     * @param collection the parent collection
     * @param book       the book
     * @param baseUrl    the base URL prefix, or {@code null} for relative IDs
     * @return the ranges as a JSON ArrayNode
     */
    public ArrayNode generateRanges(BookCollection collection, Book book, String baseUrl) {
        String collectionId = collection.getId();
        String bookId = book.getId();
        String lang = getCollectionLanguage(collection);
        ArrayNode ranges = mapper.createArrayNode();

        List<BookImage> images = book.getImages() != null ? book.getImages().getImages() : List.of();

        // Image location ranges: group canvases by BookImageLocation
        Map<BookImageLocation, List<Integer>> locationGroups = new LinkedHashMap<>();
        for (int i = 0; i < images.size(); i++) {
            BookImage image = images.get(i);
            BookImageLocation loc = image.getLocation();
            if (loc != null) {
                locationGroups.computeIfAbsent(loc, _ -> new ArrayList<>()).add(i);
            }
        }
        for (Map.Entry<BookImageLocation, List<Integer>> entry : locationGroups.entrySet()) {
            BookImageLocation loc = entry.getKey();
            String locLabel = loc.getDisplay();
            if (locLabel == null || locLabel.isEmpty()) {
                locLabel = loc.name().toLowerCase().replace('_', ' ');
            }

            ObjectNode range = mapper.createObjectNode();
            range.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/range/location/" + loc.name().toLowerCase()));
            range.put("type", "Range");
            range.set("label", languageMap(lang, locLabel));

            ArrayNode rangeItems = mapper.createArrayNode();
            for (int idx : entry.getValue()) {
                ObjectNode ref = mapper.createObjectNode();
                ref.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/canvas/" + idx));
                ref.put("type", "Canvas");
                rangeItems.add(ref);
            }
            range.set("items", rangeItems);
            ranges.add(range);
        }

        // Text ranges from BookMetadata BookTexts
        BookMetadata metadata = book.getBookMetadata();
        if (metadata != null && metadata.getBookTexts() != null) {
            int textIndex = 0;
            for (BookText bookText : metadata.getBookTexts()) {
                if (bookText.getTitle() == null && bookText.getFirstPage() == null) {
                    textIndex++;
                    continue;
                }

                ObjectNode range = mapper.createObjectNode();
                range.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/range/text/" + textIndex));
                range.put("type", "Range");
                String textLabel = bookText.getTitle() != null ? bookText.getTitle() : "Text " + (textIndex + 1);
                range.set("label", languageMap(lang, textLabel));

                // Find canvases between firstPage and lastPage
                ArrayNode rangeItems = mapper.createArrayNode();
                boolean inRange = false;
                for (int i = 0; i < images.size(); i++) {
                    BookImage image = images.get(i);
                    String imageName = image.getName() != null ? image.getName() : image.getId();
                    if (bookText.getFirstPage() != null && imageName != null
                            && imageName.contains(bookText.getFirstPage())) {
                        inRange = true;
                    }
                    if (inRange) {
                        ObjectNode ref = mapper.createObjectNode();
                        ref.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/canvas/" + i));
                        ref.put("type", "Canvas");
                        rangeItems.add(ref);
                    }
                    if (bookText.getLastPage() != null && imageName != null
                            && imageName.contains(bookText.getLastPage())) {
                        break;
                    }
                }
                if (rangeItems.size() > 0) {
                    range.set("items", rangeItems);
                    ranges.add(range);
                }
                textIndex++;
            }
        }

        // Illustration ranges: one Range per illustration
        IllustrationTagging tagging = book.getIllustrationTagging();
        if (tagging != null) {
            for (int i = 0; i < tagging.size(); i++) {
                Illustration illus = tagging.getIllustrationData(i);
                String illusLabel = illus.getId() != null ? illus.getId() : "Illustration " + (i + 1);
                List<String> illusTitles = IllustrationRefs.resolveTitles(collection, book, illus);
                if (!illusTitles.isEmpty()) {
                    illusLabel = String.join(", ", illusTitles);
                }

                // Find the canvas index for this illustration's page
                String imageName = book.guessImageName(illus.getPage());
                int canvasIndex = -1;
                if (imageName != null) {
                    for (int j = 0; j < images.size(); j++) {
                        if (images.get(j).getId().equals(imageName)) {
                            canvasIndex = j;
                            break;
                        }
                    }
                }

                if (canvasIndex >= 0) {
                    ObjectNode range = mapper.createObjectNode();
                    range.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/range/illustration/" + i));
                    range.put("type", "Range");
                    range.set("label", languageMap(lang, illusLabel));

                    ArrayNode rangeItems = mapper.createArrayNode();
                    ObjectNode ref = mapper.createObjectNode();
                    ref.put("id", buildId(baseUrl, collectionId + "/" + bookId + "/canvas/" + canvasIndex));
                    ref.put("type", "Canvas");
                    rangeItems.add(ref);
                    range.set("items", rangeItems);
                    ranges.add(range);
                }
            }
        }

        return ranges;
    }

    // ---- Private helper methods ----

    /**
     * Adds a JHSearchService1 service reference to a IIIF resource node.
     *
     * @param node         the ObjectNode to add the service to
     * @param baseUrl      the base URL prefix, or {@code null} for relative IDs
     * @param collectionId the collection identifier
     */
    private void addJHSearchService(ObjectNode node, String baseUrl, String collectionId) {
        ArrayNode serviceArray = mapper.createArrayNode();
        ObjectNode service = mapper.createObjectNode();
        service.put("id", buildId(baseUrl, collectionId + "/service/jhsearch.json"));
        service.put("type", "JHSearchService2");
        service.put("profile", "https://github.com/jhu-digital-manuscripts/rosa2/doc/search.md");
        serviceArray.add(service);
        node.set("service", serviceArray);
    }

    /**
     * Resolves the manifest label from BiblioData commonName or title.
     */
    private String resolveManifestLabel(BookCollection collection, Book book) {
        String lang = getCollectionLanguage(collection);
        BiblioData biblio = book.getBiblioData(lang);
        if (biblio == null) {
            // Try first available language
            BookMetadata metadata = book.getBookMetadata();
            if (metadata != null && !metadata.getBiblioDataMap().isEmpty()) {
                biblio = metadata.getBiblioDataMap().values().iterator().next();
            }
        }
        if (biblio != null) {
            if (biblio.getCommonName() != null && !biblio.getCommonName().isEmpty()) {
                return biblio.getCommonName();
            }
            if (biblio.getTitle() != null && !biblio.getTitle().isEmpty()) {
                return biblio.getTitle();
            }
        }
        return book.getId();
    }

    /**
     * Resolves the canvas label: prefers AoR pagination, then signature, then image name.
     */
    private String resolveCanvasLabel(Book book, BookImage image) {
        // Look for AnnotatedPage matching this image
        String imageId = image.getId();
        if (imageId != null) {
            for (AnnotatedPage ap : book.getAnnotatedPages()) {
                if (ap.getPage() != null && ap.getPage().contains(imageId.replace(".tif", ""))) {
                    if (ap.getPagination() != null && !ap.getPagination().isEmpty()) {
                        return ap.getPagination();
                    }
                    if (ap.getSignature() != null && !ap.getSignature().isEmpty()) {
                        return ap.getSignature();
                    }
                }
            }
        }
        // Fallback to image name
        if (image.getName() != null && !image.getName().isEmpty()) {
            return image.getName();
        }
        return imageId != null ? imageId : "unknown";
    }

    /**
     * Generates the metadata array for a Manifest from BookMetadata and BiblioData.
     */
    private ArrayNode generateMetadataArray(BookCollection collection, Book book, String lang) {
        ArrayNode metadata = mapper.createArrayNode();
        BiblioData biblio = book.getBiblioData(lang);
        if (biblio == null) {
            BookMetadata bm = book.getBookMetadata();
            if (bm != null && !bm.getBiblioDataMap().isEmpty()) {
                biblio = bm.getBiblioDataMap().values().iterator().next();
            }
        }
        BookMetadata bookMeta = book.getBookMetadata();

        if (biblio != null) {
            addMetadataEntry(metadata, "Title", biblio.getTitle(), lang);
            addMetadataEntry(metadata, "Repository", biblio.getRepository(), lang);
            addMetadataEntry(metadata, "Shelfmark", biblio.getShelfmark(), lang);
            addMetadataEntry(metadata, "Current Location", biblio.getCurrentLocation(), lang);
            addMetadataEntry(metadata, "Origin", biblio.getOrigin(), lang);
            addMetadataEntry(metadata, "Date", biblio.getDateLabel(), lang);
            addMetadataEntry(metadata, "Type", biblio.getType(), lang);
            addMetadataEntry(metadata, "Material", biblio.getMaterial(), lang);

            // Readers
            if (biblio.getReaders() != null && biblio.getReaders().length > 0) {
                List<String> readerNames = new ArrayList<>();
                for (ObjectRef reader : biblio.getReaders()) {
                    if (reader.getName() != null && !reader.getName().isEmpty()) {
                        readerNames.add(reader.getName());
                    }
                }
                if (!readerNames.isEmpty()) {
                    addMetadataEntry(metadata, "Readers", String.join("; ", readerNames), lang);
                }
            }

            // Authors
            if (biblio.getAuthors() != null && biblio.getAuthors().length > 0) {
                List<String> authorNames = new ArrayList<>();
                for (ObjectRef author : biblio.getAuthors()) {
                    if (author.getName() != null && !author.getName().isEmpty()) {
                        authorNames.add(author.getName());
                    }
                }
                if (!authorNames.isEmpty()) {
                    addMetadataEntry(metadata, "Authors", String.join("; ", authorNames), lang);
                }
            }

            // Websites
            if (biblio.getWebsites() != null && biblio.getWebsites().length > 0) {
                List<String> sites = new ArrayList<>();
                for (String site : biblio.getWebsites()) {
                    if (site != null && !site.isEmpty()) {
                        sites.add(site);
                    }
                }
                if (!sites.isEmpty()) {
                    addMetadataEntry(metadata, "Websites", String.join("; ", sites), lang);
                }
            }
        }

        if (bookMeta != null) {
            // Dimensions
            if (bookMeta.getWidth() > 0 && bookMeta.getHeight() > 0) {
                String dims = bookMeta.getDimensionsString();
                addMetadataEntry(metadata, "Dimensions", dims, lang);
            }

            // Year range
            if (bookMeta.getYearStart() > 0) {
                addMetadataEntry(metadata, "Year Start", String.valueOf(bookMeta.getYearStart()), lang);
            }
            if (bookMeta.getYearEnd() > 0) {
                addMetadataEntry(metadata, "Year End", String.valueOf(bookMeta.getYearEnd()), lang);
            }

            // Pages and illustrations
            if (bookMeta.getNumberOfPages() > 0) {
                addMetadataEntry(metadata, "Number of Pages", String.valueOf(bookMeta.getNumberOfPages()), lang);
            }
            if (bookMeta.getNumberOfIllustrations() > 0) {
                addMetadataEntry(metadata, "Number of Illustrations", String.valueOf(bookMeta.getNumberOfIllustrations()), lang);
            }
        }

        // Note: License is now in rights property, Attribution is in requiredStatement

        return metadata;
    }

    /**
     * Adds a metadata entry (label/value pair) to the metadata array.
     */
    private void addMetadataEntry(ArrayNode metadata, String label, String value, String lang) {
        if (value == null || value.isEmpty()) {
            return;
        }
        ObjectNode entry = mapper.createObjectNode();
        entry.set("label", languageMap(lang, label));
        entry.set("value", languageMap(lang, value));
        metadata.add(entry);
    }

    /**
     * Creates a thumbnail property: one THUMBNAIL_WIDTH pixel wide JPEG from the image service.
     * When the image size is known, the thumbnail's width and height are included, so viewers
     * can lay out thumbnails before they load. The height is the one the Image API returns for
     * a "w," size request: the image height scaled by the same factor and rounded.
     *
     * @param imageServiceId the image service ID
     * @param imageWidth     the full image width, or 0 if unknown
     * @param imageHeight    the full image height, or 0 if unknown
     */
    private ArrayNode thumbnailArray(String imageServiceId, int imageWidth, int imageHeight) {
        ObjectNode thumbnail = mapper.createObjectNode();
        thumbnail.put("id", imageServiceId + "/full/" + THUMBNAIL_WIDTH + ",/0/default.jpg");
        thumbnail.put("type", "Image");
        thumbnail.put("format", "image/jpeg");
        if (imageWidth > 0 && imageHeight > 0) {
            thumbnail.put("width", THUMBNAIL_WIDTH);
            thumbnail.put("height", Math.max(1, Math.round((float) imageHeight * THUMBNAIL_WIDTH / imageWidth)));
        }
        return mapper.createArrayNode().add(thumbnail);
    }

    /**
     * Creates a IIIF 3.0 language map object.
     */
    private ObjectNode languageMap(String lang, String value) {
        ObjectNode map = mapper.createObjectNode();
        ArrayNode values = mapper.createArrayNode();
        values.add(value);
        map.set(lang, values);
        return map;
    }

    /**
     * Builds an ID string from the base URL and path.
     */
    private String buildId(String baseUrl, String path) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return base + "/" + path;
        }
        return path;
    }

    /**
     * Builds the image service ID for a given image, optionally using the cropped path.
     * The image identifier is encoded as a single IIIF Image API path segment:
     * slashes are percent-encoded as %2F, and the file extension is stripped.
     * When cropped is true, a "cropped/" segment is prepended before the image ID.
     */
    private String buildImageServiceId(String baseUrl, String imageBaseUrl, String collectionId, String bookId, String imageId, boolean cropped) {
        String effectiveBase = imageBaseUrl != null ? imageBaseUrl : baseUrl;

        // Strip file extension from the image id
        String strippedId = imageId;
        int dotIndex = strippedId.lastIndexOf('.');
        if (dotIndex > 0) {
            strippedId = strippedId.substring(0, dotIndex);
        }

        // Build the composite image identifier: collection/book/[cropped/]image (or collection/[cropped/]image if book is null)
        String compositeId = collectionId
                + (bookId != null ? "/" + bookId : "")
                + "/" + (cropped ? "cropped/" : "") + strippedId;

        // Encode as a single IIIF Image API identifier path segment (slashes become %2F)
        String encodedId = encodeImageId(compositeId);

        if (effectiveBase != null && !effectiveBase.isEmpty()) {
            String base = effectiveBase.endsWith("/") ? effectiveBase.substring(0, effectiveBase.length() - 1) : effectiveBase;
            return base + "/" + encodedId;
        }
        return encodedId;
    }

    /**
     * Encodes a IIIF Image API image identifier as a single path segment.
     * Slashes within the identifier are percent-encoded as %2F per the IIIF Image API specification.
     */
    private String encodeImageId(String imageId) {
        try {
            // Use URI to encode the identifier properly, then replace slashes with %2F
            return new URI("http", "x", "/" + imageId, null).getRawPath().substring(1).replace("/", "%2F");
        } catch (URISyntaxException e) {
            // Fallback: manual percent-encoding of slashes
            return imageId.replace("/", "%2F");
        }
    }

    /**
     * Resolves the primary language code for the collection, defaulting to "none".
     */
    private String getCollectionLanguage(BookCollection collection) {
        String[] languages = collection.getAllSupportedLanguages();
        if (languages != null && languages.length > 0 && languages[0] != null && !languages[0].isEmpty()) {
            return languages[0];
        }
        return "none";
    }
}
