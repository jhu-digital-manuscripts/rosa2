package rosa.website.core.client.widget;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import rosa.website.model.select.BookInfo;

/**
 * Displays specific books when a category has been selected. Used in the CellBrowser
 * when selecting books. This cell also provides a clickable link to read the specified
 * book.
 */
public class BookInfoCell extends AbstractCell<BookInfo> {
    @Override
    public void render(Context context, BookInfo value, SafeHtmlBuilder sb) {
        StringBuilder url = new StringBuilder(GWT.getHostPageBaseURL());

        String locale = LocaleInfo.getCurrentLocale().getLocaleName();
        if (locale != null && !locale.isEmpty() && !locale.equalsIgnoreCase("en")) {
            url.append("?locale=");
            url.append(locale);
        }

        url.append("#book;");
        url.append(value.id);

        sb.appendHtmlConstant("<a href=\"" + url.toString() + "\">");
        sb.appendEscaped(value.title);
        sb.appendHtmlConstant("</a>");
    }
}
