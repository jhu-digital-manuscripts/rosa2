package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.BookMetadata;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.BookDescriptionPlace;
import rosa.website.core.client.view.BookDescriptionView;

public class BookDescriptionActivity implements Activity {

    private final String book;
    private final BookDescriptionView view;

    public BookDescriptionActivity(BookDescriptionPlace place, ClientFactory clientFactory) {
        this.book = place.getBook();
        this.view = clientFactory.bookDescriptionView();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);

        BookMetadata metadata = null;
    }
}
