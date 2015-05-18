package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import rosa.website.core.client.view.SidebarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SidebarViewImpl extends Composite implements SidebarView {

    private final FlowPanel content;
    private final FlowPanel bookPanel;

    private List<Hyperlink> sidebar_links;  // Use for selection?
    private List<Hyperlink> book_links;

    public SidebarViewImpl() {
        SimpleLayoutPanel root = new SimpleLayoutPanel();
        root.addStyleName("Sidebar");

        this.bookPanel = new FlowPanel();
        this.sidebar_links = new ArrayList<>();
        this.book_links = new ArrayList<>();

        content = new FlowPanel();
        root.setWidget(content);
        content.add(bookPanel);

        bookPanel.setVisible(false);

        initWidget(root);
    }

    @Override
    public void setSiteNavigationLinks(Map<String, String> nav_links) {
        addLinks(nav_links, sidebar_links, content);
    }

    @Override
    public void addSection(String title, Map<String, String> links) {
        addHeader(title, content);
        addLinks(links, sidebar_links, content);
    }

    @Override
    public void setBookLinks(String title, Map<String, String> links) {
        addHeader(title, bookPanel);
        addLinks(links, book_links, bookPanel);
        bookPanel.setVisible(true);
    }

    @Override
    public void clearBookLinks() {
        bookPanel.clear();
        bookPanel.setVisible(false);
    }

    @Override
    public void resize(String width, String height) {
        content.setSize(width, height);
    }

    private void addHeader(String header, HasWidgets container) {
        if (header == null || header.isEmpty()) {
            return;
        }

        HTML h = new HTML(header);
        h.setStylePrimaryName("SidebarHeader");

        container.add(h);
    }

    private void addLinks(Map<String, String> links, List<Hyperlink> list, HasWidgets container) {
        if (links == null) {
            return;
        }

        for (Entry<String, String> entry : links.entrySet()) {
            Hyperlink link = new Hyperlink(entry.getKey(), entry.getValue());
            link.setStylePrimaryName("SidebarItem");

            list.add(link);
            container.add(link);
        }
    }
}
