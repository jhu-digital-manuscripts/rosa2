package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import rosa.website.core.client.view.HeaderViewNoSearch;

public class HeaderViewNoSearchImpl extends Composite implements HeaderViewNoSearch {
    private final FlowPanel root;

    private Presenter presenter;

    private final ClickHandler goHomeClickHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            if (presenter != null) {
                presenter.goHome();
            }
        }
    };

    /**  */
    public HeaderViewNoSearchImpl() {
        root = new FlowPanel();
        root.setStylePrimaryName("Header");
        initWidget(root);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void addHeaderImage(String imageUrl, String altText) {
        Image image = new Image(imageUrl);

        image.setAltText(altText);
        image.addStyleName("link");
        image.addClickHandler(goHomeClickHandler);

        if (root.getWidgetCount() > 1) {
            root.insert(image, root.getWidgetCount() - 1);
        } else {
            root.add(image);
        }

    }

}
