package rosa.website.core.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface LangaugeChangeEventHandler extends EventHandler {
    void onLanguageChange(LanguageChangeEvent event);
}
