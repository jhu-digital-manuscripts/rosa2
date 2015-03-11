package rosa.website.core.client.mvp;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import rosa.website.core.client.place.HTMLPlace;

@WithTokenizers({
        HTMLPlace.Tokenizer.class
})
public interface BaseHistoryMapper extends PlaceHistoryMapper{}
