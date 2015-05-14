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
import rosa.website.core.client.view.BookDescriptionView.Presenter;

public class BookMetadataWidget extends Composite {
    private static final Label EMPTY_LABEL = new Label("");
    private Presenter presenter;

    private BookMetadata metadata;

    private FlowPanel root;

    private FlexTable metadataTable;
    private FlexTable textsTable;

    /**
     * Create a new BookMetadataWidget for displaying structured metadata
     * about a book.
     */
    public BookMetadataWidget() {
        root = new FlowPanel();

        metadataTable = new FlexTable();
        textsTable = new FlexTable();

        initWidget(root);
    }

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    /**
     * @param metadata structured metadata describing a book
     */
    public void setData(BookMetadata metadata) {
        this.metadata = metadata;
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
        if (metadata == null) {
            return;
        }

        if (isNotEmpty(metadata.getCurrentLocation())) {
            metadataTable.setText(0, 0, "Current Location:");
            metadataTable.setText(0, 1, metadata.getCurrentLocation());
        }

        if (metadata.getNumberOfPages() != -1) {
            metadataTable.setText(0, 2, "Folios:");
            metadataTable.setText(0, 3, String.valueOf(metadata.getNumberOfPages()));
        }

        if (isNotEmpty(metadata.getType())) {
            metadataTable.setText(1, 0, "Type:");
            metadataTable.setText(1, 1, metadata.getType());
        }

        if (metadata.getNumberOfIllustrations() != -1) {
            metadataTable.setText(1, 2, "Illustrations:");
            metadataTable.setText(1, 3, String.valueOf(metadata.getNumberOfIllustrations()));
        }

        if (isNotEmpty(metadata.getDate())) {
            metadataTable.setText(2, 0, "Date:");
            metadataTable.setText(2, 1, metadata.getDate());
        }

        // TODO transcriptions

        if (isNotEmpty(metadata.getOrigin())) {
            metadataTable.setText(3, 0, "Origin:");
            metadataTable.setText(3, 1, metadata.getOrigin());
        }

        // TODO illustration description

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
}
