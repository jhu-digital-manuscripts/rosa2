package rosa.website.rose.client.nav;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.mvp.BaseActivityMapper;
import rosa.website.core.client.place.BookDescriptionPlace;
import rosa.website.core.client.place.BookSelectPlace;
import rosa.website.core.client.place.BrowseBookPlace;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.rose.client.activity.BookDescriptionActivity;
import rosa.website.rose.client.activity.BookSelectActivity;
import rosa.website.rose.client.activity.BrowseBookActivity;
import rosa.website.rose.client.activity.CSVDataActivity;
import rosa.website.rose.client.activity.HTMLActivity;

public class RosaActivityMapper extends BaseActivityMapper implements ActivityMapper {
    public RosaActivityMapper(ClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Activity getActivity(Place place) {
        if (place instanceof HTMLPlace) {
            return new HTMLActivity((HTMLPlace) place, clientFactory);
        } else if (place instanceof CSVDataPlace) {
            return new CSVDataActivity((CSVDataPlace) place, clientFactory);
        } else if (place instanceof BookSelectPlace) {
            return new BookSelectActivity((BookSelectPlace) place, clientFactory);
        } else if (place instanceof BookDescriptionPlace) {
            return new BookDescriptionActivity((BookDescriptionPlace) place, clientFactory);
        } else if (place instanceof BrowseBookPlace) {
            return new BrowseBookActivity((BrowseBookPlace) place, clientFactory);
        }

        // If custom activities are created by the web app, extend the BaseActivityMapper
        // and override its getActivity method.
        return super.getActivity(place);
    }

}
