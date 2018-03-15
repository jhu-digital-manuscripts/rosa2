package rosa.archive.core.util;

import org.apache.commons.lang3.StringUtils;
import rosa.archive.model.aor.AorLocation;

/**
 * This utility class must be able to parse IDs specified in 'internal references' in AOR2 data.
 *
 * In general, the ID pattern must be interpreted in some way.
 * ref="<book>:<page>:<annotation>"
 *
 * Can I use the following rules to parse these IDs?
 * 1. Attempt to match full string ID
 *      * This should all direct references to other annotations through the AOR corpus
 * 2. If no match found, split ID by delimiter (colon ':'). Generally, there should only be one or two parts that
 *    come out of this. A third part will fall out the reference has an bad annotation ID. Scrub all periods '.'
 *    from only the first part. <book> should now be usable as the archive book ID
 *      1. If three parts found, ??
 *      2. If two parts found. Split part 2 into two pieces: p2a[p2 up to last underscore (not including)] and
 *         p2b[p2 after last underscore (not including)]. 'p1' is the first part from the original ID
 *          1. If p2b is not empty, try a reformatted three part ID >> p1:p2a:p2b
 *          2. If no results found, match to page. <page> must be matched through the <book>/filemap.csv to get
 *             the final page name.
 *      3. If one part found, match reference to <book>.
 * 3. If no matches have been found yet, log and ignore this ID
 *
 */
public class AorIDUtil {
    private static final String DELIMITER = ":";

    /**
     * @param loc a location in AOR
     * @return corresponding "ID" following convention in AOR transcriber's manual
     */
    public static String getId(AorLocation loc) {
        if (StringUtils.isNotEmpty(loc.getAnnotation())) {
            return loc.getBook() + DELIMITER + loc.getPage() + DELIMITER + loc.getAnnotation();
        } else if (StringUtils.isNotEmpty(loc.getPage())) {
            return loc.getBook() + DELIMITER + loc.getPage();
        } else if (StringUtils.isNotEmpty(loc.getBook())) {
            return loc.getBook();
        } else if (StringUtils.isNotEmpty(loc.getCollection())) {
            return null;
        } else {
            return null;
        }
    }

    public static AorLocation parse(String id) {
        return null;
    }

}
