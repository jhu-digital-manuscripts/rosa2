package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import rosa.website.core.client.view.SidebarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SidebarViewImpl extends Composite implements SidebarView {

    private final FlowPanel content;

    private List<Hyperlink> sidebar_links;  // Use for selection?

    public SidebarViewImpl() {
        SimpleLayoutPanel root = new SimpleLayoutPanel();
        this.sidebar_links = new ArrayList<>();

        content = new FlowPanel();
        root.setWidget(content);

        initWidget(root);
    }

    @Override
    public void setSiteNavigationLinks(Map<String, String> nav_links) {
        if (nav_links == null) {
            return;
        }

        for (Entry<String, String> entry : nav_links.entrySet()) {
            Hyperlink link = new Hyperlink(entry.getKey(), entry.getValue());
            link.setWidth("100%");
            link.addStyleName("SidebarItem");

            sidebar_links.add(link);
            content.add(link);
        }
    }

    @Override
    public void addSection(String title, Map<String, String> links) {
        if (title != null && !title.isEmpty()) {
            HTML header = new HTML(title);
            header.addStyleName("SidebarHeader");

            content.add(new HTML(title));
        }

        setSiteNavigationLinks(links);
    }

    @Override
    public void resize(String width, String height) {
        content.setSize(width, height);
    }
}
