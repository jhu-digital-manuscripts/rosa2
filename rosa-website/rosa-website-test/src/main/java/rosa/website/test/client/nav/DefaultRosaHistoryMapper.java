package rosa.website.test.client.nav;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import rosa.website.core.client.place.CSVDataPlace;
import rosa.website.core.client.place.HTMLPlace;

@WithTokenizers({
        HTMLPlace.Tokenizer.class,
        CSVDataPlace.Tokenizer.class
})
public interface DefaultRosaHistoryMapper extends PlaceHistoryMapper {}
