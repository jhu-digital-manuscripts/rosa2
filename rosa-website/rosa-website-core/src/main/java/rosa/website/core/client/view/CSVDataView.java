package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.model.csv.CSVData;

public interface CSVDataView extends IsWidget {
    public interface Presenter {

    }

//    <T> void setData(CSVData<T> data);

    void clear();

    void setData(CSVData data);
}
