package rosa.website.core.client.widget;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import rosa.website.model.select.BookSelection;

/**
 * Cell used to render a BookSelection object in the CellBrowser used when
 * selecting books.
 */
public class BookSelectionCell extends AbstractCell<BookSelection> {
    @Override
    public void render(Context context, BookSelection value, SafeHtmlBuilder sb) {
        sb.appendEscaped(value.name + " (" + value.getCount() + ")");
    }
}
