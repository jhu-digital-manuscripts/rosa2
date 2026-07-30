package rosa.archive.core;

import rosa.archive.core.serialize.ArchiveReaders;
import rosa.archive.core.util.HashUtil;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.CollectionMetadata;
import rosa.archive.model.ImageList;
import rosa.archive.model.Permission;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.aor.AnnotatedPage;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * File-system-based implementation of {@link ArchiveStore}.
 * Reads archive data from a directory hierarchy where each top-level directory
 * is a collection and each subdirectory within a collection is a book.
 */
public final class FileSystemArchiveStore implements ArchiveStore {

    private static final String ILLUSTRATION_TITLES = "illustration_titles.csv";
    private static final String NARRATIVE_SECTIONS = "narrative_sections.csv";
    private static final String CHARACTER_NAMES = "character_names.csv";
    private static final String IMAGES = ".images.csv";
    private static final String IMAGES_CROP = ".images.crop.csv";
    private static final String METADATA = ".metadata.xml";
    private static final String SHA1SUM = ".SHA1SUM";
    private static final String TRANSCRIPTION = ".transcription.xml";
    private static final String DESCRIPTION = ".description_";
    private static final String PERMISSION = ".permission_";
    private static final String PEOPLE = "people.csv";
    private static final String LOCATIONS = "locations.csv";
    private static final String BOOKS = "books.csv";
    private static final String HTML_ANNOS = "annos.jsonld";
    private static final String ID_LOCATION_MAP = "id_locations.csv";
    private static final String COLLECTION_CONFIG = "config.properties";
    private static final String MISSING_IMAGE = "missing_image.tif";
    private static final String XML_EXT = ".xml";
    private static final String HTML_EXT = ".html";

    private static final Set<String> INCLUDE_EXTENSIONS = Set.of(
            "xml", "txt", "html", "csv"
    );

    private static final String CONFIG_LABEL = "label";
    private static final String CONFIG_DESCRIPTION = "description";
    private static final String CONFIG_MISSING_HEIGHT = "missing_image.height";
    private static final String CONFIG_MISSING_WIDTH = "missing_image.width";
    private static final String CONFIG_PARENTS = "parents";
    private static final String CONFIG_CHILDREN = "children";
    private static final String CONFIG_LANGUAGES = "languages";
    private static final String CONFIG_LOGO = "logo";

    private static final ArchiveNameParser PARSER = new ArchiveNameParser();

    private final Path archivePath;

    /**
     * Creates a FileSystemArchiveStore rooted at the given path.
     *
     * @param archivePath the root directory of the archive
     */
    public FileSystemArchiveStore(Path archivePath) {
        this.archivePath = archivePath;
    }

    @Override
    public List<String> listCollections() throws IOException {
        List<String> collections = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(archivePath, Files::isDirectory)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (!name.contains(".ignore") && !name.equals("biblehistoriale")) {
                    collections.add(name);
                }
            }
        }

        collections.sort(String::compareTo);
        return collections;
    }

    @Override
    public List<String> listBooks(String collectionId) throws IOException {
        Path collectionDir = archivePath.resolve(collectionId);
        if (!Files.isDirectory(collectionDir)) {
            throw new IOException("Collection not found: " + collectionId);
        }

        List<String> books = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionDir, Files::isDirectory)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (!name.contains(".ignore")) {
                    books.add(name);
                }
            }
        }

        books.sort(String::compareTo);
        return books;
    }

    @Override
    public BookCollection loadCollection(String collectionId) throws IOException {
        Path collectionDir = archivePath.resolve(collectionId);
        if (!Files.isDirectory(collectionDir)) {
            throw new IOException("Collection not found in archive: " + collectionId);
        }

        List<String> errors = new ArrayList<>();
        BookCollection collection = new BookCollection();

        collection.setId(collectionId);

        // List books
        List<String> bookNames = listBooks(collectionId);
        collection.setBooks(bookNames.toArray(new String[0]));

        // Character names
        collection.setCharacterNames(
                ArchiveReaders.readCharacterNames(collectionDir.resolve(CHARACTER_NAMES), errors));

        // Illustration titles
        collection.setIllustrationTitles(
                ArchiveReaders.readIllustrationTitles(collectionDir.resolve(ILLUSTRATION_TITLES), errors));

        // Narrative sections
        collection.setNarrativeSections(
                ArchiveReaders.readNarrativeSections(collectionDir.resolve(NARRATIVE_SECTIONS), errors));

        // Checksum
        collection.setChecksum(
                ArchiveReaders.readSHA1Checksum(collectionDir.resolve(collectionId + SHA1SUM), errors));

        // AoR reference sheets
        collection.setPeopleRef(
                ArchiveReaders.readReferenceSheet(collectionDir.resolve(PEOPLE), errors));
        collection.setLocationsRef(
                ArchiveReaders.readReferenceSheet(collectionDir.resolve(LOCATIONS), errors));
        collection.setBooksRef(
                ArchiveReaders.readBookReferenceSheet(collectionDir.resolve(BOOKS), errors));

        // HTML annotations
        collection.setHTMLAnnotations(
                ArchiveReaders.readHTMLAnnotations(collectionDir.resolve(HTML_ANNOS), errors));

        // Annotation location map
        collection.setAnnotationMap(
                ArchiveReaders.readAnnotationLocationMap(collectionDir.resolve(ID_LOCATION_MAP)));

        // Collection config
        Path configPath = collectionDir.resolve(COLLECTION_CONFIG);
        CollectionMetadata cmd = new CollectionMetadata();

        if (Files.exists(configPath)) {
            Properties props = new Properties();
            try (InputStream configIn = Files.newInputStream(configPath)) {
                props.load(configIn);
            }

            if (props.containsKey(CONFIG_LABEL)) {
                cmd.setLabel(props.getProperty(CONFIG_LABEL));
            }

            String langs = props.getProperty(CONFIG_LANGUAGES);
            if (langs != null) {
                cmd.setLanguages(langs.split(","));
            }

            if (props.containsKey(CONFIG_MISSING_WIDTH) && props.containsKey(CONFIG_MISSING_HEIGHT)) {
                BookImage missing = new BookImage();
                missing.setId(MISSING_IMAGE);
                missing.setMissing(false);
                try {
                    missing.setWidth(Integer.parseInt(props.getProperty(CONFIG_MISSING_WIDTH)));
                    missing.setHeight(Integer.parseInt(props.getProperty(CONFIG_MISSING_HEIGHT)));
                } catch (NumberFormatException e) {
                    // Skip setting missing image on parse failure
                }
                collection.setMissingImage(missing);
            }

            if (props.containsKey(CONFIG_LOGO)) {
                cmd.setLogoUrl(props.getProperty(CONFIG_LOGO));
            }
            if (props.containsKey(CONFIG_PARENTS)) {
                String val = props.getProperty(CONFIG_PARENTS).trim();
                if (!val.isEmpty()) {
                    cmd.setParents(val.split(","));
                }
            }
            if (props.containsKey(CONFIG_CHILDREN)) {
                String val = props.getProperty(CONFIG_CHILDREN).trim();
                if (!val.isEmpty()) {
                    cmd.setChildren(val.split(","));
                }
            }
            if (props.containsKey(CONFIG_DESCRIPTION)) {
                cmd.setDescription(props.getProperty(CONFIG_DESCRIPTION));
            }
        }

        collection.setMetadata(cmd);

        return collection;
    }

    @Override
    public Book loadBook(BookCollection collection, String bookId) throws IOException {
        Path collectionDir = archivePath.resolve(collection.getId());
        if (!Files.isDirectory(collectionDir)) {
            throw new IOException("Collection not found in archive: " + collection.getId());
        }

        Path bookDir = collectionDir.resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + bookId);
        }

        List<String> errors = new ArrayList<>();
        Book book = new Book();
        book.setId(bookId);

        // Image lists
        book.setImages(
                ArchiveReaders.readImageList(bookDir.resolve(bookId + IMAGES), errors));
        book.setCroppedImages(
                ArchiveReaders.readImageList(bookDir.resolve(bookId + IMAGES_CROP), errors));

        // Book metadata
        book.setBookMetadata(
                ArchiveReaders.readBookMetadata(bookDir.resolve(bookId + METADATA), errors));

        // Checksum
        book.setChecksum(
                ArchiveReaders.readSHA1Checksum(bookDir.resolve(bookId + SHA1SUM), errors));

        // Transcription
        book.setTranscription(
                ArchiveReaders.readTranscription(bookDir.resolve(bookId + TRANSCRIPTION)));

        // Content listing
        List<String> content = listFiles(bookDir);
        book.setContent(content.toArray(new String[0]));

        // Set missing dimensions on image lists
        setMissingDimensions(book.getImages(), collection.getMissingImage());
        setMissingDimensions(book.getCroppedImages(), collection.getMissingImage());

        // Permissions and descriptions in all languages
        for (String lang : collection.getAllSupportedLanguages()) {
            String permName = bookId + PERMISSION + lang + HTML_EXT;
            Permission perm = ArchiveReaders.readPermission(bookDir.resolve(permName));
            if (perm != null) {
                book.addPermission(perm, lang);
            }
        }

        // AoR annotations
        for (String name : content) {
            if (PARSER.isAorTranscription(name)) {
                AnnotatedPage annotatedPage = ArchiveReaders.readAnnotatedPage(
                        bookDir.resolve(name), errors);
                if (annotatedPage != null) {
                    book.getAnnotatedPages().add(annotatedPage);
                }
            }
        }

        return book;
    }

    @Override
    public CheckResult check(BookCollection collection, Book book, boolean checkBits) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (book == null) {
            errors.add("Book is missing.");
            return CheckResult.of(errors, warnings);
        }

        // Verify image list exists and images are consistent with content
        checkImageList(book, errors, warnings);

        // Verify metadata file exists
        checkMetadata(book, errors, warnings);

        // If checkBits, compute SHA1 for each file and compare against stored checksums
        if (checkBits) {
            checkBitIntegrity(collection, book, errors, warnings);
        }

        return CheckResult.of(errors, warnings);
    }

    @Override
    public void validateXml(String collectionId, String bookId, List<String> errors, List<String> warnings) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book directory not found: " + collectionId + "/" + bookId);
        }

        List<String> content = listFiles(bookDir);
        for (String name : content) {
            if (PARSER.isAorTranscription(name)) {
                validateXmlWellFormedness(bookDir.resolve(name), name, errors, warnings);
            }
        }
    }

    @Override
    public void shallowCopy(Path destination) throws IOException {
        if (!Files.isDirectory(archivePath)) {
            throw new IOException("Archive path does not exist or is not readable: " + archivePath);
        }

        Files.createDirectories(destination);

        try (var walker = Files.walk(archivePath)) {
            walker.filter(Files::isRegularFile)
                  .filter(this::shouldCopyFile)
                  .forEach(source -> {
                      Path relativePath = archivePath.relativize(source);
                      Path target = destination.resolve(relativePath);
                      try {
                          Files.createDirectories(target.getParent());
                          Files.copy(source, target);
                      } catch (IOException e) {
                          throw new UncheckedIOException(e);
                      }
                  });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private boolean shouldCopyFile(Path file) {
        String name = file.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        String ext = name.substring(dotIndex + 1).toLowerCase();
        return INCLUDE_EXTENSIONS.contains(ext);
    }

    /**
     * Returns the archive root path.
     *
     * @return the archive path
     */
    public Path getArchivePath() {
        return archivePath;
    }

    // ---- Private helpers ----

    private void setMissingDimensions(ImageList images, BookImage missingImage) {
        if (missingImage == null || images == null) {
            return;
        }
        for (BookImage image : images) {
            if (image.isMissing()) {
                image.setWidth(missingImage.getWidth());
                image.setHeight(missingImage.getHeight());
                image.setMissing(true);
            }
        }
    }

    private List<String> listFiles(Path dir) throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isRegularFile)) {
            for (Path entry : stream) {
                names.add(entry.getFileName().toString());
            }
        }
        names.sort(String::compareTo);
        return names;
    }

    // ---- Check helpers ----

    /**
     * Verifies that the image list exists and that each non-missing image
     * corresponds to a file reference in the book's content listing.
     */
    private void checkImageList(Book book, List<String> errors, List<String> warnings) {
        ImageList images = book.getImages();
        if (images == null || images.getImages() == null) {
            errors.add("Image list is missing for book [" + book.getId() + "].");
            return;
        }

        String[] content = book.getContent();
        if (content == null) {
            warnings.add("Book content listing is null for book [" + book.getId() + "].");
            return;
        }

        for (BookImage image : images) {
            if (image.getId() == null || image.getId().isBlank()) {
                errors.add("Image ID not set in book [" + book.getId() + "].");
                continue;
            }

            boolean inContent = Arrays.binarySearch(content, image.getId()) >= 0;

            if (!image.isMissing() && !inContent) {
                errors.add("Image [" + image.getId() + "] marked as present is missing from archive ["
                        + book.getId() + "].");
            } else if (image.isMissing() && inContent) {
                errors.add("Image [" + image.getId() + "] marked as missing is present in archive ["
                        + book.getId() + "].");
            }
        }
    }

    /**
     * Verifies that the book metadata file exists in the book's content listing.
     */
    private void checkMetadata(Book book, List<String> errors, List<String> warnings) {
        String metadataName = book.getId() + METADATA;
        String[] content = book.getContent();

        if (content == null) {
            warnings.add("Book content listing is null for book [" + book.getId() + "].");
            return;
        }

        if (Arrays.binarySearch(content, metadataName) < 0) {
            warnings.add("Metadata file not found in book content [" + metadataName + "].");
        }

        if (book.getBookMetadata() == null) {
            warnings.add("Book metadata object is null for book [" + book.getId() + "].");
        }
    }

    /**
     * Computes SHA1 for each file in the book directory and compares against stored checksums.
     */
    private void checkBitIntegrity(BookCollection collection, Book book, List<String> errors, List<String> warnings) {
        SHA1Checksum storedChecksums = book.getChecksum();
        if (storedChecksums == null) {
            errors.add("Book [" + book.getId() + "] has no stored SHA1SUM. Cannot check bit integrity.");
            return;
        }

        Path bookDir = archivePath.resolve(collection.getId()).resolve(book.getId());
        if (!Files.isDirectory(bookDir)) {
            errors.add("Book directory not found: " + bookDir);
            return;
        }

        String[] content = book.getContent();
        if (content == null) {
            errors.add("Book content listing is null for book [" + book.getId() + "].");
            return;
        }

        for (String fileName : content) {
            // Skip the checksum file itself
            if (fileName.contains(SHA1SUM)) {
                continue;
            }

            String storedHash = storedChecksums.checksums().get(fileName);
            if (storedHash == null) {
                // No stored checksum for this file, skip
                continue;
            }

            Path filePath = bookDir.resolve(fileName);
            if (!Files.isRegularFile(filePath)) {
                errors.add("File listed in content not found on disk: " + fileName);
                continue;
            }

            try {
                String computedHash = HashUtil.computeSHA1(filePath);
                if (!storedHash.equalsIgnoreCase(computedHash)) {
                    errors.add("Checksum mismatch for [" + fileName + "]: "
                            + "stored=" + storedHash + ", computed=" + computedHash);
                }
            } catch (IOException e) {
                errors.add("Failed to compute checksum for [" + fileName + "]: " + e.getMessage());
            }
        }
    }

    // ---- XML validation helpers ----

    /**
     * Validates that an XML file is well-formed by parsing it with a namespace-aware
     * DocumentBuilder. Reports parse errors and warnings to the provided lists.
     */
    private void validateXmlWellFormedness(Path xmlFile, String fileName, List<String> errors, List<String> warnings) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable external entities for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    warnings.add("[Warn: " + fileName + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }

                @Override
                public void error(SAXParseException e) {
                    errors.add("[Error: " + fileName + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) {
                    errors.add("[Fatal: " + fileName + "] (" + e.getLineNumber() + ":"
                            + e.getColumnNumber() + "): " + e.getMessage());
                }
            });

            try (InputStream in = Files.newInputStream(xmlFile)) {
                builder.parse(in);
            }
        } catch (ParserConfigurationException e) {
            errors.add("XML parser configuration error for [" + fileName + "]: " + e.getMessage());
        } catch (SAXException e) {
            errors.add("[" + fileName + "] is not well-formed XML: " + e.getMessage());
        } catch (IOException e) {
            errors.add("Failed to read [" + fileName + "]: " + e.getMessage());
        }
    }
}
