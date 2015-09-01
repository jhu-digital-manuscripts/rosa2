package rosa.website.core.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface SidebarItemSelectedEventHandler extends EventHandler {
    void onSelected(SidebarItemSelectedEvent event);
}
