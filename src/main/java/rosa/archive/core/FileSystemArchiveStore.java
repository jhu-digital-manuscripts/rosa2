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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String TIF_EXT = ".tif";
    private static final String FILE_MAP = "filemap.csv";
    private static final String IMG_FRONTCOVER = ".binding.frontcover.tif";
    private static final String IMG_BACKCOVER = ".binding.backcover.tif";
    private static final String IMG_FRONTPASTEDOWN = ".frontmatter.pastedown.tif";
    private static final String IMG_ENDPASTEDOWN = ".endmatter.pastedown.tif";
    private static final String IMG_FRONT_FLYLEAF = ".frontmatter.flyleaf.";
    private static final String IMG_END_FLYLEAF = ".endmatter.flyleaf.";

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

        // Illustration tagging
        book.setIllustrationTagging(
                ArchiveReaders.readIllustrationTagging(bookDir.resolve(bookId + ".imagetag.csv"), errors));

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

    @Override
    public void updateChecksum(String collectionId, boolean force, List<String> errors) throws IOException {
        Path collectionDir = archivePath.resolve(collectionId);
        if (!Files.isDirectory(collectionDir)) {
            throw new IOException("Collection not found: " + collectionId);
        }

        Path checksumFile = collectionDir.resolve(collectionId + SHA1SUM);
        Map<String, String> existingChecksums = loadExistingChecksums(checksumFile, errors);
        long checksumLastModified = checksumFileLastModified(checksumFile);

        Map<String, String> updatedChecksums = new java.util.LinkedHashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionDir, Files::isRegularFile)) {
            List<Path> files = new ArrayList<>();
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                // Skip the checksum file itself and hidden files
                if (name.equals(collectionId + SHA1SUM) || name.startsWith(".")) {
                    continue;
                }
                files.add(entry);
            }
            files.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));

            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String hash = computeOrReuseChecksum(file, fileName, force, checksumLastModified,
                        existingChecksums, errors);
                if (hash != null) {
                    updatedChecksums.put(fileName, hash);
                }
            }
        }

        writeChecksumFile(checksumFile, updatedChecksums);
    }

    @Override
    public void updateChecksum(String collectionId, String bookId, boolean force, List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        Path checksumFile = bookDir.resolve(bookId + SHA1SUM);
        Map<String, String> existingChecksums = loadExistingChecksums(checksumFile, errors);
        long checksumLastModified = checksumFileLastModified(checksumFile);

        Map<String, String> updatedChecksums = new java.util.LinkedHashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, Files::isRegularFile)) {
            List<Path> files = new ArrayList<>();
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                // Skip the checksum file itself
                if (name.equals(bookId + SHA1SUM)) {
                    continue;
                }
                files.add(entry);
            }
            files.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));

            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String hash = computeOrReuseChecksum(file, fileName, force, checksumLastModified,
                        existingChecksums, errors);
                if (hash != null) {
                    updatedChecksums.put(fileName, hash);
                }
            }
        }

        writeChecksumFile(checksumFile, updatedChecksums);
    }

    @Override
    public void generateAndWriteImageList(String collectionId, String bookId, boolean force, List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        Path imagesCsv = bookDir.resolve(bookId + IMAGES);
        if (!force && Files.exists(imagesCsv)) {
            return;
        }

        List<Path> imageFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, this::isImageFile)) {
            for (Path entry : stream) {
                imageFiles.add(entry);
            }
        }
        imageFiles.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));

        var sb = new StringBuilder();
        for (Path imageFile : imageFiles) {
            String filename = imageFile.getFileName().toString();
            int[] dimensions = readImageDimensions(imageFile, errors);
            int width = dimensions[0];
            int height = dimensions[1];
            sb.append(filename).append(',').append(width).append(',').append(height).append(",false\n");
        }

        Files.writeString(imagesCsv, sb.toString(), StandardCharsets.UTF_8);
    }

    @Override
    public void cropImages(String collectionId, String bookId, boolean force, List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        Path cropFile = bookDir.resolve(bookId + ".crop.txt");
        if (!Files.exists(cropFile)) {
            errors.add("Crop file not found: " + cropFile);
            return;
        }

        Path croppedDir = bookDir.resolve("cropped");
        Files.createDirectories(croppedDir);

        List<String> lines = Files.readAllLines(cropFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 5) {
                errors.add("Malformed crop line: " + line);
                continue;
            }

            String filename = parts[0].trim();
            Path croppedFile = croppedDir.resolve(filename);

            if (!force && Files.exists(croppedFile)) {
                continue;
            }

            Path sourceFile = bookDir.resolve(filename);
            if (!Files.isRegularFile(sourceFile)) {
                errors.add("Source image not found for cropping: " + filename);
                continue;
            }

            try {
                int left = Integer.parseInt(parts[1].trim());
                int top = Integer.parseInt(parts[2].trim());
                int right = Integer.parseInt(parts[3].trim());
                int bottom = Integer.parseInt(parts[4].trim());

                BufferedImage source = ImageIO.read(sourceFile.toFile());
                if (source == null) {
                    errors.add("Failed to read image for cropping: " + filename);
                    continue;
                }

                int cropWidth = right - left;
                int cropHeight = bottom - top;

                if (left < 0 || top < 0 || right > source.getWidth() || bottom > source.getHeight()
                        || cropWidth <= 0 || cropHeight <= 0) {
                    errors.add("Invalid crop region for [" + filename + "]: "
                            + left + "," + top + "," + right + "," + bottom);
                    continue;
                }

                BufferedImage cropped = source.getSubimage(left, top, cropWidth, cropHeight);
                String ext = getImageExtension(filename);
                ImageIO.write(cropped, ext, croppedFile.toFile());
            } catch (NumberFormatException e) {
                errors.add("Failed to parse crop coordinates for [" + filename + "]: " + e.getMessage());
            } catch (IOException e) {
                errors.add("Failed to crop image [" + filename + "]: " + e.getMessage());
            }
        }
    }

    @Override
    public void generateAndWriteCropList(String collectionId, String bookId, boolean force, List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        Path cropCsv = bookDir.resolve(bookId + IMAGES_CROP);
        if (!force && Files.exists(cropCsv)) {
            return;
        }

        Path croppedDir = bookDir.resolve("cropped");
        if (!Files.isDirectory(croppedDir)) {
            errors.add("Cropped directory not found: " + croppedDir);
            return;
        }

        List<Path> imageFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(croppedDir, this::isImageFile)) {
            for (Path entry : stream) {
                imageFiles.add(entry);
            }
        }
        imageFiles.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));

        var sb = new StringBuilder();
        for (Path imageFile : imageFiles) {
            String filename = imageFile.getFileName().toString();
            int[] dimensions = readImageDimensions(imageFile, errors);
            int width = dimensions[0];
            int height = dimensions[1];
            sb.append(filename).append(',').append(width).append(',').append(height).append(",false\n");
        }

        Files.writeString(cropCsv, sb.toString(), StandardCharsets.UTF_8);
    }

    @Override
    public void generateFileMap(String collectionId, String bookId, String newId, boolean hasFrontCover,
                                boolean hasBackCover, int numFrontmatter, int numEndmatter, int numMisc,
                                List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        List<String> imageNames = listTifFiles(bookDir);
        Map<String, String> map = buildFileMap(imageNames, newId, hasFrontCover, hasBackCover,
                numFrontmatter, numEndmatter, numMisc);

        // Write filemap.csv sorted by old filename (key)
        Path fileMapPath = bookDir.resolve(FILE_MAP);
        List<Map.Entry<String, String>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort((a, b) -> a.getKey().compareTo(b.getKey()));

        var sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted) {
            sb.append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
        }
        Files.writeString(fileMapPath, sb.toString(), StandardCharsets.UTF_8);
    }

    @Override
    public void renameImages(String collectionId, String bookId, boolean changeId, boolean reverse,
                             List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        if (changeId) {
            // Simply replace the ID prefix in all image filenames with the book directory name
            List<String> imageNames = listTifFiles(bookDir);
            for (String image : imageNames) {
                String target = bookId + image.substring(image.indexOf('.'));
                if (target.equals(image)) {
                    continue;
                }
                Files.move(bookDir.resolve(image), bookDir.resolve(target));
            }
            return;
        }

        Path fileMapPath = bookDir.resolve(FILE_MAP);
        if (!Files.isRegularFile(fileMapPath)) {
            errors.add("No file map found. Cannot change image names.");
            return;
        }

        Map<String, String> fileMap = readFileMap(fileMapPath, errors);
        if (fileMap.isEmpty()) {
            errors.add("Failed to load file map. Cannot rename images.");
            return;
        }

        if (containsDuplicateValues(fileMap, errors)) {
            errors.add("Duplicate target names found. Check the file map.");
            return;
        }

        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            String source;
            String target;

            if (reverse) {
                source = entry.getValue();
                target = entry.getKey();
            } else {
                source = entry.getKey();
                target = entry.getValue();
            }

            if (source == null || source.isBlank()) {
                errors.add("Failed to rename image, no source image was specified.");
                continue;
            }
            if (target == null || target.isBlank()) {
                errors.add("Failed to rename image, no target was specified.");
                continue;
            }

            Path sourcePath = bookDir.resolve(source);
            Path targetPath = bookDir.resolve(target);

            if (!Files.exists(sourcePath)) {
                errors.add("Source file not found: " + source);
                continue;
            }
            if (Files.exists(targetPath)) {
                errors.add("Failed to rename image, an image with the target name already exists. ("
                        + source + " -> " + target + ")");
                continue;
            }

            Files.move(sourcePath, targetPath);
        }
    }

    @Override
    public void renameTranscriptions(String collectionId, String bookId, boolean reverse,
                                     List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        Path fileMapPath = bookDir.resolve(FILE_MAP);
        if (!Files.isRegularFile(fileMapPath)) {
            errors.add("No file map found. Cannot rename transcriptions.");
            return;
        }

        Map<String, String> fileMap = readFileMap(fileMapPath, errors);
        if (fileMap.isEmpty()) {
            errors.add("Failed to load file map. Cannot rename transcriptions.");
            return;
        }

        if (containsDuplicateValues(fileMap, errors)) {
            // Duplicate entries mean data loss risk — abort
            return;
        }

        // Find all AoR transcription XML files
        List<String> transcriptions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, Files::isRegularFile)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (PARSER.isAorTranscription(name)) {
                    transcriptions.add(name);
                }
            }
        }

        for (String pageName : transcriptions) {
            renameTranscription(pageName, fileMap, bookDir, reverse, errors);
        }
    }

    @Override
    public void generateTEITranscriptions(String collectionId, String bookId, List<String> errors,
                                          List<String> warnings) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        List<String> content = listFiles(bookDir);
        for (String name : content) {
            if (PARSER.isAorTranscription(name)) {
                generateSingleTEI(bookDir, name, errors, warnings);
            }
        }
    }

    /**
     * Generates a single TEI P5 XML file from an AoR transcription XML file.
     *
     * @param bookDir  the book directory
     * @param fileName the AoR transcription filename
     * @param errors   list to collect error messages
     * @param warnings list to collect warning messages
     */
    private void generateSingleTEI(Path bookDir, String fileName, List<String> errors,
                                   List<String> warnings) {
        Path aorFile = bookDir.resolve(fileName);

        Document aorDoc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            // Allow DTD references but don't fetch them
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            try (InputStream in = Files.newInputStream(aorFile)) {
                aorDoc = builder.parse(in);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            errors.add("Failed to read AoR transcription [" + fileName + "]: " + e.getMessage());
            return;
        }

        // Extract page identifier for the output filename
        String pageId = extractPageId(aorDoc, fileName);
        String outputName = pageId + ".tei.xml";

        try {
            Document teiDoc = buildTEIDocument(aorDoc, pageId, warnings);
            Path outputFile = bookDir.resolve(outputName);
            try (OutputStream os = Files.newOutputStream(outputFile)) {
                writeXml(teiDoc, os);
            }
        } catch (ParserConfigurationException | IOException e) {
            errors.add("Failed to write TEI file [" + outputName + "]: " + e.getMessage());
        }
    }

    /**
     * Extracts the page identifier from the AoR document's {@code <page>} element filename attribute.
     * Falls back to deriving the page id from the filename.
     */
    private String extractPageId(Document aorDoc, String fileName) {
        NodeList pageEls = aorDoc.getElementsByTagName("page");
        if (pageEls.getLength() > 0) {
            Element pageEl = (Element) pageEls.item(0);
            String imageFilename = pageEl.getAttribute("filename");
            if (imageFilename != null && !imageFilename.isBlank()) {
                // Remove extension from image filename to get page id
                // e.g. "2862_005.tif" -> "2862_005", "Ha2.001r.tif" -> "Ha2.001r"
                int dotIdx = imageFilename.lastIndexOf('.');
                return dotIdx > 0 ? imageFilename.substring(0, dotIdx) : imageFilename;
            }
        }
        // Fallback: derive from AoR filename by removing the .xml extension and .aor. segment
        // e.g. "BookId.aor.001r.xml" -> "001r"
        String base = fileName.replace(".xml", "");
        int aorIdx = base.indexOf(".aor.");
        if (aorIdx >= 0) {
            return base.substring(aorIdx + 5);
        }
        return base;
    }

    /**
     * Builds a TEI P5 document from an AoR transcription document.
     */
    private Document buildTEIDocument(Document aorDoc, String pageId,
                                      List<String> warnings) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document teiDoc = builder.newDocument();

        // Root TEI element
        Element tei = teiDoc.createElement("TEI");
        tei.setAttribute("xmlns", "http://www.tei-c.org/ns/1.0");
        teiDoc.appendChild(tei);

        // teiHeader
        Element teiHeader = teiDoc.createElement("teiHeader");
        tei.appendChild(teiHeader);

        Element fileDesc = teiDoc.createElement("fileDesc");
        teiHeader.appendChild(fileDesc);

        Element titleStmt = teiDoc.createElement("titleStmt");
        fileDesc.appendChild(titleStmt);
        Element title = teiDoc.createElement("title");
        title.setTextContent("AoR transcription of page " + pageId);
        titleStmt.appendChild(title);

        Element publicationStmt = teiDoc.createElement("publicationStmt");
        fileDesc.appendChild(publicationStmt);
        Element p = teiDoc.createElement("p");
        p.setTextContent("Generated from Archaeology of Reading transcription data");
        publicationStmt.appendChild(p);

        Element sourceDesc = teiDoc.createElement("sourceDesc");
        fileDesc.appendChild(sourceDesc);
        Element sourceP = teiDoc.createElement("p");
        sourceP.setTextContent("Converted from AoR transcription XML");
        sourceDesc.appendChild(sourceP);

        // text > body
        Element text = teiDoc.createElement("text");
        tei.appendChild(text);
        Element body = teiDoc.createElement("body");
        text.appendChild(body);

        Element div = teiDoc.createElement("div");
        div.setAttribute("type", "annotations");
        body.appendChild(div);

        // Process annotations
        NodeList annotationEls = aorDoc.getElementsByTagName("annotation");
        if (annotationEls.getLength() > 0) {
            Element annotationEl = (Element) annotationEls.item(0);
            transformAnnotations(teiDoc, div, annotationEl, warnings);
        }

        return teiDoc;
    }

    /**
     * Transforms all annotations within an {@code <annotation>} element into TEI elements.
     */
    private void transformAnnotations(Document teiDoc, Element parent, Element annotationEl,
                                      List<String> warnings) {
        NodeList children = annotationEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) child;
            String tagName = el.getTagName();

            switch (tagName) {
                case "marginalia" -> transformMarginalia(teiDoc, parent, el);
                case "underline" -> transformUnderline(teiDoc, parent, el);
                case "mark" -> transformMark(teiDoc, parent, el);
                case "symbol" -> transformSymbol(teiDoc, parent, el);
                case "errata" -> transformErrata(teiDoc, parent, el);
                default -> warnings.add("Unmappable annotation type: " + tagName);
            }
        }
    }

    /**
     * Transforms a {@code <marginalia>} element into a TEI {@code <note>} element.
     */
    private void transformMarginalia(Document teiDoc, Element parent, Element marginalia) {
        Element note = teiDoc.createElement("note");
        note.setAttribute("type", "marginalia");

        String hand = marginalia.getAttribute("hand");
        if (hand != null && !hand.isBlank()) {
            note.setAttribute("hand", hand);
        }

        // Extract place from the first position element
        NodeList positions = marginalia.getElementsByTagName("position");
        if (positions.getLength() > 0) {
            Element pos = (Element) positions.item(0);
            String place = pos.getAttribute("place");
            if (place != null && !place.isBlank()) {
                note.setAttribute("place", place);
            }
        }

        // Collect all marginalia_text content
        NodeList textEls = marginalia.getElementsByTagName("marginalia_text");
        StringBuilder textContent = new StringBuilder();
        for (int i = 0; i < textEls.getLength(); i++) {
            String t = textEls.item(i).getTextContent().trim();
            if (!t.isEmpty()) {
                if (!textContent.isEmpty()) {
                    textContent.append(" ");
                }
                textContent.append(t);
            }
        }

        if (!textContent.isEmpty()) {
            note.setTextContent(textContent.toString());
        }

        parent.appendChild(note);
    }

    /**
     * Transforms an {@code <underline>} element into a TEI {@code <hi rend="underline">} element.
     */
    private void transformUnderline(Document teiDoc, Element parent, Element underline) {
        Element hi = teiDoc.createElement("hi");
        hi.setAttribute("rend", "underline");

        String text = underline.getAttribute("text");
        if (text != null && !text.isBlank()) {
            hi.setTextContent(text);
        }

        parent.appendChild(hi);
    }

    /**
     * Transforms a {@code <mark>} element into a TEI {@code <metamark>} element.
     */
    private void transformMark(Document teiDoc, Element parent, Element mark) {
        Element metamark = teiDoc.createElement("metamark");

        String name = mark.getAttribute("name");
        if (name != null && !name.isBlank()) {
            metamark.setAttribute("function", name);
        }

        String place = mark.getAttribute("place");
        if (place != null && !place.isBlank()) {
            metamark.setAttribute("place", place);
        }

        String text = mark.getAttribute("text");
        if (text != null && !text.isBlank()) {
            metamark.setTextContent(text);
        }

        parent.appendChild(metamark);
    }

    /**
     * Transforms a {@code <symbol>} element into a TEI {@code <g>} (glyph) element.
     */
    private void transformSymbol(Document teiDoc, Element parent, Element symbol) {
        Element g = teiDoc.createElement("g");

        String name = symbol.getAttribute("name");
        if (name != null && !name.isBlank()) {
            g.setAttribute("ref", "#" + name);
        }

        String place = symbol.getAttribute("place");
        if (place != null && !place.isBlank()) {
            g.setAttribute("place", place);
        }

        parent.appendChild(g);
    }

    /**
     * Transforms an {@code <errata>} element into a TEI {@code <choice><sic>...<corr>...</choice>}.
     */
    private void transformErrata(Document teiDoc, Element parent, Element errata) {
        Element choice = teiDoc.createElement("choice");

        Element sic = teiDoc.createElement("sic");
        String copytext = errata.getAttribute("copytext");
        if (copytext != null && !copytext.isBlank()) {
            sic.setTextContent(copytext);
        }
        choice.appendChild(sic);

        Element corr = teiDoc.createElement("corr");
        String amendedtext = errata.getAttribute("amendedtext");
        if (amendedtext != null && !amendedtext.isBlank()) {
            corr.setTextContent(amendedtext);
        }
        choice.appendChild(corr);

        parent.appendChild(choice);
    }

    /**
     * Loads existing checksums from a checksum file, or returns an empty map if the file doesn't exist.
     */
    private Map<String, String> loadExistingChecksums(Path checksumFile, List<String> errors) throws IOException {
        if (!Files.exists(checksumFile)) {
            return Map.of();
        }
        SHA1Checksum existing = ArchiveReaders.readSHA1Checksum(checksumFile, errors);
        return existing != null ? existing.checksums() : Map.of();
    }

    /**
     * Returns the last-modified time of the checksum file in millis, or 0 if it doesn't exist.
     */
    private long checksumFileLastModified(Path checksumFile) throws IOException {
        if (!Files.exists(checksumFile)) {
            return 0;
        }
        return Files.getLastModifiedTime(checksumFile).toMillis();
    }

    /**
     * Computes a new checksum for a file, or reuses the existing one if the file hasn't changed
     * and force is false.
     *
     * @return the SHA-1 hash, or null if computation fails
     */
    private String computeOrReuseChecksum(Path file, String fileName, boolean force,
                                          long checksumLastModified,
                                          Map<String, String> existingChecksums,
                                          List<String> errors) {
        if (!force) {
            String existingHash = existingChecksums.get(fileName);
            if (existingHash != null) {
                // File has an existing checksum — check if it needs recomputation
                try {
                    long fileLastModified = Files.getLastModifiedTime(file).toMillis();
                    if (fileLastModified <= checksumLastModified) {
                        // File not modified since checksum was written — reuse existing hash
                        return existingHash;
                    }
                } catch (IOException e) {
                    errors.add("Failed to read last-modified time for [" + fileName + "]: " + e.getMessage());
                    return existingHash;
                }
            }
            // File is missing from checksum file or has been modified — recompute
        }

        try {
            return HashUtil.computeSHA1(file);
        } catch (IOException e) {
            errors.add("Failed to compute checksum for [" + fileName + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Writes the checksum map to a file in sha1sum-compatible format:
     * {@code hash  filename} (two spaces between hash and filename).
     */
    private void writeChecksumFile(Path checksumFile, Map<String, String> checksums) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (var entry : checksums.entrySet()) {
            sb.append(entry.getValue()).append("  ").append(entry.getKey()).append('\n');
        }
        Files.writeString(checksumFile, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
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
     * Returns whether the given path is an image file (.tif or .jpg, case-insensitive).
     */
    private boolean isImageFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return Files.isRegularFile(file) && (name.endsWith(".tif") || name.endsWith(".jpg"));
    }

    /**
     * Reads the dimensions of an image file without loading the full raster data
     * into memory (uses ImageReader for efficient header-only reading).
     *
     * @return an int array of [width, height], or [0, 0] if dimensions cannot be read
     */
    private int[] readImageDimensions(Path imageFile, List<String> errors) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile.toFile())) {
            if (iis == null) {
                errors.add("Cannot create image input stream for: " + imageFile.getFileName());
                return new int[]{0, 0};
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                errors.add("No image reader found for: " + imageFile.getFileName());
                return new int[]{0, 0};
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return new int[]{width, height};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            errors.add("Failed to read dimensions for [" + imageFile.getFileName() + "]: " + e.getMessage());
            return new int[]{0, 0};
        }
    }

    /**
     * Extracts the image format extension from a filename (without the dot).
     * Defaults to "tif" if no recognized extension is found.
     */
    private String getImageExtension(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "jpg";
        }
        return "tif";
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

    // ---- File map and rename helpers ----

    /**
     * Lists all {@code .tif} files in a directory, sorted alphabetically.
     *
     * @param dir the directory to scan
     * @return sorted list of {@code .tif} filenames
     * @throws IOException if the directory cannot be read
     */
    private List<String> listTifFiles(Path dir) throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isRegularFile)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (name.toLowerCase().endsWith(TIF_EXT)) {
                    names.add(name);
                }
            }
        }
        names.sort(String::compareTo);
        return names;
    }

    /**
     * Builds a file map from old image filenames to new standardized filenames.
     * The naming convention allocates images sequentially:
     * front cover, front pastedown, frontmatter flyleaves, body pages,
     * endmatter flyleaves, back pastedown, back cover, misc.
     *
     * @param fileNames      sorted list of existing image filenames
     * @param id             the new book ID for generated names
     * @param hasFrontCover  whether to allocate front cover and pastedown
     * @param hasBackCover   whether to allocate back cover and pastedown
     * @param numFrontmatter number of frontmatter flyleaf images
     * @param numEndmatter   number of endmatter flyleaf images
     * @param numMisc        number of miscellaneous images
     * @return map of old filename → new filename
     */
    private Map<String, String> buildFileMap(List<String> fileNames, String id, boolean hasFrontCover,
                                             boolean hasBackCover, int numFrontmatter, int numEndmatter,
                                             int numMisc) {
        Map<String, String> map = new LinkedHashMap<>();
        int idx = 0;

        // Front cover and pastedown
        if (hasFrontCover) {
            map.put(fileNames.get(idx++), id + IMG_FRONTCOVER);
            map.put(fileNames.get(idx++), id + IMG_FRONTPASTEDOWN);
        }

        // Frontmatter flyleaves (recto + verso pairs)
        int nextSeq = 1;
        char nextRv = 'r';
        for (int i = 0; i < numFrontmatter; i++) {
            map.put(fileNames.get(idx++),
                    id + IMG_FRONT_FLYLEAF + String.format("%03d", nextSeq) + nextRv + TIF_EXT);
            if (nextRv == 'v') {
                nextSeq++;
                nextRv = 'r';
            } else {
                nextRv = 'v';
            }
        }

        // Calculate end positions
        int total = fileNames.size();
        int endFlyleafEnd = hasBackCover ? total - numMisc - 2 : total - numMisc;
        int endFlyleafStart = endFlyleafEnd - numEndmatter;

        // Body pages (recto + verso pairs)
        nextSeq = 1;
        nextRv = 'r';
        for (int i = idx; i < endFlyleafStart; i++) {
            map.put(fileNames.get(i),
                    id + "." + String.format("%03d", nextSeq) + nextRv + TIF_EXT);
            if (nextRv == 'v') {
                nextSeq++;
                nextRv = 'r';
            } else {
                nextRv = 'v';
            }
        }

        // Endmatter flyleaves (recto + verso pairs)
        nextSeq = 1;
        nextRv = 'r';
        for (int i = endFlyleafStart; i < endFlyleafEnd; i++) {
            map.put(fileNames.get(i),
                    id + IMG_END_FLYLEAF + String.format("%03d", nextSeq) + nextRv + TIF_EXT);
            if (nextRv == 'v') {
                nextSeq++;
                nextRv = 'r';
            } else {
                nextRv = 'v';
            }
        }

        // Back pastedown and cover
        int afterEnd = endFlyleafEnd;
        if (hasBackCover) {
            map.put(fileNames.get(afterEnd++), id + IMG_ENDPASTEDOWN);
            map.put(fileNames.get(afterEnd++), id + IMG_BACKCOVER);
        }

        // Misc images
        for (int i = afterEnd; i < total; i++) {
            map.put(fileNames.get(i), id + ".misc." + String.format("%03d", i - afterEnd + 1) + TIF_EXT);
        }

        return map;
    }

    /**
     * Reads a file map CSV from disk. Each line is {@code oldname,newname}.
     * Lines starting with {@code #} are treated as comments.
     *
     * @param fileMapPath path to the filemap.csv
     * @param errors      list to collect error messages
     * @return map of old name → new name
     * @throws IOException if the file cannot be read
     */
    private Map<String, String> readFileMap(Path fileMapPath, List<String> errors) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(fileMapPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            // Strip inline comments
            if (line.contains("#")) {
                line = line.substring(0, line.indexOf('#'));
            }
            String[] parts = line.split(",", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                continue;
            }
            map.put(parts[0].trim(), parts[1].trim());
        }
        return map;
    }

    /**
     * Checks whether a map contains duplicate values. Reports all duplicates.
     *
     * @param map    the map to check
     * @param errors list to collect error messages for duplicates found
     * @return true if duplicates exist
     */
    private boolean containsDuplicateValues(Map<String, String> map, List<String> errors) {
        boolean hasDuplicates = false;
        Set<String> seen = new HashSet<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (seen.contains(entry.getValue())) {
                hasDuplicates = true;
                errors.add("Duplicate entry: [" + entry.getKey() + "," + entry.getValue() + "]");
            } else {
                seen.add(entry.getValue());
            }
        }
        return hasDuplicates;
    }

    /**
     * Converts an image filename to the corresponding AoR transcription filename.
     * For example, {@code BookId.001r.tif} becomes {@code BookId.aor.001r.xml}.
     *
     * @param imageName the image filename
     * @return the transcription filename
     */
    private String imageToTranscriptionName(String imageName) {
        List<String> parts = new ArrayList<>(Arrays.asList(imageName.split("\\.")));
        parts.add(1, "aor");

        var sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts.get(i));
        }
        return sb.toString().replace(TIF_EXT, XML_EXT);
    }

    /**
     * Renames a single transcription file and updates the internal {@code page}
     * element's {@code filename} attribute to reference the new image filename.
     *
     * @param pageName   the current transcription filename
     * @param fileMap    the file map (image old → new)
     * @param bookDir    the book directory path
     * @param reverse    whether to apply in reverse direction
     * @param errors     list to collect error messages
     */
    private void renameTranscription(String pageName, Map<String, String> fileMap, Path bookDir,
                                     boolean reverse, List<String> errors) {
        try {
            Path filePath = bookDir.resolve(pageName);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc;
            try (InputStream in = Files.newInputStream(filePath)) {
                doc = builder.parse(in);
            }

            String transformedImageName = null;

            NodeList pageEls = doc.getElementsByTagName("page");
            for (int i = 0; i < pageEls.getLength(); i++) {
                Node pageNode = pageEls.item(i);
                if (pageNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element page = (Element) pageNode;
                String referencePage = page.getAttribute("filename");

                // Determine the new image name from the file map
                String newImageName;
                if (reverse) {
                    // In reverse, we need to find the key whose value matches referencePage
                    newImageName = null;
                    for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                        if (entry.getValue().equals(referencePage)) {
                            newImageName = entry.getKey();
                            break;
                        }
                    }
                } else {
                    newImageName = fileMap.get(referencePage);
                }

                if (newImageName == null || newImageName.isEmpty()) {
                    continue;
                }

                transformedImageName = newImageName;
                page.setAttribute("filename", transformedImageName);
            }

            // Write XML back and rename file
            if (transformedImageName != null) {
                try (OutputStream os = Files.newOutputStream(filePath)) {
                    writeXml(doc, os);
                }
                // Rename the transcription file to match the new image name
                String newTranscriptionName = imageToTranscriptionName(transformedImageName);
                Path targetPath = bookDir.resolve(newTranscriptionName);
                if (!filePath.getFileName().toString().equals(newTranscriptionName)) {
                    Files.move(filePath, targetPath);
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            errors.add("Failed to read XML transcription. " + pageName);
        } catch (IOException e) {
            errors.add("Failed to process transcription [" + pageName + "]: " + e.getMessage());
        }
    }

    /**
     * Writes a DOM document to an output stream as XML.
     *
     * @param doc the DOM document
     * @param os  the output stream
     * @throws IOException if writing fails
     */
    private void writeXml(Document doc, OutputStream os) throws IOException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), new StreamResult(os));
        } catch (TransformerException e) {
            throw new IOException("Failed to write XML", e);
        }
    }
}
