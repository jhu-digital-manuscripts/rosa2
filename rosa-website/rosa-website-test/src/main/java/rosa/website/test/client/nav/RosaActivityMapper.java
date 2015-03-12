package rosa.website.test.client.nav;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.mvp.BaseActivityMapper;

public class RosaActivityMapper extends BaseActivityMapper implements ActivityMapper {
    public RosaActivityMapper(ClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Activity getActivity(Place place) {
        // If custom activities are created by the web app, extend the BaseActivityMapper
        // and override its getActivity method.
        return super.getActivity(place);
    }

}
