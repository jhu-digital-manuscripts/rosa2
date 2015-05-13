package rosa.website.core.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface BookSelectEventHandler extends EventHandler {
    void onBookSelect(BookSelectEvent event);
}
