package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import rosa.archive.model.BookMetadata;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.core.client.widget.BookMetadataWidget;

public class BookDescriptionViewImpl extends Composite implements BookDescriptionView {

    private BookMetadataWidget metadataWidget;
    private Presenter presenter;

    public BookDescriptionViewImpl() {
        FlowPanel root = new FlowPanel();
        metadataWidget = new BookMetadataWidget();

        root.add(metadataWidget);

        initWidget(root);
    }

    @Override
    public void setMetadata(BookMetadata metadata) {
        metadataWidget.setData(metadata);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        metadataWidget.setPresenter(presenter);
    }
}
