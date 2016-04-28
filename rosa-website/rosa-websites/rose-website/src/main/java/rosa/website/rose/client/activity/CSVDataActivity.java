package rosa.website.rose.client.activity;

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
import rosa.website.core.client.Labels;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVType;
import rosa.website.model.csv.CollectionDisplayCSV;
import rosa.website.model.csv.IllustrationTitleCSV;
import rosa.website.model.csv.NarrativeSectionsCSV;
import rosa.website.rose.client.RosaHistoryConfig;
import rosa.website.rose.client.WebsiteConfig;

import java.util.HashMap;
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
        logger.info("Starting CSVDataActivity. Current state: " + place.toString());
        panel.setWidget(view);
        view.setPresenter(this);

        LoadingPanel.INSTANCE.show();

        CSVType type = RosaHistoryConfig.getCsvType(place.getName());
        if (type == null) {
            logger.warning("No CSV data associated associated with this place. " + place.toString());
            return;
        }

        final Map<Enum, String> links = getPossibleLinks(type);
        final String[] headers = getHeaders(type);
        service.loadCSVData(WebsiteConfig.INSTANCE.collection(), lang, type, new AsyncCallback<CSVData>() {
            @Override
            public void onFailure(Throwable caught) {
                String msg = "Failed to load CSV data.";
                logger.log(Level.SEVERE, msg, caught);
                view.addErrorMessage(msg);
                LoadingPanel.INSTANCE.hide();
            }

            @Override
            public void onSuccess(CSVData result) {
                view.setData(result, links, headers);
                LoadingPanel.INSTANCE.hide();
            }
        });

        ExternalTextResource resource = RosaHistoryConfig.getCsvDescription(place.getName());
        if (resource != null) {
            try {
                resource.getText(new ResourceCallback<TextResource>() {
                    @Override
                    public void onError(ResourceException e) {
                        String msg = "Failed to load CSV description.";
                        logger.log(Level.SEVERE, msg, e);
                        view.addErrorMessage(msg);
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

    private Map<Enum, String> getPossibleLinks(CSVType type) {
        Map<Enum, String> map = new HashMap<>();

        switch (type) {
            case COLLECTION_DATA:
            case COLLECTION_BOOKS:
                map.put(CollectionDisplayCSV.Column.NAME, "book");
                return map;
            case NARRATIVE_SECTIONS:
                map.put(NarrativeSectionsCSV.Column.ID, "search;NARRATIVE_SECTION");
                return map;
            case ILLUSTRATIONS:
                map.put(IllustrationTitleCSV.Column.TITLE, "search;ILLUSTRATION_TITLE");
                return map;
            default:
                return null;
        }
    }

    private String[] getHeaders(CSVType type) {
        Labels labels = Labels.INSTANCE;
        switch (type) {
            case COLLECTION_DATA:
            case COLLECTION_BOOKS:
                return new String[] {
                        labels.name(),
                        labels.date(),
                        labels.folios(),
                        labels.numIllustrationsShort(),
                        labels.colsPerFolio(),
                        labels.linesPerColumn(),
                        labels.dimensions(),
                        labels.leavesPerGathering(),
                        labels.foliosWithGreaterThanOneIllustration()
                };
            case ILLUSTRATIONS:
                return new String[] {labels.position(), labels.illustrationTitle(), labels.frequency()};
            case CHARACTERS:
                return new String[] {labels.name(), labels.french(), labels.english()};
            case NARRATIVE_SECTIONS:
                return new String[] {labels.identifier(), labels.description(), labels.lecoy()};
            default:
                break;
        }
        return null;
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
