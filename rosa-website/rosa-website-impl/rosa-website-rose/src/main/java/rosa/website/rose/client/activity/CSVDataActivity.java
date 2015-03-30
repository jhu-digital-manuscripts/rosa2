package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CsvType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CSVDataActivity implements Activity {
    private static final Logger logger = Logger.getLogger(CSVDataActivity.class.toString());

    private final CSVDataPlace place;
    private CSVDataView view;

    private ArchiveDataServiceAsync service;

    /**
     * Create a new CSVDataActivity.
     *
     * @param place state information
     * @param clientFactory .
     */
    public CSVDataActivity(CSVDataPlace place, ClientFactory clientFactory) {
        this.place = place;
        this.view = clientFactory.csvDataView();
        this.service = clientFactory.archiveDataService();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onStop() {
        view.clear();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        logger.info("Starting CSVDataActivity. Current state: " + place.toString());
        panel.setWidget(view);

        CsvType type = getType(place.getName());

        service.loadCSVData(place.getCollection(), "en", type, new AsyncCallback<CSVData>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load CSV data.", caught);
            }

            @Override
            public void onSuccess(CSVData result) {
                handleCsvData(result);
            }
        });
    }

    private void handleCsvData(CSVData data) {
        logger.fine("Done CSVDataActivity.\n" + data.getId());
        view.setData(data);
    }

    private CsvType getType(String name) {
        for (CsvType type : CsvType.values()) {
            if (type.getKey().equals(name)) {
                return type;
            }
        }

        return null;
    }
}
