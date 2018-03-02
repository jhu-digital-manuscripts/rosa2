package rosa.archive.tool;

import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.Store;

import java.io.PrintStream;

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

    public AORIdMapper(Store store, ByteStreamGroup base, PrintStream report) {
        this.store = store;
        this.base = base;
        this.report = report;
    }

    /**
     * Load the specified collection and generate an annotation ID map.
     *
     * @param collection collection ByteStreamGroup
     */
    public void run(String collection) {
        if (!base.hasByteStream(collection)) {
            report.println("  No such collection found (" + collection + ")");
        }
        ByteStreamGroup colBSG = base.getByteStreamGroup(collection);


    }

}
