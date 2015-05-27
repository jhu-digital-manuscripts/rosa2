package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import rosa.website.core.client.view.HeaderView;

public class HeaderViewImpl extends Composite implements HeaderView {
    private Image banner_img;
    private Image banner_txt;

    public HeaderViewImpl() {
        FlowPanel root = new FlowPanel();
        root.setStylePrimaryName("Header");

        banner_img = new Image(GWT.getModuleBaseURL() + "banner_image1.gif");
        banner_img.setAltText("banner");
        banner_img.addStyleName("link");

        banner_txt = new Image(GWT.getModuleBaseURL() + "banner_text.jpg");
        banner_txt.setAltText("Roman de la Rose Digital Library");
        banner_txt.addStyleName("link");

        root.add(banner_img);
        root.add(banner_txt);

        initWidget(root);
    }

    @Override
    public void setPresenter(final Presenter presenter) {
        ClickHandler bannerClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.goHome();
            }
        };

        banner_img.addClickHandler(bannerClickHandler);
        banner_txt.addClickHandler(bannerClickHandler);
    }
}
