package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.FlowPanel;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.core.client.view.ErrorComposite;
import rosa.website.core.client.widget.BookDescriptionWidget;
import rosa.website.core.client.widget.BookMetadataWidget;
import rosa.website.model.view.BookDescriptionViewModel;

public class BookDescriptionViewImpl extends ErrorComposite implements BookDescriptionView {

    private BookMetadataWidget metadataWidget;
    private BookDescriptionWidget descriptionWidget;

    /**  */
    public BookDescriptionViewImpl() {
        super();

        FlowPanel root = new FlowPanel();
        metadataWidget = new BookMetadataWidget();
        descriptionWidget = new BookDescriptionWidget();

        root.add(errorPanel);
        root.add(metadataWidget);
        root.add(descriptionWidget);

        initWidget(root);
    }

    @Override
    public void clear() {
        metadataWidget.clear();
        descriptionWidget.clear();
        clearErrors();
    }

    @Override
    public void setModel(BookDescriptionViewModel model) {
        if (model.getMetadata() != null) {
            metadataWidget.setData(model);
        }
        if (model.getProse() != null && model.getProse().getXML() != null) {
            descriptionWidget.setDescription(model.getProse().getXML());
        }
    }

    @Override
    public void setPresenter(Presenter presenter) {
        metadataWidget.setPresenter(presenter);
        descriptionWidget.setPresenter(presenter);
    }
}
