package rosa.website.rose.client.nav;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.rose.client.RosaHistoryConfig;
import rosa.website.rose.client.WebsiteConfig;
import rosa.website.core.client.mvp.BaseHistoryMapper;

import java.util.logging.Logger;

public class RosaHistoryMapper extends BaseHistoryMapper {
    private static final Logger logger = Logger.getLogger(RosaHistoryMapper.class.toString());

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
    }

    @Override
    public Place getPlace(String token) {

        String[] parts = token.split(DELIMITER);
        if (RosaHistoryConfig.isValidHtmlPage(parts[0])) {
            logger.fine("Found history token for HTMLPlace.");
            return new HTMLPlace(parts[0]);
        } else if (RosaHistoryConfig.isValidCsvPage(parts[0])) {
            logger.fine("Found history token for CSVDataPlace.");
            return new CSVDataPlace(parts[0]);
        }

        return super.getPlace(token);
    }

    @Override
    public String getToken(Place place) {

        if (place instanceof HTMLPlace) {
            HTMLPlace htmlPlace = (HTMLPlace) place;
            return htmlPlace.getName();
        } else if (place instanceof CSVDataPlace) {
            CSVDataPlace csvPlace = (CSVDataPlace) place;
            return csvPlace.getName();
        }

        return super.getToken(place);
    }
}
