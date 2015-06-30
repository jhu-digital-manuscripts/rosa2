package rosa.website.core.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface FlashStatusChangeEventHandler extends EventHandler {
    void onFlashStatusChange(FlashStatusChangeEvent event);
}
