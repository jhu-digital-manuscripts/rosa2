package rosa.website.core.client.mvp;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.activity.CSVDataActivity;
import rosa.website.core.client.activity.HTMLActivity;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;

import java.util.logging.Logger;

public class BaseActivityMapper implements ActivityMapper {
    private static final Logger logger = Logger.getLogger(BaseActivityMapper.class.toString());

    private ClientFactory clientFactory;

    public BaseActivityMapper(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Activity getActivity(Place place) {
        logger.fine("Getting activity.");

        if (place instanceof HTMLPlace) {
            return new HTMLActivity((HTMLPlace) place, clientFactory);
        } else if (place instanceof CSVDataPlace) {
            return new CSVDataActivity((CSVDataPlace) place, clientFactory);
        }

        logger.fine("Could not find associated activity.");
        return null;
    }
}
