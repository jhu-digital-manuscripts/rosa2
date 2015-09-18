package rosa.website.core.client.widget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.website.core.client.Labels;
import rosa.website.core.client.view.BookDescriptionView.Presenter;
import rosa.website.model.select.DataStatus;
import rosa.website.model.view.BookDescriptionViewModel;

public class BookMetadataWidget extends Composite {
    private static final Label EMPTY_LABEL = new Label("");
    private Presenter presenter;

    private BookDescriptionViewModel model;

    private FlowPanel root;

    private FlexTable metadataTable;
    private FlexTable textsTable;

    /**
     * Create a new BookMetadataWidget for displaying structured metadata
     * about a book.
     */
    public BookMetadataWidget() {
        root = new FlowPanel();
        root.setStylePrimaryName("BookDescription");

        metadataTable = new FlexTable();
        textsTable = new FlexTable();

        initWidget(root);
    }

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    public void setData(BookDescriptionViewModel model) {
        this.model = model;
        draw();
    }

    /**
     * Clear all contents from this widget.
     */
    public void clear() {
        root.clear();

        metadataTable.clear();
        textsTable.clear();
    }

    private void draw() {
        metadataTable.clear();
        BookMetadata metadata = model.getMetadata();
        if (metadata == null) {
            return;
        }

        if (isNotEmpty(metadata.getCurrentLocation())) {
            metadataTable.setText(0, 0, Labels.INSTANCE.currentLocation());
            metadataTable.setText(0, 1, metadata.getCurrentLocation());
        }

        if (metadata.getNumberOfPages() != -1) {
            metadataTable.setText(0, 2, Labels.INSTANCE.folios());
            metadataTable.setText(0, 3, String.valueOf(metadata.getNumberOfPages()));
        }

        if (isNotEmpty(metadata.getType())) {
            metadataTable.setText(1, 0, Labels.INSTANCE.type());
            metadataTable.setText(1, 1, metadata.getType());
        }

        if (metadata.getNumberOfIllustrations() != -1) {
            metadataTable.setText(1, 2, Labels.INSTANCE.illustrations());
            metadataTable.setText(1, 3, String.valueOf(metadata.getNumberOfIllustrations()));
        }

        if (isNotEmpty(metadata.getDate())) {
            metadataTable.setText(2, 0, Labels.INSTANCE.date());
            metadataTable.setText(2, 1, metadata.getDate());
        }

        metadataTable.setText(2, 2, Labels.INSTANCE.transcription());
        metadataTable.setText(2, 3, getStatusString(model.getTranscriptionStatus()));

        if (isNotEmpty(metadata.getOrigin())) {
            metadataTable.setText(3, 0, Labels.INSTANCE.origin());
            metadataTable.setText(3, 1, metadata.getOrigin());
        }

        metadataTable.setText(3, 2, Labels.INSTANCE.illustrationDescription());
        metadataTable.setText(3, 3, getStatusString(model.getIllustrationDescriptionStatus()));

        metadataTable.addStyleName("BookDescriptionData");
        for (int i = 0; i < metadataTable.getRowCount(); i++) {
            metadataTable.getCellFormatter().addStyleName(i, 0, "BookDescriptionDataName");
            metadataTable.getCellFormatter().addStyleName(i, 2, "BookDescriptionDataName");
        }

        root.add(metadataTable);
        drawBookTexts(metadata.getTexts());
    }

    private void drawBookTexts(BookText[] texts) {
        if (texts == null || texts.length == 0) {
            return;
        }

        textsTable.setText(0, 0, "Text");
        textsTable.setText(0, 1, "Range");
        textsTable.setText(0, 2, "Folios");
        textsTable.setText(0, 3, "Illustrations");

        for (int i = 1; i <= texts.length; i++) {
            BookText text = texts[i - 1];

            // Text
            if (isNotEmpty(text.getTitle())) {
                textsTable.setText(i, 0, text.getTitle());
            }

            // Range
            HorizontalPanel rangePanel = new HorizontalPanel();
            rangePanel.add(createPageLink(text.getFirstPage()));
            rangePanel.add(new Label(" - "));
            rangePanel.add(createPageLink(text.getLastPage()));
            textsTable.setWidget(i, 1, rangePanel);

            // Folios
            if (text.getNumberOfPages() != -1) {
                textsTable.setText(i, 2, String.valueOf(text.getNumberOfPages()));
            }

            // Illustrations
            if (text.getNumberOfIllustrations() != -1) {
                textsTable.setText(i, 3, String.valueOf(text.getNumberOfIllustrations()));
            }
        }

        textsTable.setStylePrimaryName("BookDescriptionTextData");
        textsTable.getRowFormatter().setStylePrimaryName(0, "BookDescriptionDataName");

        root.add(textsTable);
    }

    /**
     * @param page .
     * @return link to read specified page, or a label if no such link exists
     */
    private Widget createPageLink(String page) {
        if (isNotEmpty(page)) {
            if (isNumeric(page)) {
                return new Hyperlink(page, presenter.getPageUrlFragment(parseInt(page)));
            } else if (isRectoVerso(page)) {
                return new Hyperlink(page, presenter.getPageUrlFragment(page));
            }
        }

        return EMPTY_LABEL;
    }

    private boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private boolean isRectoVerso(String page) {
        return page.endsWith("r") || page.endsWith("v") || page.endsWith("R") || page.endsWith("V");
    }

    /**
     * @param str .
     * @return is this string a number
     */
    private native boolean isNumeric(String str) /*-{
        return !isNaN(str);
    }-*/;

    private native int parseInt(String str) /*-{
        return parseInt(str);
    }-*/;

    private String getStatusString(DataStatus status) {
        if (status == null) {
            status = DataStatus.NONE;
        }

        switch (status) {
            default:
            case NONE:
                return Labels.INSTANCE.none();
            case PARTIAL:
                return Labels.INSTANCE.partial();
            case FULL:
                return Labels.INSTANCE.complete();
        }
    }
}
