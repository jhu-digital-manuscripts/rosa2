package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookMetadata;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.core.client.widget.BookDescriptionWidget;
import rosa.website.core.client.widget.BookMetadataWidget;

public class BookDescriptionViewImpl extends Composite implements BookDescriptionView {

    private BookMetadataWidget metadataWidget;
    private BookDescriptionWidget descriptionWidget;

    /**  */
    public BookDescriptionViewImpl() {
        FlowPanel root = new FlowPanel();
        metadataWidget = new BookMetadataWidget();
        descriptionWidget = new BookDescriptionWidget();

        root.add(metadataWidget);
        root.add(descriptionWidget);

        initWidget(root);
    }

    @Override
    public void clear() {
        metadataWidget.clear();
        descriptionWidget.clear();
    }

    @Override
    public void setMetadata(BookMetadata metadata) {
        metadataWidget.setData(metadata);
    }

    @Override
    public void setDescription(BookDescription description) {
        descriptionWidget.setDescription(description.getXML());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        metadataWidget.setPresenter(presenter);
        descriptionWidget.setPresenter(presenter);
    }
}
