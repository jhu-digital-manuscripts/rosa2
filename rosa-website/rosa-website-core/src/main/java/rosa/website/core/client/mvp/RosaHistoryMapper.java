package rosa.website.core.client.mvp;

import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Core history mapper. This class maps history tokens to appropriate Places.
 *
 * The history tokens recognized by this class can follow the history scheme from
 * the Roman de la Rose site, where the token consists of the name of the place. Any
 * state information about that place is appended after a semi-colon. For
 * example: {@code #home} or {@code #search;ALL;asdf;0}. Some places allow for more
 * than one name to be mapped to them.
 *
 * If the Roman de la Rose history scheme fails to return any place results, then
 * the default GWT generated history scheme is investigated. This scheme has the
 * Place class name as the prefix of the history token, which is delimited with a
 * colon instead of a semi-colon.
 *
 * For both, state information is determined by each Place's Tokenizer.
 */
public class RosaHistoryMapper implements PlaceHistoryMapper {
    private static final Logger logger = Logger.getLogger(RosaHistoryMapper.class.toString());
    private static final String COLON = ":";

    private final String collection;
    private final Set<String> htmlPages;
    /** Map associating history token name to CSV name */
    private final Map<String, String> csvPages;
    private String DELIMITER = ";";

    /**
     * If this mapper cannot map tokens to places, fall back to the default mapper.
     */
    private final PlaceHistoryMapper defaultHistoryMapper;

    /**
     * Create a new RosaHistoryMapper.
     *
     * @param defaultHistoryMapper fallback mapper
     * @param collection book/manuscript collection to act on
     * @param htmlPages names of static HTML pages
     * @param csvPages names of CSV data pages
     */
    public RosaHistoryMapper(PlaceHistoryMapper defaultHistoryMapper, String collection, String[] htmlPages,
                             Map<String, String> csvPages) {
        this.defaultHistoryMapper = defaultHistoryMapper;
        this.collection = collection;
        this.htmlPages = new HashSet<>();
        this.csvPages = new HashMap<>();

        setHtmlPages(htmlPages);
        setCsvPages(csvPages);
    }

    /**
     * Create a new RosaHistoryMapper
     *
     * @param defaultHistoryMapper fallback mapper
     * @param collection book/manuscript collection to act on
     * @param htmlPages names of the static HTML pages
     */
    public RosaHistoryMapper(PlaceHistoryMapper defaultHistoryMapper, String collection, String[] htmlPages) {
        this(defaultHistoryMapper, collection, htmlPages, null);
    }

    /**
     * Create a new RosaHistoryMapper
     *
     * @param collection book/manuscript collection to act on
     * @param htmlPages names of the static HTML pages
     */
    public RosaHistoryMapper(String collection, String[] htmlPages) {
        this(null, collection, htmlPages);
    }

    public void setHtmlPages(String ... pages) {
        if (pages == null || pages.length == 0) {
            return;
        } else if (htmlPages.size() > 0) {
            htmlPages.clear();
        }
        htmlPages.addAll(Arrays.asList(pages));
    }

    public void setCsvPages(Map<String, String> pages) {
        if (pages == null || pages.size() == 0) {
            return;
        } else if (csvPages.size() > 0) {
            csvPages.clear();
        }
        csvPages.putAll(pages);
    }

    public void setDelimiter(String delimiter) {
        if (delimiter != null && !delimiter.isEmpty()) {
            this.DELIMITER = delimiter;
        }
    }

    /**
     * Get the Place associated with a given history token. Multiple place names
     * can map to HTMLPlace or CSVPlace. If none of these are detected, the
     * token will be parsed according to the default GWT generated history scheme.
     *
     * @param token history token
     * @return the Place associated with the token, or NULL if there is no such place
     */
    @Override
    public Place getPlace(String token) {
        logger.fine("Getting place for history token: [" + token + "]");
        if (token == null) {
            return null;
        }

        token = URL.decode(token);

        // Put any new core Places here
        String[] token_parts = token.split(DELIMITER);
        if (htmlPages.contains(token_parts[0])) {
            logger.fine("Encountered history token for static HTML page. [" + token_parts[0] + "]");
            return new HTMLPlace(token_parts[0]);
        } else if (csvPages.containsKey(token_parts[0])) {
            logger.fine("Encountered history token for CSV data page. [" + token_parts[0] + "]");
            return new CSVDataPlace(csvPages.get(token_parts[0]), collection);
        }

        // If token not already recognized, revert back to the default history token scheme
        // and try the default history mapper
        if (!token.endsWith(DELIMITER)) {
            token = token.concat(DELIMITER).replaceAll(DELIMITER, COLON);
        }

        return defaultHistoryMapper == null ? null : defaultHistoryMapper.getPlace(token);
    }

    @Override
    public String getToken(Place place) {
        String token = defaultHistoryMapper.getToken(place).replaceAll(COLON, DELIMITER);
        if (token != null && token.endsWith(DELIMITER)) {
            token = token.substring(0, token.length() - 1);
        }
        return token;
    }
}
