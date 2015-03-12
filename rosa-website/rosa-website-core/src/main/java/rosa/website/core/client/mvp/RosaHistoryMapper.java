package rosa.website.core.client.mvp;

import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.place.HTMLPlace;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Core history mapper. This class maps history tokens to appropriate Places.
 */
public class RosaHistoryMapper implements PlaceHistoryMapper {
    private static final Logger logger = Logger.getLogger(RosaHistoryMapper.class.toString());
    private static final String COLON = ":";

    private final Set<String> htmlPages;
    private String DELIMITER = ";";

    /**
     * If this mapper cannot map tokens to places, fall back to the default mapper.
     */
    private final PlaceHistoryMapper defaultHistoryMapper;

    /**
     * Create a new RosaHistoryMapper
     *
     * @param defaultHistoryMapper fallback mapper
     * @param htmlPages names of the static HTML pages
     */
    public RosaHistoryMapper(PlaceHistoryMapper defaultHistoryMapper, String[] htmlPages) {
        this.defaultHistoryMapper = defaultHistoryMapper;
        this.htmlPages = new HashSet<>();

        setHtmlPages(htmlPages);
    }

    /**
     * Create a new RosaHistoryMapper
     *
     * @param htmlPages names of the static HTML pages
     */
    public RosaHistoryMapper(String[] htmlPages) {
        this(null, htmlPages);
    }

    public void setHtmlPages(String ... pages) {
        if (pages == null || pages.length == 0) {
            return;
        }
        htmlPages.addAll(Arrays.asList(pages));
    }

    public void setDelimiter(String delimiter) {
        if (delimiter != null && !delimiter.isEmpty()) {
            this.DELIMITER = delimiter;
        }
    }

    /**
     * Get the Place associated with a given history token. This uses the history scheme
     * from the Roman de la Rose website, which includes the place. If any state data
     * existed, it was delimited by a semi-colon (;). Since the history token is taken from
     * the URL fragment, it is properly URL encoded.
     *
     * Example: {@code #home} or {@code #search;ALL;search%20token;0}
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
