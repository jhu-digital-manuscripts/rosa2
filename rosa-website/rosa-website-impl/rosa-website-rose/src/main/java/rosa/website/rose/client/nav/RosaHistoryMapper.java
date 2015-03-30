package rosa.website.rose.client.nav;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.rose.client.WebsiteConfig;
import rosa.website.core.client.mvp.BaseHistoryMapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class RosaHistoryMapper extends BaseHistoryMapper {
    private static final Logger logger = Logger.getLogger(RosaHistoryMapper.class.toString());

    private final Set<String> htmlPlaces;
    private final Set<String> csvPlaces;

    /**
     * Create a new history mapper.
     *
     * @param defaultHistoryMapper fallback history handler
     */
    public RosaHistoryMapper(PlaceHistoryMapper defaultHistoryMapper) {
        super(defaultHistoryMapper,
                WebsiteConfig.INSTANCE.collection(),
                WebsiteConfig.INSTANCE.historyDelimiter()
        );

        this.htmlPlaces = new HashSet<>(Arrays.asList(WebsiteConfig.INSTANCE.htmlHistory().split(",")));
        this.csvPlaces = new HashSet<>(Arrays.asList(WebsiteConfig.INSTANCE.csvHistory().split(",")));
    }

    @Override
    public Place getPlace(String token) {

        String[] parts = token.split(DELIMITER);
        if (htmlPlaces.contains(parts[0])) {
            logger.fine("Found history token for HTMLPlace.");
            return new HTMLPlace(collection, parts[0]);
        } else if (csvPlaces.contains(parts[0])) {
            logger.fine("Found history token for CSVDataPlace.");
            return new CSVDataPlace(collection, parts[0]);
        }

        return super.getPlace(token);
    }

    @Override
    public String getToken(Place place) {
        return super.getToken(place);
    }
}
