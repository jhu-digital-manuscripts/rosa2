package rosa.website.test.client.nav;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import rosa.website.core.client.mvp.BaseActivityMapper;
import rosa.website.test.client.ClientFactory;
import rosa.website.test.client.activity.CSVDataActivity;
import rosa.website.test.client.activity.HTMLActivity;
import rosa.website.test.client.place.CSVDataPlace;
import rosa.website.test.client.place.HTMLPlace;

public class RosaActivityMapper extends BaseActivityMapper implements ActivityMapper {
    private final ClientFactory clientFactory;

    public RosaActivityMapper(ClientFactory clientFactory) {
        super(clientFactory);
        this.clientFactory = clientFactory;
    }

    @Override
    public Activity getActivity(Place place) {

        if (place instanceof HTMLPlace) {
            return new HTMLActivity((HTMLPlace) place, clientFactory);
        } else if (place instanceof CSVDataPlace) {
            return new CSVDataActivity((CSVDataPlace) place, clientFactory);
        }

        return super.getActivity(place);
    }
}
