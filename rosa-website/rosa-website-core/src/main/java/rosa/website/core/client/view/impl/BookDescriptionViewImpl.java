package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookMetadata;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.core.client.widget.BookMetadataWidget;

public class BookDescriptionViewImpl extends Composite implements BookDescriptionView {

    private BookMetadataWidget metadataWidget;
    private VerticalPanel descriptions;

    public BookDescriptionViewImpl() {
        FlowPanel root = new FlowPanel();
        metadataWidget = new BookMetadataWidget();
        descriptions = new VerticalPanel();

        root.add(metadataWidget);
        root.add(descriptions);

        initWidget(root);
    }

    @Override
    public void clear() {
        metadataWidget.clear();
        descriptions.clear();
    }

    @Override
    public void setMetadata(BookMetadata metadata) {
        metadataWidget.setData(metadata);
    }

    @Override
    public void setDescription(BookDescription description) {
        if (description == null) {
            descriptions.add(new Label("No description found."));
            return;
        }

        for (String topic : description.getTopics()) {
            descriptions.add(new Label(topic));
            descriptions.add(new HTML(description.getDescription(topic)));
        }
    }

    @Override
    public void setPresenter(Presenter presenter) {
        metadataWidget.setPresenter(presenter);
    }
}
