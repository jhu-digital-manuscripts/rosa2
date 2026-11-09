package rosa.archive.core;

import rosa.archive.core.serialize.ArchiveReaders;
import rosa.archive.model.CropInfo;
import rosa.archive.model.CropData;
import rosa.archive.core.serialize.ArchiveWriters;
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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
    /** Collection checksum file: {@code {collection}collection.SHA1SUM}. */
    private static final String COLLECTION_SHA1SUM = "collection.SHA1SUM";
    private static final String CROP = ".crop.txt";
    private static final String CROPPED_DIR = "cropped";
    private static final String TRANSCRIPTION = ".transcription.xml";
    private static final String DESCRIPTION = ".description_";
    private static final String PERMISSION = ".permission_";
    private static final String PEOPLE = "people.csv";
    private static final String LOCATIONS = "locations.csv";
    private static final String BOOKS = "books.csv";
    private static final String ID_LOCATIONS = "id_locations.csv";
    private static final String HTML_ANNOS = "annos.jsonld";
    private static final String COLLECTION_CONFIG = "config.properties";
    private static final String MISSING_IMAGE = "missing_image.tif";
    private static final String XML_EXT = ".xml";
    private static final String HTML_EXT = ".html";

    /** Extensions of the metadata files a shallow copy keeps; images are everything else. */
    private static final Set<String> INCLUDE_EXTENSIONS = Set.of(
            "xml", "txt", "html", "csv", "jsonld", "properties"
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "tif", "tiff", "jpg", "jpeg", "png"
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
    public Path bookFile(String collectionId, String bookId, String fileName) {
        return archivePath.resolve(collectionId).resolve(bookId).resolve(fileName);
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
        return loadCollection(collectionId, new ArrayList<>());
    }

    @Override
    public BookCollection loadCollection(String collectionId, List<String> errors) throws IOException {
        Path collectionDir = archivePath.resolve(collectionId);
        if (!Files.isDirectory(collectionDir)) {
            throw new IOException("Collection not found in archive: " + collectionId);
        }

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
                ArchiveReaders.readSHA1Checksum(collectionDir.resolve(collectionId + COLLECTION_SHA1SUM), errors));

        // AoR reference sheets
        collection.setPeopleRef(
                ArchiveReaders.readPeopleReferenceSheet(collectionDir.resolve(PEOPLE), errors));
        collection.setLocationsRef(
                ArchiveReaders.readReferenceSheet(collectionDir.resolve(LOCATIONS), errors));
        collection.setBooksRef(
                ArchiveReaders.readBookReferenceSheet(collectionDir.resolve(BOOKS), errors));
        collection.setIdLocations(
                ArchiveReaders.readIdLocations(collectionDir.resolve(ID_LOCATIONS), errors));

        // HTML annotations
        collection.setHTMLAnnotations(
                ArchiveReaders.readHTMLAnnotations(collectionDir.resolve(HTML_ANNOS), errors));

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
        return loadBook(collection, bookId, new ArrayList<>());
    }

    @Override
    public Book loadBook(BookCollection collection, String bookId, List<String> errors) throws IOException {
        Path collectionDir = archivePath.resolve(collection.getId());
        if (!Files.isDirectory(collectionDir)) {
            throw new IOException("Collection not found in archive: " + collection.getId());
        }

        Path bookDir = collectionDir.resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + bookId);
        }

        Book book = new Book();
        book.setId(bookId);

        // Image lists and crop data
        book.setImages(
                ArchiveReaders.readImageList(bookDir.resolve(bookId + IMAGES), errors));
        book.setCroppedImages(
                ArchiveReaders.readImageList(bookDir.resolve(bookId + IMAGES_CROP), errors));
        book.setCropInfo(
                ArchiveReaders.readCropInfo(bookDir.resolve(bookId + CROP), errors));

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

            String descName = bookId + DESCRIPTION + lang + XML_EXT;
            rosa.archive.model.BookDescription desc = ArchiveReaders.readBookDescription(
                    bookDir.resolve(descName), errors);
            if (desc != null) {
                desc.setId(descName);
                desc.setLanguage(lang);
                book.addDescription(desc, lang);
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
        return check(collection, book, checkBits, true);
    }

    @Override
    public CheckResult check(BookCollection collection, Book book, boolean checkBits, boolean checkImages) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (book == null) {
            errors.add("Book is missing.");
            return CheckResult.of(errors, warnings);
        }

        // Verify image list exists and images are consistent with content
        checkImageList(book, checkImages, errors, warnings);

        // Verify the cropped image list refers to listed images
        checkCropList(book, warnings);

        // Verify metadata file exists
        checkMetadata(book, errors, warnings);

        // If checkBits, compute SHA1 for each file and compare against stored checksums
        if (checkBits) {
            checkBitIntegrity(collection, book, errors, warnings);
        }

        return CheckResult.of(errors, warnings);
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
                  .filter(source -> !isIgnoredPath(archivePath.relativize(source)))
                  .forEach(source -> {
                      Path relativePath = archivePath.relativize(source);
                      Path target = destination.resolve(relativePath);
                      try {
                          Files.createDirectories(target.getParent());
                          Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
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

        Path checksumFile = collectionDir.resolve(collectionId + COLLECTION_SHA1SUM);
        Map<String, String> existingChecksums = loadExistingChecksums(checksumFile, errors);
        long checksumLastModified = checksumFileLastModified(checksumFile);

        Map<String, String> updatedChecksums = new java.util.LinkedHashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionDir, Files::isRegularFile)) {
            List<Path> files = new ArrayList<>();
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                // Skip the checksum file itself and hidden files
                if (name.equals(collectionId + COLLECTION_SHA1SUM) || name.startsWith(".")) {
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

        // Keep the labels and missing markers of an existing list
        Map<String, BookImage> existing = new LinkedHashMap<>();
        ImageList existingList = ArchiveReaders.readImageList(imagesCsv, errors);
        if (existingList != null) {
            for (BookImage image : existingList) {
                existing.put(image.getId(), image);
            }
        }

        List<BookImage> entries = new ArrayList<>();
        for (Path imageFile : listImageFiles(bookDir)) {
            String id = imageFile.getFileName().toString();
            int[] dimensions = readImageDimensions(imageFile, errors);
            BookImage image = newImage(id, dimensions[0], dimensions[1], false);
            BookImage previous = existing.remove(id);
            if (previous != null) {
                image.setName(previous.getName());
                image.setAutoGeneratedName(previous.isAutoGeneratedName());
            }
            entries.add(image);
        }
        // Listed images that are not on disk stay listed as missing
        for (BookImage previous : existing.values()) {
            previous.setMissing(true);
            entries.add(previous);
        }
        entries.sort((a, b) -> a.getId().compareTo(b.getId()));

        ArchiveWriters.writeImageList(imagesCsv, entries);
    }

    @Override
    public void cropImages(String collectionId, String bookId, boolean force, List<String> errors) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        Path cropFile = bookDir.resolve(bookId + CROP);
        if (!Files.exists(cropFile)) {
            errors.add("Crop file not found: " + cropFile);
            return;
        }
        CropInfo cropInfo = ArchiveReaders.readCropInfo(cropFile, errors);

        Path croppedDir = bookDir.resolve(CROPPED_DIR);
        Files.createDirectories(croppedDir);

        List<CropData> crops = new ArrayList<>();
        cropInfo.forEach(crops::add);
        crops.sort((a, b) -> a.getId().compareTo(b.getId()));

        for (CropData crop : crops) {
            String filename = crop.getId();
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
                BufferedImage source = ImageIO.read(sourceFile.toFile());
                if (source == null) {
                    errors.add("Failed to read image for cropping: " + filename);
                    continue;
                }

                // Fractions of the width and height to remove from each side
                int width = source.getWidth();
                int height = source.getHeight();
                int left = (int) Math.round(crop.getLeft() * width);
                int right = width - (int) Math.round(crop.getRight() * width);
                int top = (int) Math.round(crop.getTop() * height);
                int bottom = height - (int) Math.round(crop.getBottom() * height);

                if (right - left <= 0 || bottom - top <= 0) {
                    errors.add("Invalid crop region for [" + filename + "]: " + crop);
                    continue;
                }

                BufferedImage cropped = source.getSubimage(left, top, right - left, bottom - top);
                String ext = getImageExtension(filename);
                ImageIO.write(cropped, ext, croppedFile.toFile());
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

        Path croppedDir = bookDir.resolve(CROPPED_DIR);
        if (!Files.isDirectory(croppedDir)) {
            errors.add("Cropped directory not found: " + croppedDir);
            return;
        }

        Map<String, Path> croppedFiles = new java.util.TreeMap<>();
        for (Path imageFile : listImageFiles(croppedDir)) {
            croppedFiles.put(imageFile.getFileName().toString(), imageFile);
        }

        // Follow the image list's order and labels; cropped files it does not list come last
        List<BookImage> entries = new ArrayList<>();
        ImageList images = ArchiveReaders.readImageList(bookDir.resolve(bookId + IMAGES), errors);
        if (images != null) {
            for (BookImage image : images) {
                Path croppedFile = croppedFiles.remove(image.getId());
                if (croppedFile == null) {
                    continue;
                }
                int[] dimensions = readImageDimensions(croppedFile, errors);
                BookImage entry = newImage(image.getId(), dimensions[0], dimensions[1], false);
                entry.setName(image.getName());
                entry.setAutoGeneratedName(image.isAutoGeneratedName());
                entries.add(entry);
            }
        }
        for (Map.Entry<String, Path> extra : croppedFiles.entrySet()) {
            int[] dimensions = readImageDimensions(extra.getValue(), errors);
            entries.add(newImage(extra.getKey(), dimensions[0], dimensions[1], false));
        }

        ArchiveWriters.writeImageList(cropCsv, entries);
    }

    @Override
    public void generateTEITranscriptions(String collectionId, String bookId, boolean force, Charset inputCharset,
                                          List<String> errors, List<String> warnings) throws IOException {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            throw new IOException("Book not found: " + collectionId + "/" + bookId);
        }

        // Find per-page text transcription files: BookId.transcription.NNNr.txt
        List<Path> textFiles = listPerPageTranscriptionFiles(bookDir, bookId);
        if (textFiles.isEmpty()) {
            errors.add("No per-page transcription files (" + bookId + ".transcription.*.txt) found in "
                    + collectionId + "/" + bookId);
            return;
        }

        // Convert all text files into a single TEI XML, never silently replacing a hand-authored one
        Path outputFile = bookDir.resolve(bookId + TRANSCRIPTION);
        if (Files.exists(outputFile) && !force) {
            errors.add("Transcription already exists and would be overwritten: " + outputFile.getFileName()
                    + " (use --force to replace it)");
            return;
        }
        var converter = new TranscriptionConverter();
        converter.convert(textFiles, outputFile, inputCharset);

        warnings.addAll(converter.getWarnings());
        errors.addAll(converter.getErrors());
    }

    private List<Path> listPerPageTranscriptionFiles(Path bookDir, String bookId) throws IOException {
        String prefix = bookId + ".transcription.";
        String suffix = ".txt";
        try (var stream = Files.list(bookDir)) {
            return stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(suffix);
                    })
                    .sorted()
                    .toList();
        }
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
        if (name.endsWith(SHA1SUM)) {
            return true;
        }
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        String ext = name.substring(dotIndex + 1).toLowerCase();
        return INCLUDE_EXTENSIONS.contains(ext);
    }

    /**
     * Whether a path relative to the archive lies in a directory the tool ignores
     * (a name containing {@code .ignore}, or the legacy {@code biblehistoriale} collection).
     */
    private static boolean isIgnoredPath(Path relative) {
        for (int i = 0; i < relative.getNameCount() - 1; i++) {
            String dir = relative.getName(i).toString();
            if (dir.contains(".ignore") || (i == 0 && dir.equals("biblehistoriale"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the given path is an image file (tif, tiff, jpg, jpeg or png, case-insensitive).
     */
    private boolean isImageFile(Path file) {
        String name = file.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        return Files.isRegularFile(file) && dotIndex > 0
                && IMAGE_EXTENSIONS.contains(name.substring(dotIndex + 1).toLowerCase());
    }

    /** A new image list entry with the location, role and default name derived from its id. */
    private static BookImage newImage(String id, int width, int height, boolean missing) {
        BookImage image = new BookImage(id, width, height, missing);
        image.setLocation(PARSER.location(id));
        image.setRole(PARSER.role(id));
        image.setName(PARSER.shortName(id));
        return image;
    }

    /** Image files in a directory, sorted by name. */
    private List<Path> listImageFiles(Path dir) throws IOException {
        List<Path> imageFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, this::isImageFile)) {
            for (Path entry : stream) {
                imageFiles.add(entry);
            }
        }
        imageFiles.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        return imageFiles;
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
    private void checkImageList(Book book, boolean checkFiles, List<String> errors, List<String> warnings) {
        ImageList images = book.getImages();
        if (images == null || images.getImages() == null) {
            errors.add("Image list is missing for book [" + book.getId() + "].");
            return;
        }
        if (!checkFiles) {
            for (BookImage image : images) {
                if (image.getId() == null || image.getId().isBlank()) {
                    errors.add("Image ID not set in book [" + book.getId() + "].");
                }
            }
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
     * Warns about cropped images that are not in the image list. Canvases follow the image
     * list, so such entries are never shown (rose/Bodmer79 has two).
     */
    private void checkCropList(Book book, List<String> warnings) {
        ImageList cropped = book.getCroppedImages();
        ImageList images = book.getImages();
        if (cropped == null || images == null) {
            return;
        }
        Set<String> ids = new java.util.HashSet<>();
        for (BookImage image : images) {
            ids.add(image.getId());
        }
        for (BookImage image : cropped) {
            if (!ids.contains(image.getId())) {
                warnings.add("Cropped image [" + image.getId() + "] is not in the image list of book ["
                        + book.getId() + "] and is never shown.");
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

        Set<String> onDisk = new java.util.HashSet<>(Arrays.asList(content));
        for (String fileName : storedChecksums.checksums().keySet()) {
            if (!onDisk.contains(fileName)) {
                errors.add("File listed in SHA1SUM not found on disk: " + fileName);
            }
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
}
