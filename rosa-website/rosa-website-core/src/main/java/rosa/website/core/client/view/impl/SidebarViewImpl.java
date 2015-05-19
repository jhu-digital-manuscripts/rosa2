package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.view.SidebarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SidebarViewImpl extends Composite implements SidebarView {
    private Presenter presenter;

    private final FlowPanel content;
    private final FlowPanel bookPanel;
    private final FlowPanel localePanel;

    private List<Hyperlink> sidebar_links;  // Use for selection?
    private List<HandlerRegistration> handlers;

    public SidebarViewImpl() {
        SimpleLayoutPanel root = new SimpleLayoutPanel();
        root.addStyleName("Sidebar");

        this.content = new FlowPanel();
        this.bookPanel = new FlowPanel();
        this.localePanel = new FlowPanel();
        this.sidebar_links = new ArrayList<>();
        this.handlers = new ArrayList<>();

        root.setWidget(content);
        content.add(bookPanel);
        content.add(localePanel);

        bookPanel.setVisible(false);

        handlers.add(this.addAttachHandler(new Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (!event.isAttached()) { // Remove all handlers on detach
                    for (HandlerRegistration h : handlers) {
                        h.removeHandler();
                    }
                }
            }
        }));
        addLanguageLinks();
        addFlashSelector();

        initWidget(root);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setSiteNavigationLinks(Map<String, String> nav_links) {
        addLinks(nav_links, content);
    }

    @Override
    public void addSection(String title, Map<String, String> links) {
        addHeader(title, content);
        addLinks(links, content);
    }

    @Override
    public void setBookLinks(String title, Map<String, String> links) {
        addHeader(title, bookPanel);
        addLinks(links, bookPanel);
        bookPanel.setVisible(true);
    }

    @Override
    public void clearBookLinks() {
        bookPanel.setVisible(false);

        for (Widget w : bookPanel) {
            if (w instanceof Hyperlink) {
                sidebar_links.remove((Hyperlink) w);
            }
        }

        bookPanel.clear();
    }

    @Override
    public void resize(String width, String height) {
        content.setSize(width, height);
    }

    public void addLanguageLinks() {
        addHeader("Language", localePanel);

        final Label en = new Label("English");
        en.addStyleName("SidebarItem");
        en.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (!LocaleInfo.getCurrentLocale().getLocaleName().equals("en")) {
                    String token = presenter.getCurrentToken();
                    Window.Location.assign(GWT.getHostPageBaseURL() + (token.isEmpty() ? "" : "#" + token));
                }
            }
        });

        final Label fr = new Label("French");
        fr.addStyleName("SidebarItem");
        fr.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (!LocaleInfo.getCurrentLocale().getLocaleName().equals("fr")) {
                    String token = presenter.getCurrentToken();
                    Window.Location.assign(GWT.getHostPageBaseURL() + "?locale=fr" + (token.isEmpty() ? "" : "#" + token));
                }
            }
        });

        localePanel.add(en);
        localePanel.add(fr);
    }

    private void addFlashSelector() {
        addHeader("Feature", content);
        final CheckBox useFlash = new CheckBox("Flash");
        content.add(useFlash);

        handlers.add(useFlash.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                if (presenter != null) {
                    presenter.setUseFlash(event.getValue());
                }
            }
        }));
    }

    private void addHeader(String header, HasWidgets container) {
        if (header == null || header.isEmpty()) {
            return;
        }

        HTML h = new HTML(header);
        h.setStylePrimaryName("SidebarHeader");

        container.add(h);
    }

    private void addLinks(Map<String, String> links, HasWidgets container) {
        if (links == null) {
            return;
        }

        for (final Entry<String, String> entry : links.entrySet()) {
            Hyperlink link = new Hyperlink(entry.getKey(), entry.getValue());

            link.setStylePrimaryName("SidebarItem");
            handlers.add(link.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    unselectAll();
                    ((Widget) event.getSource()).addStyleName("SidebarSelected");
                }
            }));

            sidebar_links.add(link);
            container.add(link);
        }
    }

    private void unselectAll() {
        for (Hyperlink link : sidebar_links) {
            link.removeStyleName("SidebarSelected");
        }
    }
}
