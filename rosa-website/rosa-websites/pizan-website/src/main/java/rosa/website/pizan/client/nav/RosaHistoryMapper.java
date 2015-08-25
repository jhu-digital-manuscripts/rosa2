package rosa.website.pizan.client.nav;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.core.client.Analytics;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.mvp.BaseHistoryMapper;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.pizan.client.HistoryConfig;
import rosa.website.pizan.client.PizanAnalytics;
import rosa.website.pizan.client.WebsiteConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RosaHistoryMapper extends BaseHistoryMapper {

    private final Analytics ANALYTICS = PizanAnalytics.INSTANCE;

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

        if (WebsiteConfig.INSTANCE.trackAnalytics()) {
            triggerAnalytics(token);
        }

        if (HistoryConfig.isValidHtmlPage(parts[0])) {
            return new HTMLPlace(parts[0]);
        } else if (HistoryConfig.isValidCsvPage(parts[0])) {
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

    private void triggerAnalytics(String historyToken) {
        String[] token = historyToken.split(DELIMITER);

        String bookPart = token.length > 1 ? token[1] : null;

        // TODO doesn't deal with strangely escaped characters (-; and --)
        String[] arg;
        if (token.length > 2) {
            int length = token.length - 2;
            arg = new String[length];

            System.arraycopy(token, 2, arg, 0, length);
        } else {
            arg = new String[0];
        }

        List<String> args = new ArrayList<>(Arrays.asList(arg));

        ANALYTICS.track(token[0], bookPart, args);
    }
}
