package rosa.website.core.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.model.csv.CSVData;

import java.util.Map;

public interface CSVDataView extends IsWidget {

    interface Presenter {
        void goTo(Place place);
    }

    /**
     * Clear data.
     */
    void clear();

    void setPresenter(Presenter presenter);

    void setData(CSVData data);

    void setData(CSVData data, Map<Enum, String> links);

    void setDescription(String description);
}
