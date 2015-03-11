package rosa.website.test.client.nav;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import rosa.website.test.client.place.CSVDataPlace;
import rosa.website.test.client.place.HTMLPlace;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RosaPlaceHistoryMapper implements PlaceHistoryMapper {
    private static final String DELIMITER = ";";

    private final String homeName;
    private final Set<String> htmlPageNames;
    private final Set<String> csvNames;

    public RosaPlaceHistoryMapper(String homeName, String[] htmlPageNames, String[] csvNames) {
        this.homeName = homeName;

        this.htmlPageNames = new HashSet<>(Arrays.asList(htmlPageNames));
        this.csvNames = new HashSet<>(Arrays.asList(csvNames));
    }
    
    @Override
    public Place getPlace(String token) {
        String[] token_parts = token.split(DELIMITER);

        if (token_parts.length < 1) {
            return new HTMLPlace(homeName);
        }

        String place = token_parts[0];
        if (htmlPageNames.contains(place)) {
            return new HTMLPlace(place);
        } else if (csvNames.contains(place)) {
            return new CSVDataPlace(place);
        }

        return null;
    }

    @Override
    public String getToken(Place place) {
        return place.getClass().getSimpleName().toLowerCase();
    }
}
