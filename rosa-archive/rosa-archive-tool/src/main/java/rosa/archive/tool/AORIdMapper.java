package rosa.archive.tool;

import rosa.archive.core.ArchiveConstants;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.core.util.AnnotationLocationMapUtil;
import rosa.archive.model.ArchiveItemType;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.AorLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate a mapping of annotation IDs to their location in the corpus.
 *  ID -> collection id, book id, page id, annotation id
 *
 * Store this mapping with the collection in 'annotation-map.csv
 */
public class AORIdMapper {

    private Store store;
    private PrintStream report;
    private ByteStreamGroup base;

    private ArchiveNameParser nameParser;
    private AORAnnotatedPageSerializer aorSerializer;

    public AORIdMapper(Store store, ByteStreamGroup base, PrintStream report) {
        this.store = store;
        this.base = base;
        this.report = report;
        this.nameParser = new ArchiveNameParser();
        this.aorSerializer = new AORAnnotatedPageSerializer();
    }

    /**
     * Load the specified collection and generate an annotation ID map.
     *
     * @param collection collection ByteStreamGroup
     */
    public void run(String collection) {
        if (!base.hasByteStream(collection)) {
            print("  No such collection found (" + collection + ")");
        }
        Map<String, AorLocation> result = new HashMap<>();
        ByteStreamGroup colBSG = base.getByteStreamGroup(collection);

        try {
            for (String book : colBSG.listByteStreamGroupNames()) {
                result.putAll(doBook(collection, colBSG.getByteStreamGroup(book)));
            }
        } catch (IOException e) {
            print("Failed to get books for collection (" + collection + ")");
        }

        List<String> errors = new ArrayList<>();
        AnnotationLocationMapUtil.write(colBSG, result, errors);
        if (!errors.isEmpty()) {
            print("Errors encountered while writing '" + ArchiveConstants.ANNOTATION_LINK_MAP + "' (" + collection + ")");
            errors.forEach(e -> print("  > " + e));
        }
    }

    private Map<String, AorLocation> doBook(String col, ByteStreamGroup book) throws IOException {
        Map<String, AorLocation> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        book.listByteStreamNames().stream()
                .filter(file -> nameParser.getArchiveItemType(file) == ArchiveItemType.TRANSCRIPTION_AOR)
                .forEach(aor -> {
                    errors.clear();
                    try (InputStream pageIS = book.getByteStream(aor)) {
                        AnnotatedPage page = aorSerializer.read(pageIS, errors);
                        if (!errors.isEmpty()) {
                            print("Errors found while reading page (" + aor + ")");
                            errors.forEach(e -> print("  > " + e));
                            return;
                        }

                        result.putAll(doPage(col, book.name(), page));
                    } catch (IOException e) {
                        print("Failed to open page (" + aor + ")");
                    }
                });

        return result;
    }

    /**
     * For each page, add a mapping for the page and a mapping for any annotations on the page.
     *
     * @param col collection name
     * @param book book name
     * @param page .
     * @return map of IDs to locations
     */
    private Map<String, AorLocation> doPage(String col, String book, AnnotatedPage page) {
        Map<String, AorLocation> result = new HashMap<>();

        page.getAnnotations().stream()
                .filter(a -> a.getId() != null && !a.getId().isEmpty())
                .forEach();

        return result;
    }

    private void print(String message) {
        if (report == null || message == null) {
            return;
        }
        report.println(message);
    }

}
