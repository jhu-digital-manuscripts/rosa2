package rosa.archive.aor;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.serialize.ArchiveReaders;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates AoR transcription XML files against reference spreadsheets.
 *
 * <p>Checks that people, book, and location references in annotations match
 * entries in the respective reference CSV files. Also detects duplicate annotation
 * IDs and validates internal reference targets.</p>
 */
public final class AoRTranscriptionChecker {

    private static final String PEOPLE_CSV = "people.csv";
    private static final String BOOKS_CSV = "books.csv";
    private static final String LOCATIONS_CSV = "locations.csv";

    private static final ArchiveNameParser PARSER = new ArchiveNameParser();

    private final Path archivePath;
    private final Path spreadsheetDir;

    /**
     * Creates a new AoR transcription checker.
     *
     * @param archivePath    the root directory of the archive
     * @param spreadsheetDir the directory containing reference CSV files, or null
     *                       to look in the collection directory
     */
    public AoRTranscriptionChecker(Path archivePath, Path spreadsheetDir) {
        this.archivePath = archivePath;
        this.spreadsheetDir = spreadsheetDir;
    }

    /**
     * Validates AoR transcription files against reference spreadsheets.
     *
     * @param collectionId the collection to check
     * @param bookId       optional book ID (null means all books in collection)
     * @return list of validation error messages
     */
    public List<String> run(String collectionId, String bookId) {
        List<String> errors = new ArrayList<>();

        Path collectionDir = archivePath.resolve(collectionId);
        if (!Files.isDirectory(collectionDir)) {
            errors.add("Collection not found: " + collectionId);
            return errors;
        }

        // Determine where to load reference CSVs from
        Path refDir = spreadsheetDir != null ? spreadsheetDir : collectionDir;

        // Load reference data
        ReferenceSheet peopleRef = loadReferenceSheet(refDir.resolve(PEOPLE_CSV), PEOPLE_CSV, errors);
        BookReferenceSheet booksRef = loadBookReferenceSheet(refDir.resolve(BOOKS_CSV), BOOKS_CSV, errors);
        ReferenceSheet locationsRef = loadReferenceSheet(refDir.resolve(LOCATIONS_CSV), LOCATIONS_CSV, errors);

        // Collect all annotation IDs across the corpus for internal reference validation
        Map<String, List<String>> allAnnotationIds = new HashMap<>(); // id -> list of locations
        List<BookPageData> allPages = new ArrayList<>();

        // Determine books to check
        List<String> booksToCheck = new ArrayList<>();
        if (bookId != null) {
            booksToCheck.add(bookId);
        } else {
            try {
                booksToCheck.addAll(listBooks(collectionDir));
            } catch (IOException e) {
                errors.add("Failed to list books in collection " + collectionId + ": " + e.getMessage());
                return errors;
            }
        }

        // First pass: load all pages and collect annotation IDs
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

            for (String xmlFile : xmlFiles) {
                Path xmlPath = bookDir.resolve(xmlFile);
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
                        allAnnotationIds.computeIfAbsent(id, k -> new ArrayList<>()).add(location);
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
            validatePage(pageData, peopleRef, booksRef, locationsRef, knownIds, errors);
        }

        return errors;
    }

    /**
     * Validates a single annotated page against reference data.
     */
    private void validatePage(BookPageData pageData, ReferenceSheet peopleRef,
                              BookReferenceSheet booksRef, ReferenceSheet locationsRef,
                              Set<String> knownIds, List<String> errors) {
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
                            errors.add("[" + context + "] Unknown person reference: " + person);
                        }
                    }
                    // Check book references
                    for (String book : pos.getBooks()) {
                        if (booksRef != null && !booksRef.containsKey(book)) {
                            errors.add("[" + context + "] Unknown book reference: " + book);
                        }
                    }
                    // Check location references
                    for (String location : pos.getLocations()) {
                        if (locationsRef != null && !locationsRef.containsKey(location)) {
                            errors.add("[" + context + "] Unknown location reference: " + location);
                        }
                    }

                    // Check internal references
                    for (InternalReference ref : pos.getInternalRefs()) {
                        validateInternalReference(ref, knownIds, context, errors);
                    }
                    for (InternalReference ref : pos.getMarginaliaRefs()) {
                        validateInternalReference(ref, knownIds, context, errors);
                    }
                }
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
     * Validates targets within an internal reference point to known annotation IDs.
     */
    private void validateInternalReference(InternalReference ref, Set<String> knownIds,
                                           String context, List<String> errors) {
        for (ReferenceTarget target : ref.getTargets()) {
            String targetId = target.targetId();
            if (targetId != null && !targetId.isBlank() && !knownIds.contains(targetId)) {
                errors.add("[" + context + "] Internal reference target not found: " + targetId);
            }
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
    private String stripExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(0, dotIdx) : filename;
    }

    /**
     * Loads a reference sheet (People.csv or Locations.csv), reporting an error if it cannot be loaded.
     */
    private ReferenceSheet loadReferenceSheet(Path path, String name, List<String> errors) {
        try {
            ReferenceSheet sheet = ArchiveReaders.readReferenceSheet(path, errors);
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
