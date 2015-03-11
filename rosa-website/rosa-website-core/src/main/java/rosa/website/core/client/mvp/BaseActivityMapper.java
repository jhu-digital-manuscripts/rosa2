package rosa.website.core.client.mvp;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import rosa.website.core.client.BaseClientFactory;
import rosa.website.core.client.activity.TestActivity;
import rosa.website.core.client.place.TestPlace;

import java.util.logging.Logger;

public class BaseActivityMapper implements ActivityMapper {
    private static final Logger logger = Logger.getLogger(BaseActivityMapper.class.toString());

    private BaseClientFactory clientFactory;

    public BaseActivityMapper(BaseClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Activity getActivity(Place place) {
        logger.fine("Getting activity.");

        if (place instanceof TestPlace) {
            return new TestActivity((TestPlace) place, clientFactory);
        }

        logger.fine("Could not find associated activity.");
        return null;
    }
}
