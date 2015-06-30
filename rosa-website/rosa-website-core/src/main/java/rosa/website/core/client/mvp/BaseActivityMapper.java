package rosa.website.core.client.mvp;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import rosa.website.core.client.ClientFactory;

import java.util.logging.Logger;

public abstract class BaseActivityMapper implements ActivityMapper {
    private static final Logger logger = Logger.getLogger(BaseActivityMapper.class.toString());

    protected ClientFactory clientFactory;

    /**
     * @param clientFactory .
     */
    public BaseActivityMapper(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Activity getActivity(Place place) {
        logger.fine("Getting activity.");

        // TODO shared activities will go here

        logger.fine("Could not find associated activity.");
        return null;
    }
}
