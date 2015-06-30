package rosa.website.core.client.mvp;

import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.BookViewerPlace;

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
public abstract class BaseHistoryMapper implements PlaceHistoryMapper {
    private static final Logger logger = Logger.getLogger(BaseHistoryMapper.class.toString());
    private static final String COLON = ":";

    protected final String collection;
    protected final String DELIMITER;

    /**
     * If this mapper cannot map tokens to places, fall back to the default mapper.
     */
    private final PlaceHistoryMapper defaultHistoryMapper;

    /**
     * Create a new history mapper.
     *
     * @param defaultHistoryMapper fallback history handler
     * @param clientFactory .
     * @param DELIMITER delimiter character used in history tokens
     */
    public BaseHistoryMapper(PlaceHistoryMapper defaultHistoryMapper, ClientFactory clientFactory, String DELIMITER) {
        this.DELIMITER = DELIMITER;
        this.defaultHistoryMapper = defaultHistoryMapper;

        this.collection = clientFactory.context().getCollection();
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

        String[] parts = token.split(DELIMITER);
        if (parts.length == 2 && (parts[0].equals("browse") || parts[0].equals("read"))) {
            String frag = parts[1];

            if (frag.contains(".")) {
                int index = frag.indexOf('.');
                return new BookViewerPlace(parts[0], frag.substring(0, index), parts[1]);
            } else {
                return new BookViewerPlace(parts[0], parts[1]);
            }
        }

        // If token not already recognized, revert back to the default history token scheme
        // and try the default history mapper

        logger.fine("Checking default history mapper.");
        return defaultHistoryMapper == null ? null : defaultHistoryMapper.getPlace(token.replaceAll(DELIMITER, COLON));
    }

    @Override
    public String getToken(Place place) {
        if (place instanceof BookViewerPlace) {
            BookViewerPlace b = (BookViewerPlace) place;
            String token = b.getType() + DELIMITER;
            if (b.getPage() == null || b.getPage().isEmpty()) {
                token = token.concat(b.getBook());
            } else {
                token = token.concat(b.getPage());
            }

            return token;
        }

        String token = defaultHistoryMapper.getToken(place).replaceAll(COLON, DELIMITER);
        if (token != null && token.endsWith(DELIMITER)) {
            token = token.substring(0, token.length() - 1);
        }
        return token;
    }
}
