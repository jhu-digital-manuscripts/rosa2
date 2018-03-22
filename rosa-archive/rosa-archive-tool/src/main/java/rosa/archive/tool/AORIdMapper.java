package rosa.archive.tool;

import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ArchiveConstants;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.core.serialize.FileMapSerializer;
import rosa.archive.core.util.AnnotationLocationMapUtil;
import rosa.archive.model.ArchiveItemType;
import rosa.archive.model.FileMap;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.AorLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generate a mapping of annotation IDs to their location in the corpus.
 *  ID -> collection id, book id, page id, annotation id
 *
 * Also generate IDs for all books and pages.
 *
 * The mapped location information can later be used to construct IIIF
 * URIs.
 *
 * Store this mapping with the collection in 'annotation-map.csv
 */
public class AORIdMapper {
    private static final String DELIMITER = ":";
    private static final String FILEMAP_NAME = "filemap.csv";
    private static final AORAnnotatedPageSerializer aorSerializer = new AORAnnotatedPageSerializer();
    private static final FileMapSerializer fileMapSerializer = new FileMapSerializer();
    private static final Map<String, FileMap> map_cache = new ConcurrentHashMap<>();
    private static final FileMap emptyFileMap = new FileMap();

    private PrintStream report;
    private ByteStreamGroup base;

    private ArchiveNameParser nameParser;

    public AORIdMapper(ByteStreamGroup base, PrintStream report) {
        this.base = base;
        this.report = report;
        this.nameParser = new ArchiveNameParser();
    }

    /**
     * Load the specified collection and generate an annotation ID map.
     *
     * @param collection collection ByteStreamGroup
     */
    public void run(String collection) {
        if (!base.hasByteStreamGroup(collection)) {
            print("  No such collection found (" + collection + ")");
        }
        Map<String, AorLocation> result = new HashMap<>();
        ByteStreamGroup colBSG = base.getByteStreamGroup(collection);

        print("Writing '" + ArchiveConstants.ID_LOCATION_MAP + "' for collection: " + collection);
        result.put(collection, new AorLocation(collection, null, null, null));

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
            print("Errors encountered while writing '" + ArchiveConstants.ID_LOCATION_MAP + "' (" + collection + ")");
            errors.forEach(e -> print("  > " + e));
        }
    }

    private Map<String, AorLocation> doBook(String col, ByteStreamGroup book) throws IOException {
        Map<String, AorLocation> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        result.put(book.name(), new AorLocation(col, book.name(), null, null));

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
     * The mapping for the page will use the original file name, found from the book's page
     * file map, with the file extension removed.
     *
     * @param col collection name
     * @param book book name
     * @param page .
     * @return map of IDs to locations
     */
    private Map<String, AorLocation> doPage(String col, String book, AnnotatedPage page) {
        Map<String, AorLocation> result = new HashMap<>();

        FileMap pageMap = loadFileMap(book);
        String orig_page = pageMap.getMap().entrySet().stream()
                .filter(entry -> entry.getValue().equals(page.getPage()))
                .map(entry -> trimExt(entry.getKey()))
                .findFirst()
                .orElse(null);

        for (Entry<String, String> entry : pageMap.getMap().entrySet()) {
            if (entry.getValue().equals(page.getPage())) {
                orig_page = trimExt(entry.getKey());
                break;
            }
        }

        if (orig_page != null) {
            AorLocation loc = new AorLocation(col, book, page.getPage(), null);
            result.put(getId(book, orig_page, null), loc);
        }

        String p = trimExt(page.getPage());
        p = book + DELIMITER + p.substring(book.length() + 1);
        result.putIfAbsent(p, new AorLocation(col, book, page.getPage(), null));

        page.getAnnotations().stream()
                .map(Annotation::getId)
                .filter(id -> id != null && !id.isEmpty())
                .forEach(id -> result.put(id, new AorLocation(col, book, page.getPage(), id)));

        return result;
    }

    private FileMap loadFileMap(String parent) {
        if (map_cache.containsKey(parent)) {
            return map_cache.get(parent);
        }

        if (!base.hasByteStreamGroup(parent)) {
            return emptyFileMap;
        }
        ByteStreamGroup parentGroup = base.getByteStreamGroup(parent);
        if (!parentGroup.hasByteStream(FILEMAP_NAME)) {
            return emptyFileMap;
        }
        try (InputStream in = parentGroup.getByteStream(FILEMAP_NAME)) {
            FileMap m = fileMapSerializer.read(in, new ArrayList<>());
            map_cache.putIfAbsent(parent, m);
            return m;
        } catch (IOException e) {
            return emptyFileMap;
        }
    }

    private String getId(String book, String page, String anno) {
        if (StringUtils.isNotEmpty(anno)) {
            return book + DELIMITER + page + DELIMITER + anno;
        } else if (StringUtils.isNotEmpty(page)) {
            return book + DELIMITER + page;
        } else if (StringUtils.isNotEmpty(book)) {
            return book;
        } else {
            return null;
        }
    }

    private String trimExt(String name) {
        if (!name.contains(".")) {
            return name;
        }
        return name.substring(0, name.lastIndexOf('.'));
    }

    private void print(String message) {
        if (report == null || message == null) {
            return;
        }
        report.println(message);
    }

}
