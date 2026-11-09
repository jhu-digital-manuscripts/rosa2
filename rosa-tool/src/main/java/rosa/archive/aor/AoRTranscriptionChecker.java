package rosa.archive.aor;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.CheckResult;
import rosa.archive.core.serialize.ArchiveReaders;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.ImageList;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.IdLocations;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.XRef;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Checks the AoR transcription XML files of a collection.
 *
 * <p>Every file must be well-formed XML and, when a schema is given, valid against it
 * ({@link AorXmlValidator}). Well-formed files are then checked against the reference
 * spreadsheets: people, book, location and cross-reference names in annotations must match
 * entries in the respective reference CSV files, annotation IDs must be unique, and internal
 * reference targets must resolve to pages through {@code id_locations.csv}
 * ({@link AorReferenceResolver}). File names must follow the {@code <book>.aor.<page>.xml}
 * convention, agree with the page's {@code filename} attribute and name an image of the
 * book's image list. The sheets themselves must not list a name twice.</p>
 */
public final class AoRTranscriptionChecker {

    private static final String PEOPLE_CSV = "people.csv";
    private static final String BOOKS_CSV = "books.csv";
    private static final String LOCATIONS_CSV = "locations.csv";
    private static final String ID_LOCATIONS_CSV = "id_locations.csv";

    private static final ArchiveNameParser PARSER = new ArchiveNameParser();

    private final Path archivePath;
    private final Path spreadsheetDir;
    private final Path schema;

    /**
     * Creates a checker that only checks well-formedness of the XML.
     *
     * @param archivePath    the root directory of the archive
     * @param spreadsheetDir the directory containing reference CSV files, or null
     *                       to look in the collection directory
     */
    public AoRTranscriptionChecker(Path archivePath, Path spreadsheetDir) {
        this(archivePath, spreadsheetDir, null);
    }

    /**
     * Creates a new AoR transcription checker.
     *
     * @param archivePath    the root directory of the archive
     * @param spreadsheetDir the directory containing reference CSV files, or null
     *                       to look in the collection directory
     * @param schema         a local XML Schema file to validate the transcriptions against,
     *                       or null to check well-formedness only
     */
    public AoRTranscriptionChecker(Path archivePath, Path spreadsheetDir, Path schema) {
        this.archivePath = archivePath;
        this.spreadsheetDir = spreadsheetDir;
        this.schema = schema;
    }

    /**
     * Checks the AoR transcription files of a collection.
     *
     * @param collectionId the collection to check
     * @param bookId       optional book ID (null means all books in collection)
     * @return the errors and warnings found; XML warnings do not fail the check
     */
    public CheckResult run(String collectionId, String bookId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Path collectionDir = archivePath.resolve(collectionId);
        if (!Files.isDirectory(collectionDir)) {
            errors.add("Collection not found: " + collectionId);
            return CheckResult.of(errors, warnings);
        }

        AorXmlValidator xmlValidator = new AorXmlValidator(schema, errors);

        // Determine where to load reference CSVs from
        Path refDir = spreadsheetDir != null ? spreadsheetDir : collectionDir;

        // Load reference data
        for (String sheet : List.of(PEOPLE_CSV, BOOKS_CSV, LOCATIONS_CSV)) {
            checkDuplicateKeys(refDir.resolve(sheet), sheet, errors);
        }
        ReferenceSheet peopleRef = loadReferenceSheet(refDir.resolve(PEOPLE_CSV), PEOPLE_CSV, errors,
                ArchiveReaders::readPeopleReferenceSheet);
        BookReferenceSheet booksRef = loadBookReferenceSheet(refDir.resolve(BOOKS_CSV), BOOKS_CSV, errors);
        ReferenceSheet locationsRef = loadReferenceSheet(refDir.resolve(LOCATIONS_CSV), LOCATIONS_CSV, errors,
                ArchiveReaders::readReferenceSheet);
        IdLocations idLocations = loadIdLocations(refDir.resolve(ID_LOCATIONS_CSV), errors);

        List<String> allBooks;
        try {
            allBooks = listBooks(collectionDir);
        } catch (IOException e) {
            errors.add("Failed to list books in collection " + collectionId + ": " + e.getMessage());
            return CheckResult.of(errors, warnings);
        }

        Map<String, ImageList> imageListCache = new HashMap<>();
        Function<String, ImageList> imageLists = book -> imageListCache.computeIfAbsent(book,
                b -> readImageList(collectionDir.resolve(b).resolve(b + ".images.csv"), errors));

        // Internal references may target any book of the collection, so the resolver sees them all.
        // Without id_locations.csv the targets are only checked against the annotation ids found.
        AorReferenceResolver resolver = idLocations == null ? null
                : new AorReferenceResolver(idLocations, allBooks, imageLists::apply);

        // Collect all annotation IDs across the corpus for internal reference validation
        Map<String, List<String>> allAnnotationIds = new HashMap<>(); // id -> list of locations
        List<BookPageData> allPages = new ArrayList<>();

        // Determine books to check
        List<String> booksToCheck = bookId != null ? List.of(bookId) : allBooks;

        // First pass: validate the XML, load all pages and collect annotation IDs
        for (String book : booksToCheck) {
            Path bookDir = collectionDir.resolve(book);
            if (!Files.isDirectory(bookDir)) {
                errors.add("Book not found: " + book);
                continue;
            }

            List<String> xmlFiles;
            try {
                xmlFiles = listAorTranscriptions(bookDir);
            } catch (IOException e) {
                errors.add("Failed to list files in book " + book + ": " + e.getMessage());
                continue;
            }

            Set<String> images = imageNames(imageLists.apply(book));
            for (String xmlFile : xmlFiles) {
                Path xmlPath = bookDir.resolve(xmlFile);
                checkTranscriptionHasImage(book, xmlFile, images, errors);

                // A file that is not well-formed cannot be parsed; its XML errors are enough
                if (!xmlValidator.validate(xmlPath, "[" + book + "/" + xmlFile + "]", errors, warnings)) {
                    continue;
                }

                List<String> parseErrors = new ArrayList<>();
                AnnotatedPage page;
                try {
                    page = ArchiveReaders.readAnnotatedPage(xmlPath, parseErrors);
                } catch (IOException e) {
                    errors.add("Failed to read transcription [" + xmlFile + "] in book [" + book + "]: " + e.getMessage());
                    continue;
                }

                if (page == null) {
                    errors.addAll(parseErrors);
                    continue;
                }

                allPages.add(new BookPageData(book, xmlFile, page));

                // Collect annotation IDs
                for (Annotation annotation : page.getAnnotations()) {
                    String id = annotation.getId();
                    if (id != null && !id.isBlank()) {
                        String location = book + "/" + xmlFile + "#" + id;
                        allAnnotationIds.computeIfAbsent(id, _ -> new ArrayList<>()).add(location);
                    }
                }
            }
        }

        // Second pass: validate references and detect duplicates
        // Check for duplicate annotation IDs
        for (Map.Entry<String, List<String>> entry : allAnnotationIds.entrySet()) {
            if (entry.getValue().size() > 1) {
                errors.add("Duplicate annotation ID '" + entry.getKey() + "' found at: " + entry.getValue());
            }
        }

        // Validate each page
        Set<String> knownIds = allAnnotationIds.keySet();
        for (BookPageData pageData : allPages) {
            validatePage(pageData, peopleRef, booksRef, locationsRef, knownIds, resolver, allAnnotationIds, errors);
        }

        return CheckResult.of(errors, warnings);
    }

    /**
     * Validates a single annotated page against reference data.
     */
    private void validatePage(BookPageData pageData, ReferenceSheet peopleRef,
                              BookReferenceSheet booksRef, ReferenceSheet locationsRef,
                              Set<String> knownIds, AorReferenceResolver resolver,
                              Map<String, List<String>> annotationLocations, List<String> errors) {
        AnnotatedPage page = pageData.page();
        String context = pageData.book() + "/" + pageData.filename();

        // Validate filename consistency with image name
        validateFilenameConsistency(pageData.book(), pageData.filename(), page, errors);

        // Validate marginalia references
        for (Marginalia m : page.getMarginalia()) {
            for (MarginaliaLanguage lang : m.getLanguages()) {
                for (Position pos : lang.getPositions()) {
                    // Check people references
                    for (String person : pos.getPeople()) {
                        if (peopleRef != null && !peopleRef.containsKey(person)) {
                            errors.add("[" + context + "] Unknown person reference: " + person + hint(peopleRef, person));
                        }
                    }
                    // Check book references
                    for (String book : pos.getBooks()) {
                        if (booksRef != null && !booksRef.containsKey(book)) {
                            errors.add("[" + context + "] Unknown book reference: " + book + hint(booksRef, book));
                        }
                    }
                    // Check location references
                    for (String location : pos.getLocations()) {
                        if (locationsRef != null && !locationsRef.containsKey(location)) {
                            errors.add("[" + context + "] Unknown location reference: " + location
                                    + hint(locationsRef, location));
                        }
                    }
                    // Check cross-references against the same sheets
                    for (XRef xref : pos.getXRefs()) {
                        if (peopleRef != null && notBlank(xref.person()) && !peopleRef.containsKey(xref.person())) {
                            errors.add("[" + context + "] Unknown cross-reference person: " + xref.person()
                                    + hint(peopleRef, xref.person()));
                        }
                        if (booksRef != null && notBlank(xref.title()) && !booksRef.containsKey(xref.title())) {
                            errors.add("[" + context + "] Unknown cross-reference title: " + xref.title()
                                    + hint(booksRef, xref.title()));
                        }
                    }

                    // Check internal references
                    for (InternalReference ref : pos.getInternalRefs()) {
                        validateInternalReference(ref, knownIds, resolver, annotationLocations, context, errors);
                    }
                    for (InternalReference ref : pos.getMarginaliaRefs()) {
                        validateInternalReference(ref, knownIds, resolver, annotationLocations, context, errors);
                    }
                }
            }
        }

        // Internal references on the other annotation types that carry them
        for (Drawing d : page.getDrawings()) {
            for (InternalReference ref : d.getInternalRefs()) {
                validateInternalReference(ref, knownIds, resolver, annotationLocations, context, errors);
            }
        }
        for (Graph g : page.getGraphs()) {
            for (InternalReference ref : g.getInternalRefs()) {
                validateInternalReference(ref, knownIds, resolver, annotationLocations, context, errors);
            }
        }
        for (Table t : page.getTables()) {
            for (InternalReference ref : t.getInternalRefs()) {
                validateInternalReference(ref, knownIds, resolver, annotationLocations, context, errors);
            }
        }

        // Validate internal references on all annotation types
        for (Annotation annotation : page.getAnnotations()) {
            String internalRef = annotation.getInternalRef();
            if (internalRef != null && !internalRef.isBlank()) {
                if (!knownIds.contains(internalRef)) {
                    errors.add("[" + context + "] Internal reference targets unknown ID: " + internalRef);
                }
            }
        }
    }

    /**
     * Validates the targets of an internal reference. With {@code id_locations.csv} every
     * target must resolve to a page, and a target that names an annotation id found in a
     * transcription must be on the page the sheet says; without the sheet, targets are only
     * checked against the annotation ids found.
     */
    private void validateInternalReference(InternalReference ref, Set<String> knownIds,
                                           AorReferenceResolver resolver,
                                           Map<String, List<String>> annotationLocations,
                                           String context, List<String> errors) {
        for (ReferenceTarget target : ref.getTargets()) {
            String targetId = target.targetId() == null ? "" : target.targetId().trim();
            if (resolver == null) {
                if (!targetId.isEmpty() && !knownIds.contains(targetId)) {
                    errors.add("[" + context + "] Internal reference target not found: " + targetId);
                }
                continue;
            }
            AorReferenceResolver.Resolution resolution = resolver.resolution(targetId);
            if (!resolution.resolved()) {
                errors.add("[" + context + "] Internal reference target not resolved: '" + targetId
                        + "' (" + resolution.reason() + ")");
                continue;
            }
            List<String> found = annotationLocations.get(targetId);
            if (found != null) {
                AorReferenceResolver.Target page = resolution.target();
                String expected = page.bookId() + "/" + page.bookId() + ".aor." + page.canvasName() + ".xml#";
                if (found.stream().noneMatch(location -> location.startsWith(expected))) {
                    errors.add("[" + context + "] Internal reference target '" + targetId + "' is at "
                            + found + " but id_locations.csv puts it on " + page.bookId() + " " + page.canvasName());
                }
            }
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Names the standard entry when an unknown name is one of a sheet entry's alternates,
     * so the editor knows which name to use instead.
     */
    private static String hint(ReferenceSheet sheet, String value) {
        for (String key : sheet.getKeys()) {
            List<String> alternates = sheet.getAlternates(key);
            if (alternates != null && alternates.contains(value)) {
                return " (alternate of '" + key + "'; use the standard name)";
            }
        }
        return "";
    }

    /**
     * Reports keys that a reference sheet lists more than once. The reader merges such rows,
     * so a duplicate only shows here.
     */
    private static void checkDuplicateKeys(Path path, String name, List<String> errors) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        Set<String> seen = new HashSet<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String[] row = CSV.parse(line);
                if (row.length == 0 || row[0].isEmpty()) {
                    continue;
                }
                if (!seen.add(row[0])) {
                    errors.add("Duplicate key '" + row[0] + "' in " + name);
                }
            }
        } catch (IOException e) {
            errors.add("Failed to read reference file " + name + ": " + e.getMessage());
        }
    }

    /** The image names of a book's image list without their extensions, or null without a list. */
    private static Set<String> imageNames(ImageList imageList) {
        if (imageList == null) {
            return null;
        }
        Set<String> names = new HashSet<>();
        for (var image : imageList.getImages()) {
            if (image.getId() != null) {
                names.add(stripExtension(image.getId()));
            }
        }
        return names;
    }

    /**
     * Reports a transcription whose name does not correspond to an image of the book's image
     * list: the IIIF and Opensearch generators attach annotations to canvases by that name,
     * so such a transcription is silently dropped.
     */
    private void checkTranscriptionHasImage(String bookId, String filename, Set<String> images, List<String> errors) {
        String expectedImage = deriveImageNameFromTranscription(filename);
        if (images == null || expectedImage == null) {
            return;
        }
        if (!images.contains(stripExtension(expectedImage))) {
            errors.add("[" + bookId + "/" + filename + "] No image '" + expectedImage + "' in " + bookId
                    + ".images.csv for this transcription");
        }
    }

    /**
     * Loads id_locations.csv; a missing file is not an error (not every collection has one).
     */
    private IdLocations loadIdLocations(Path path, List<String> errors) {
        try {
            return ArchiveReaders.readIdLocations(path, errors);
        } catch (IOException e) {
            errors.add("Failed to load reference file " + ID_LOCATIONS_CSV + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads a book's image list, or null when the book has none.
     */
    private static ImageList readImageList(Path path, List<String> errors) {
        try {
            return ArchiveReaders.readImageList(path, errors);
        } catch (IOException e) {
            errors.add("Failed to read image list " + path.getFileName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates that a transcription filename is consistent with its associated image name.
     * AoR transcription files follow the pattern: BookId.aor.page.xml
     * and should reference an image like: BookId.page.tif
     */
    private void validateFilenameConsistency(String bookId, String filename, AnnotatedPage page,
                                             List<String> errors) {
        // The filename of the XML should follow AoR naming conventions
        if (!PARSER.isAorTranscription(filename)) {
            errors.add("[" + bookId + "/" + filename + "] Filename does not follow AoR naming convention");
            return;
        }

        // Check the page element's filename attribute
        String pageFilename = page.getPage();
        if (pageFilename == null || pageFilename.isBlank()) {
            errors.add("[" + bookId + "/" + filename + "] Missing page filename attribute in transcription");
            return;
        }

        // Derive expected image name from the transcription filename:
        // e.g., BookId.aor.001r.xml -> BookId.001r.tif
        String expectedImageBase = deriveImageNameFromTranscription(filename);
        if (expectedImageBase != null) {
            // The page filename should at least contain the page portion
            String imageBase = stripExtension(pageFilename);
            String transcriptionBase = stripExtension(expectedImageBase);
            if (!imageBase.equals(transcriptionBase)) {
                errors.add("[" + bookId + "/" + filename + "] Transcription filename inconsistent with page reference: "
                        + "expected image '" + expectedImageBase + "' but page references '" + pageFilename + "'");
            }
        }
    }

    /**
     * Derives the expected image filename from a transcription filename.
     * E.g., "BookId.aor.001r.xml" -> "BookId.001r.tif"
     */
    private String deriveImageNameFromTranscription(String transcriptionFilename) {
        // Remove .xml extension
        String base = transcriptionFilename;
        if (base.endsWith(".xml")) {
            base = base.substring(0, base.length() - 4);
        }

        // Remove .aor. segment: "BookId.aor.001r" -> "BookId.001r"
        int aorIdx = base.indexOf(".aor.");
        if (aorIdx >= 0) {
            String prefix = base.substring(0, aorIdx);
            String suffix = base.substring(aorIdx + 5); // skip ".aor."
            return prefix + "." + suffix + ".tif";
        }

        return null;
    }

    /**
     * Strips the file extension from a filename.
     */
    private static String stripExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(0, dotIdx) : filename;
    }

    /** Reads one reference sheet file. */
    @FunctionalInterface
    private interface SheetReader {
        ReferenceSheet read(Path path, List<String> errors) throws IOException;
    }

    /**
     * Loads a reference sheet (people.csv or locations.csv), reporting an error if it cannot be loaded.
     */
    private ReferenceSheet loadReferenceSheet(Path path, String name, List<String> errors, SheetReader reader) {
        try {
            ReferenceSheet sheet = reader.read(path, errors);
            if (sheet == null) {
                errors.add("Reference file not found: " + name + " (looked in " + path.getParent() + ")");
            }
            return sheet;
        } catch (IOException e) {
            errors.add("Failed to load reference file " + name + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads a book reference sheet (Books.csv), reporting an error if it cannot be loaded.
     */
    private BookReferenceSheet loadBookReferenceSheet(Path path, String name, List<String> errors) {
        try {
            BookReferenceSheet sheet = ArchiveReaders.readBookReferenceSheet(path, errors);
            if (sheet == null) {
                errors.add("Reference file not found: " + name + " (looked in " + path.getParent() + ")");
            }
            return sheet;
        } catch (IOException e) {
            errors.add("Failed to load reference file " + name + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Lists all book directories in a collection.
     */
    private List<String> listBooks(Path collectionDir) throws IOException {
        List<String> books = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionDir, Files::isDirectory)) {
            for (Path entry : stream) {
                books.add(entry.getFileName().toString());
            }
        }
        books.sort(String::compareTo);
        return books;
    }

    /**
     * Lists all AoR transcription XML files in a book directory.
     */
    private List<String> listAorTranscriptions(Path bookDir) throws IOException {
        List<String> transcriptions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, Files::isRegularFile)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (PARSER.isAorTranscription(name)) {
                    transcriptions.add(name);
                }
            }
        }
        transcriptions.sort(String::compareTo);
        return transcriptions;
    }

    /**
     * Internal record to hold book/page data during validation.
     */
    private record BookPageData(String book, String filename, AnnotatedPage page) {}
}
