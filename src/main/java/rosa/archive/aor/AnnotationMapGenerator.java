package rosa.archive.aor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.aor.AorLocation;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a mapping of annotation IDs to their location within a collection.
 *
 * <p>For each collection, book, page, and annotation, a mapping entry is created
 * in the output CSV ({@code id_locations.csv}). This mapping allows IIIF URIs to
 * be constructed from annotation identifiers.</p>
 *
 * <p>When a {@code filemap.csv} exists for a book, original page names are resolved
 * from it. Otherwise, page labels are derived from the transcription filename.</p>
 */
public final class AnnotationMapGenerator {

    private static final String DELIMITER = ":";
    private static final String FILEMAP_NAME = "filemap.csv";
    private static final ArchiveNameParser NAME_PARSER = new ArchiveNameParser();

    private final Path archivePath;

    /**
     * Creates an AnnotationMapGenerator rooted at the given archive path.
     *
     * @param archivePath the root directory of the archive
     */
    public AnnotationMapGenerator(Path archivePath) {
        this.archivePath = archivePath;
    }

    /**
     * Generates {@code id_locations.csv} for the given collection.
     *
     * <p>The generated CSV contains one row per mapping entry with columns:
     * {@code id,collection,book,page,annotation}. Entries are written for the
     * collection itself, each book, each page, and each annotation that has
     * an {@code id} attribute.</p>
     *
     * @param collectionId the collection to process
     * @return list of error messages (empty on success)
     */
    public List<String> run(String collectionId) {
        List<String> errors = new ArrayList<>();
        Path collectionDir = archivePath.resolve(collectionId);

        if (!Files.isDirectory(collectionDir)) {
            errors.add("Collection directory not found: " + collectionId);
            return errors;
        }

        Map<String, AorLocation> result = new HashMap<>();

        // Collection-level entry
        result.put(collectionId, new AorLocation(collectionId, null, null, null));

        // Process each book in the collection
        List<String> books = listBookDirectories(collectionDir, errors);
        for (String bookId : books) {
            Path bookDir = collectionDir.resolve(bookId);
            processBook(collectionId, bookId, bookDir, result, errors);
        }

        // Write id_locations.csv to the collection directory
        writeLocationMap(collectionDir, result, errors);

        return errors;
    }

    /**
     * Generates {@code id_locations.csv} for the given collection, writing output
     * to the specified directory.
     *
     * @param collectionId the collection to process
     * @param outputDir    the directory to write the output CSV
     * @param errors       list to collect error messages
     */
    public void generate(String collectionId, Path outputDir, List<String> errors) {
        Path collectionDir = archivePath.resolve(collectionId);

        if (!Files.isDirectory(collectionDir)) {
            errors.add("Collection directory not found: " + collectionId);
            return;
        }

        Map<String, AorLocation> result = new HashMap<>();

        // Collection-level entry
        result.put(collectionId, new AorLocation(collectionId, null, null, null));

        // Process each book in the collection
        List<String> books = listBookDirectories(collectionDir, errors);
        for (String bookId : books) {
            Path bookDir = collectionDir.resolve(bookId);
            processBook(collectionId, bookId, bookDir, result, errors);
        }

        // Write id_locations.csv to the output directory
        writeLocationMap(outputDir, result, errors);
    }

    private void processBook(String collectionId, String bookId, Path bookDir,
                             Map<String, AorLocation> result, List<String> errors) {
        // Book-level entry
        result.put(bookId, new AorLocation(collectionId, bookId, null, null));

        // Load filemap if available
        Map<String, String> fileMap = loadFileMap(bookDir, errors);

        // Find all AoR transcription XML files
        List<String> transcriptions = listAorTranscriptions(bookDir, errors);

        for (String transcriptionFile : transcriptions) {
            processTranscription(collectionId, bookId, bookDir, transcriptionFile, fileMap, result, errors);
        }
    }

    private void processTranscription(String collectionId, String bookId, Path bookDir,
                                      String transcriptionFile, Map<String, String> fileMap,
                                      Map<String, AorLocation> result, List<String> errors) {
        Path filePath = bookDir.resolve(transcriptionFile);

        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            try (InputStream in = Files.newInputStream(filePath)) {
                doc = builder.parse(in);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            errors.add("Failed to parse transcription [" + transcriptionFile + "]: " + e.getMessage());
            return;
        }

        // Extract page filename from the <page> element
        String pageImageName = extractPageFilename(doc);
        if (pageImageName == null || pageImageName.isBlank()) {
            errors.add("No page filename found in transcription: " + transcriptionFile);
            return;
        }

        // Resolve original page name via filemap
        String pageLabel = resolvePageLabel(bookId, pageImageName, fileMap);

        // Page-level entry: "bookId:pageLabel"
        String pageId = bookId + DELIMITER + pageLabel;
        result.putIfAbsent(pageId, new AorLocation(collectionId, bookId, pageLabel, null));

        // Also add entry using original filename mapping if filemap provides a different mapping
        addOriginalPageEntry(collectionId, bookId, pageImageName, fileMap, result);

        // Process annotation elements with id attributes
        NodeList annotationNodes = doc.getElementsByTagName("annotation");
        if (annotationNodes.getLength() > 0) {
            Element annotationEl = (Element) annotationNodes.item(0);
            extractAnnotationIds(collectionId, bookId, pageLabel, annotationEl, result);
        }
    }

    private void addOriginalPageEntry(String collectionId, String bookId, String pageImageName,
                                      Map<String, String> fileMap, Map<String, AorLocation> result) {
        if (fileMap.isEmpty()) {
            return;
        }

        // Find the original filename that maps to this page image
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            if (entry.getValue().equals(pageImageName)) {
                String origLabel = getPageLabel(bookId, entry.getKey());
                String origPageId = bookId + DELIMITER + origLabel;
                String currentLabel = getPageLabel(bookId, pageImageName);
                result.putIfAbsent(origPageId,
                        new AorLocation(collectionId, bookId, currentLabel, null));
                break;
            }
        }
    }

    private void extractAnnotationIds(String collectionId, String bookId, String pageLabel,
                                      Element annotationEl, Map<String, AorLocation> result) {
        // Check all child elements of <annotation> for id attributes
        String[] annotationTypes = {
                "marginalia", "underline", "mark", "symbol", "drawing",
                "errata", "numeral", "graph", "table", "calculation", "physical_link"
        };

        for (String type : annotationTypes) {
            NodeList elements = annotationEl.getElementsByTagName(type);
            for (int i = 0; i < elements.getLength(); i++) {
                Element el = (Element) elements.item(i);
                String id = el.getAttribute("id");
                if (id != null && !id.isBlank()) {
                    result.put(id, new AorLocation(collectionId, bookId, pageLabel, id));
                }
            }
        }
    }

    private String extractPageFilename(Document doc) {
        NodeList pageEls = doc.getElementsByTagName("page");
        if (pageEls.getLength() > 0) {
            Element pageEl = (Element) pageEls.item(0);
            return pageEl.getAttribute("filename");
        }
        return null;
    }

    private String resolvePageLabel(String bookId, String pageImageName, Map<String, String> fileMap) {
        // If filemap exists, try to find original name
        if (!fileMap.isEmpty()) {
            for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                if (entry.getValue().equals(pageImageName)) {
                    return getPageLabel(bookId, entry.getKey());
                }
            }
        }
        return getPageLabel(bookId, pageImageName);
    }

    private String getPageLabel(String bookId, String page) {
        String[] parts = page.split("\\.");
        if (parts.length == 2) {
            // Original image name format: "name.ext" — remove the extension
            return parts[0];
        }
        return NAME_PARSER.shortName(page);
    }

    private Map<String, String> loadFileMap(Path bookDir, List<String> errors) {
        Path fileMapPath = bookDir.resolve(FILEMAP_NAME);
        Map<String, String> map = new LinkedHashMap<>();

        if (!Files.isRegularFile(fileMapPath)) {
            return map;
        }

        try {
            List<String> lines = Files.readAllLines(fileMapPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf('#'));
                }
                String[] parts = line.split(",", 2);
                if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            errors.add("Failed to read filemap for book [" + bookDir.getFileName() + "]: " + e.getMessage());
        }

        return map;
    }

    private List<String> listBookDirectories(Path collectionDir, List<String> errors) {
        List<String> books = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionDir, Files::isDirectory)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (!name.contains(".ignore")) {
                    books.add(name);
                }
            }
        } catch (IOException e) {
            errors.add("Failed to list books in collection: " + e.getMessage());
        }
        books.sort(String::compareTo);
        return books;
    }

    private List<String> listAorTranscriptions(Path bookDir, List<String> errors) {
        List<String> transcriptions = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, Files::isRegularFile)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (NAME_PARSER.isAorTranscription(name)) {
                    transcriptions.add(name);
                }
            }
        } catch (IOException e) {
            errors.add("Failed to list transcription files in [" + bookDir.getFileName() + "]: " + e.getMessage());
        }
        transcriptions.sort(String::compareTo);
        return transcriptions;
    }

    private void writeLocationMap(Path outputDir, Map<String, AorLocation> map, List<String> errors) {
        Path outputFile = outputDir.resolve("id_locations.csv");

        List<Map.Entry<String, AorLocation>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort(Comparator.comparing(Map.Entry::getKey));

        var sb = new StringBuilder();
        for (Map.Entry<String, AorLocation> entry : sorted) {
            AorLocation loc = entry.getValue();
            sb.append(entry.getKey());
            sb.append(',').append(loc.getCollection());
            sb.append(',');
            if (loc.getBook() != null && !loc.getBook().isBlank()) {
                sb.append(loc.getBook());
            }
            sb.append(',');
            if (loc.getPage() != null && !loc.getPage().isBlank()) {
                sb.append(loc.getPage());
            }
            sb.append(',');
            if (loc.getAnnotation() != null && !loc.getAnnotation().isBlank()) {
                sb.append(loc.getAnnotation());
            }
            sb.append('\n');
        }

        try {
            Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            errors.add("Failed to write id_locations.csv: " + e.getMessage());
        }
    }
}
