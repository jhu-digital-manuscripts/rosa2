package rosa.website.test.client.nav;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.mvp.BaseHistoryMapper;
import rosa.website.core.client.place.HTMLPlace;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class RosaHistoryMapper implements BaseHistoryMapper, PlaceHistoryMapper {
    private static final Logger logger = Logger.getLogger(RosaHistoryMapper.class.toString());
    private static final String COLON = ":";
    private static final String DELIMITER = ";";

    private final Set<String> htmlPages;

    private final PlaceHistoryMapper defaultHistoryMapper;

    public RosaHistoryMapper(PlaceHistoryMapper defaultHistoryMapper, String[] htmlPages) {
        this.defaultHistoryMapper = defaultHistoryMapper;
        this.htmlPages = new HashSet<>(Arrays.asList(htmlPages));
    }

    @Override
    public Place getPlace(String token) {
        logger.fine("Getting place for history token: [" + token + "]");
        if (token == null) {
            return null;
        }

        String[] token_parts = token.split(DELIMITER);
        if (htmlPages.contains(token_parts[0])) {
            logger.fine("Encountered history token for static HTML page. [" + token_parts[0] + "]");
            return new HTMLPlace(token_parts[0]);
        }

        // If token not already recognized, try the generated history mapper
        if (!token.endsWith(DELIMITER)) {
            token = token.concat(DELIMITER).replaceAll(DELIMITER, COLON);
        }
        return defaultHistoryMapper.getPlace(token);
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
