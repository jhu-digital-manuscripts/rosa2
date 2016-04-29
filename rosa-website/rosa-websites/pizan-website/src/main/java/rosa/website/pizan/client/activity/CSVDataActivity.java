package rosa.website.pizan.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.ResourceCallback;
import com.google.gwt.resources.client.ResourceException;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVType;
import rosa.website.pizan.client.HistoryConfig;
import rosa.website.pizan.client.WebsiteConfig;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Activity for displaying CSV data in table form.
 */
public class CSVDataActivity implements Activity, CSVDataView.Presenter {
    private static final Logger logger = Logger.getLogger(CSVDataActivity.class.toString());

    private final CSVDataPlace place;
    private CSVDataView view;
    private String lang;

    private ArchiveDataServiceAsync service;
    private PlaceController placeController;

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
        this.lang = LocaleInfo.getCurrentLocale().getLocaleName();
        this.placeController = clientFactory.placeController();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {
        view.clear();
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void onStop() {
        view.clear();
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        final String error = "Failed to load CSV data.";

        LoadingPanel.INSTANCE.show();
        logger.info("Starting CSVDataActivity. Current state: " + place.toString());
        panel.setWidget(view);
        view.setPresenter(this);

        CSVType type = HistoryConfig.getCsvType(place.getName());
        if (type == null) {
            logger.warning("No CSV data associated associated with this place. " + place.toString());
            return;
        }

        service.loadCSVData(WebsiteConfig.INSTANCE.collection(), lang, type, new AsyncCallback<CSVData>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, error, caught);
                view.addErrorMessage(error);
                LoadingPanel.INSTANCE.hide();
            }

            @Override
            public void onSuccess(CSVData result) {
                if (result == null) {
                    logger.severe(error);
                    view.addErrorMessage(error);
                    return;
                }

                LoadingPanel.INSTANCE.hide();
                handleCsvData(result, null);
            }
        });
    }

    // TODO must add CSV column headers
    private void handleCsvData(CSVData data, Map<Enum, String> links) {
        view.setData(data, links, null);

        ExternalTextResource resource = HistoryConfig.getCsvDescription(place.getName());
        if (resource != null) {
            try {
                resource.getText(new ResourceCallback<TextResource>() {
                    @Override
                    public void onError(ResourceException e) {
                        logger.log(Level.SEVERE, "Failed to load CSV description.", e);
                    }

                    @Override
                    public void onSuccess(TextResource resource) {
                        view.setDescription(resource.getText());
                    }
                });
            } catch (ResourceException e) {
                logger.log(Level.SEVERE, "Failed to load CSV description.", e);
            }
        }
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
