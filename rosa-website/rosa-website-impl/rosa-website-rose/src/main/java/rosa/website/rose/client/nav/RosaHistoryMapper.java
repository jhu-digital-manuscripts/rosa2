package rosa.website.rose.client.nav;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.rose.client.RosaHistoryConfig;
import rosa.website.rose.client.WebsiteConfig;
import rosa.website.core.client.mvp.BaseHistoryMapper;

public class RosaHistoryMapper extends BaseHistoryMapper {

    /**
     * @param defaultHistoryMapper .
     * @param clientFactory .
     */
    public RosaHistoryMapper(PlaceHistoryMapper defaultHistoryMapper, ClientFactory clientFactory) {
        super(defaultHistoryMapper,
                clientFactory,
                WebsiteConfig.INSTANCE.historyDelimiter());
    }

    @Override
    public Place getPlace(String token) {

        String[] parts = token.split(DELIMITER);
        if (RosaHistoryConfig.isValidHtmlPage(parts[0])) {
            return new HTMLPlace(parts[0]);
        } else if (RosaHistoryConfig.isValidCsvPage(parts[0])) {
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
