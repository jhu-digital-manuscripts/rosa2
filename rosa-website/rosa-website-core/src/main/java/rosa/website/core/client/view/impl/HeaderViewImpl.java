package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import rosa.website.core.client.view.HeaderView;

public class HeaderViewImpl extends Composite implements HeaderView {
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
    public HeaderViewImpl() {
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

        root.add(image);
    }
}
