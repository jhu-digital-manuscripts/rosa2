package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import rosa.website.core.client.view.HeaderViewNoSearch;

import java.util.Map;
import java.util.Map.Entry;

public class HeaderViewNoSearchImpl extends Composite implements HeaderViewNoSearch {
    private final FlowPanel bannerPanel;
    private final MenuBar navPanel;

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
        VerticalPanel root = new VerticalPanel();
        root.setStylePrimaryName("Header");

        bannerPanel = new FlowPanel();
        navPanel = new MenuBar(false);

        navPanel.addStyleName("HeaderMenu");
        navPanel.setAutoOpen(true);

        root.add(bannerPanel);
        root.add(navPanel);

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

        if (bannerPanel.getWidgetCount() > 1) {
            bannerPanel.insert(image, bannerPanel.getWidgetCount() - 1);
        } else {
            bannerPanel.add(image);
        }

    }

    @Override
    public void addNavLink(String label, final String target) {
        MenuItem item = new MenuItem(label, new Command() {
            @Override
            public void execute() {
                History.newItem(target);
            }
        });
        item.addStyleName("item");

        navPanel.addItem(item);
    }

    public void addNavMenu(String topLabel, Map<String, String> subMenuMap) {
        MenuBar submenu = new MenuBar(true);

        for (final Entry<String, String> entry : subMenuMap.entrySet()) {
            MenuItem item = new MenuItem(entry.getKey(), new Command() {
                @Override
                public void execute() {
                    History.newItem(entry.getValue());
                }
            });
            item.addStyleName("item");

            submenu.addItem(item);
        }

        navPanel.addItem(topLabel, submenu);
    }

}
